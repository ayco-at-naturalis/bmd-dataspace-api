#!/usr/bin/env bash

if [ -z "${BASE_DIR:-}" ]; then
  echo "functions.sh requires BASE_DIR to be set" >&2
  exit 1
fi

# Constants used across other scripts
HOSTS_FILE="${BASE_DIR}/ansible/inventory/hosts.yml"
TOKEN_FILE="${BASE_DIR}/ansible/.vault-token"
VERSION_FILE="${BASE_DIR}/ansible/.image-version"



# Prints a space-separated list of all groups within hosts.yml file to which the specified
# host belongs (either directly or indirectly). The implicit "all" and "ungrouped" groups
# are excluded from the list. The printed groups are used to look up identically named
# secrets in HC Vault.
get_ansible_groups() { # hostname
  if [[ $# -ne 1 ]]; then
    echo "USAGE: get_ansible_groups <hostname>" >&2
    return 1
  fi

  local host="$1"

  if ! ansible-inventory -i "$HOSTS_FILE" --host "$host" > /dev/null 2>&1; then
    echo "No such host: \"$host\""
    return 1
  fi

  local json
  json="$(ansible-inventory -i "$HOSTS_FILE" --list 2>/dev/null)" || {
    echo "Error: failed to run ansible-inventory using $HOSTS_FILE" >&2
    return 1
  }

  local groups
  groups="$(
    jq -r --arg h "$host" '
      # Collect all hosts belonging to group $g, including via child groups (recursive).
      def group_hosts($inv; $g):
        ($inv[$g].hosts // []) as $direct
        | ($inv[$g].children // []) as $children
        | ($direct + ([ $children[]? | group_hosts($inv; .) ] | add? // []));

      # Inventory object without _meta.
      (del(._meta)) as $inv

      # Candidate groups: all top-level keys except implicit.
      | ($inv
         | keys
         | map(select(. != "all" and . != "ungrouped"))
        ) as $candidates

      # Keep groups whose (recursive) host set contains $h.
      | [ $candidates[]
          | select( group_hosts($inv; .) | index($h) != null )
        ]

      # Case-insensitive dedupe + sort; then join.
      | unique_by(ascii_downcase)
      | sort_by(ascii_downcase)
      | join(" ")
    ' <<< "$json"
  )" || {
    echo "Error while processing JSON" >&2
    return 2
  }

  # No groups found. That's fine.
  [[ -n "$groups" ]] && printf '%s\n' "$groups"
  return 0
}


# Ensures HC vault CLI is available. While you can use Hashicorp's Docker image to
# retrieve HC Vault secrets, using it to retrieve a vault token is almost impossible
# unless you print an extended warning beforehand about what might happen (having to
# log in via Azure AD and have your 2F app ready). Therefore, if the user has not
# already installed the CLI already, we install it in the .tools directory (git ignored)
# of this repository.
ensure_vault_cli() {
  if command -v vault >/dev/null 2>&1; then # already installed
    return 0
  fi
  local version="${VAULT_CLI_VERSION:?Missing value for VAULT_CLI_VERSION}"
  local os="linux"
  local arch="amd64"
  local tools_dir="${BASE_DIR}/.tools/vault/${version}"
  local vault_bin="${tools_dir}/vault"
  local zip_name="vault_${version}_${os}_${arch}.zip"
  local zip_path="${tools_dir}/${zip_name}"
  mkdir -p "$tools_dir"
  echo "---> Bootstrapping HC Vault CLI from $tools_dir"
  if [[ ! -x "$vault_bin" ]]; then
    # Download
    curl -fsSL -o "$zip_path" "https://releases.hashicorp.com/vault/${version}/${zip_name}"
    # Unzip
    (cd "$tools_dir" && unzip -o "$zip_name" >/dev/null)
    chmod +x "$vault_bin"
  fi
  export PATH="${tools_dir}:$PATH"
}


# Retrieves a secret from HC Vault and saves it locally. The file will only be written
# if the endpoint is valid (the secret exists - no 404) and if the returned JSON is
# not empty (at least one key-value pair). However, the function does _not_ fail if
# either is not the case. It is up to the caller to decide how to handle absent/empty
# secrets.
get_secret_as_json() { # secret_name, output_file

  # Root of our little corner in HC Vault
  local hc_vault_base="${VAULT_SERVER_URL}/v1/kv2/data/${VAULT_PATH}"

  # HC Vault Path to secret relative to hc_vault_base
	local secret="${1:?No secret specified}"
	# File system path relative to ${BASE_DIR}
	local save_as="${2:?No output file specified}"
	local endpoint="${hc_vault_base}/${secret}"
	local token="$(<"${TOKEN_FILE}")"

	echo "Retrieving secret \"${secret}\""

	# Get response body __and__ HTTP status code in one CURL request
	# by appending (-w) the HTTP status code to response body
	local body="$(curl -sS -w $'\n%{http_code}' -H "X-Vault-Token: ${token}" "${endpoint}")" || true
	# extracting the status from the response body
	local status="${body##*$'\n'}"
	# and then shaving off the status from body again
	body="${body%$'\n'*}"

	if [[ $status -eq 200 ]]; then
		# Only write a file if there are keys under .data.data
		if jq -e '.data.data | length > 0' > /dev/null 2>&1 <<< "$body"; then
			jq '.data.data' <<< "$body" > "${BASE_DIR}/${save_as}"
			echo "---> Secret saved"
		else
			echo "---> Empty secret"
		fi
	elif [[ $status -eq 404 ]]; then
		echo "---> No secret there"
	else
		echo "$body" >&2 # E.g. "403 Permission Denied"
		echo >&2
		exit 1
	fi

}


