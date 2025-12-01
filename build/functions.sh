#!/usr/bin/env bash

set -euo pipefail

# get tag associated with commit (i.e. GitLab's CI_COMMIT_TAG)
get_commit_ref_slug() {
  branch="$(git rev-parse --abbrev-ref HEAD)"
  _to_slug "${branch}"
}

# This is a translation Gitlab::Utils.to_slug(), used by
# GitLab to calculate _SLUG vars. See
# https://gitlab.com/gitlab-org/gitlab/-/blob/master/lib/gitlab/utils.rb#L49
_to_slug() {
  # 1. to lower case
  slug="${1,,}"
  # 2. replace all non-alphanumerics with '-'
  slug="${slug//[^a-z0-9]/-}"
  # 3. collapse multiple '-'
  slug="$(echo "$slug" | tr -s '-')"
  # 4. trim leading/trailing '-'
  slug="${slug#-}"
  slug="${slug%-}"
  echo "$slug"
}
