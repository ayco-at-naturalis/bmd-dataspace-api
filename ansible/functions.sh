#!/usr/bin/env bash

HOSTS_FILE="${BASE_DIR}/ansible/inventory/hosts.yml"

# Prints a space-separated list of all groups within hosts.yml file to which the specified
# hosts belongs (either directly or indirectly). The implicit "all" and "ungrouped" groups
# are excluded from the list. The printed groups are used to look up identically named
# secrets in HC Vault.
get_ansible_groups() {
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
# of this git repository.
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


