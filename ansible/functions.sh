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
