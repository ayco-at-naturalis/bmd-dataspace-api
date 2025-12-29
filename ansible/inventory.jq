# jq code that parses the output from `ansible-inventory --list` and
# returns a flattened, space-separated list of all ansible groups to
# which a given host belongs. The host is passed in as $hostname from
# the jq command line. See filter_ansible_groups() in functions.sh

# Collect all hosts belonging to group $group, including via child
# groups (recursive).
def get_hosts_in_group($groups; $group):
    ($groups[$group].hosts // []) as $direct
    | ($groups[$group].children // []) as $children
    | ($direct + ([ $children[]? | get_hosts_in_group($groups; .) ] | add? // []));

# Remove the _meta object from the input (.) and assign resulting object to
# a variable named "group"
(del(._meta)) as $groups

# Candidate groups: all top-level keys except implicit "all" and "ungrouped" groups
| ($groups
 | keys
 | map(select(. != "all" and . != "ungrouped"))
) as $candidates

# Only keep groups whose (recursive) host set contains $hostname.
| [ $candidates[]
  | select( get_hosts_in_group($groups; .) | index($hostname) != null )
]

# Case-insensitive dedupe + sort; then join.
| unique_by(ascii_downcase)
| sort_by(ascii_downcase)
| join(" ")
