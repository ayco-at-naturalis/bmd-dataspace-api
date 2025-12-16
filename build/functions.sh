#!/bin/sh

# get tag associated with branch (GitLab's CI_COMMIT_REF_SLUG)
get_commit_ref_slug() {
  branch=$(git rev-parse --abbrev-ref HEAD)
  _to_slug "$branch"
}

# Determines whether the provided commit hash is a merge commit
is_merge_commit() { # commit_sha
  [ "$(git show -s --pretty=%P "$1" | wc -w)" -ge 2 ]
}

# Executes Maven in a standardized way. Pass the desired Maven goals
# as arguments to this function. For example:
#   run_maven clean install
run_maven() { # goal1, goal2, ...
  if [ -z "${BASE_DIR-}" ]; then
    echo 'Missing variable BASE_DIR' >&2
    exit 1
  fi
  # JVM options:
  export MAVEN_OPTS="${MAVEN_OPTS-} --enable-native-access=ALL-UNNAMED"
  "${BASE_DIR}/mvnw" \
    -Dmaven.repo.local="${BASE_DIR}/build/mvn-repo" \
    --show-version \
    --batch-mode \
    --no-transfer-progress \
    --file="${BASE_DIR}/pom.xml" \
    "$@"
}

# Translation of Gitlab::Utils.to_slug(), used by GitLab to calculate *_SLUG vars.
# See:
# https://gitlab.com/gitlab-org/gitlab/-/blob/master/lib/gitlab/utils.rb#L49
_to_slug() {
  # 1. to lower case
  slug=$(printf '%s\n' "$1" | tr 'A-Z' 'a-z')
  # 2. replace all non-alphanumerics with '-'
  slug=$(printf '%s\n' "$slug" | sed 's/[^a-z0-9]/-/g')
  # 3. collapse multiple '-'
  slug=$(printf '%s\n' "$slug" | tr -s '-')
  # 4. trim leading/trailing '-'
  #   - remove leading '-'
  slug=$(printf '%s\n' "$slug" | sed 's/^-*//')
  #   - remove trailing '-'
  slug=$(printf '%s\n' "$slug" | sed 's/-*$//')
  printf '%s\n' "$slug"
}
