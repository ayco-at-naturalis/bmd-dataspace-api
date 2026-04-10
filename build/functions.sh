#!/bin/sh

require_function() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf '%s\n' "o ---> Missing required function: $1" && exit 1
  fi
}

get_commit_ref_slug() {
  if [ -n "${CI_COMMIT_REF_SLUG-}" ]; then
    printf '%s' "${CI_COMMIT_REF_SLUG}"
  else # DIY
    branch=$(git rev-parse --abbrev-ref HEAD)
    _to_slug "$branch"
  fi
}

get_commit_sha() {
  if [ -n "${CI_COMMIT_SHA-}" ]; then
    printf '%s' "${CI_COMMIT_SHA}"
  else
    : "${BASE_DIR?Missing value for BASE_DIR}"
    git -C "${BASE_DIR}" rev-parse HEAD
  fi
}

get_commit_short_sha() {
  if [ -n "${CI_COMMIT_SHORT_SHA-}" ]; then
    printf '%s' "${CI_COMMIT_SHORT_SHA}"
  else
    : "${BASE_DIR?Missing value for BASE_DIR}"
    git -C "${BASE_DIR}" rev-parse --short HEAD
  fi
}

get_commit_tag() {
  if [ -n "${CI_COMMIT_TAG-}" ]; then
    printf '%s' "${CI_COMMIT_TAG}"
  else # DIY
    : "${BASE_DIR?Missing value for BASE_DIR}"
    printf '%s' "$(git -C "${BASE_DIR}" log -n 1 --pretty=format:'%H')"
  fi
}

# Determines whether the provided commit hash is a merge commit
is_merge_commit() { # commit_sha
  [ "$(git show -s --pretty=%P "$1" | wc -w)" -ge 2 ]
}

# Prints the full image URL of the very first image created in the
# pipeline. The image tag for this image is "<branch>_<commit_sha>".
get_base_image() {
  : "${CI_REGISTRY_IMAGE?Missing value for CI_REGISTRY_IMAGE}"
  base="${CI_REGISTRY_IMAGE}"
  branch="$(get_commit_ref_slug)"
  sha="$(get_commit_sha)"
  printf '%s:%s_%s' "${base}" "${branch}" "${sha}"
}

add_tag() { #tag
  : "${CI_REGISTRY_IMAGE?Missing value for CI_REGISTRY_IMAGE}"
  : "${1?No argument specified for add_tag}"
  tag="${1}"
  echo
  echo "o ---> Adding tag \"${tag}\""
  docker tag "$(get_base_image)" "${CI_REGISTRY_IMAGE}:${tag}"
  docker push "${CI_REGISTRY_IMAGE}:${tag}"
}

# Executes Maven in a standardized way. Pass the desired Maven goals
# as arguments to this function. For example:
#   run_maven clean install
run_maven() { # goal1 goal2 ...
  : "{BASE_DIR:?Missing value for BASE_DIR}"
  # JVM options:
  export MAVEN_OPTS="${MAVEN_OPTS-} --enable-native-access=ALL-UNNAMED"
  "${BASE_DIR}/mvnw" \
    -Dmaven.repo.local="${BASE_DIR}/build/mvn-repo" \
    --settings "${BASE_DIR}/build/maven-settings.xml" \
    --show-version \
    --batch-mode \
    --no-transfer-progress \
    --file "${BASE_DIR}/pom.xml" \
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

is_valid_image_tag() { # tag
  : "${1?is_valid_image_tag(): missing argumnent}"
  tag="$1"
  echo "o ---> Searching for image \"$tag\" in container registry"
  docker_login
  image="${CI_REGISTRY_IMAGE}:${tag}"
  if docker manifest inspect "${image}" >/dev/null 2>&1; then
    echo "o ---> Image found"
    return 0
  else
    echo "o ---> No such image: \"${image}\""
    return 1
  fi
}

is_repository_dirty() {
  test -n "$(git status --porcelain)"
}

docker_login() {
  echo 'o ---> Authenticating with container registry'
  if is_gitlab_pipeline; then
    : "${CI_JOB_TOKEN:?Missing value for CI_JOB_TOKEN}"
    : "${CI_REGISTRY:?Missing value for CI_REGISTRY}"
    echo -n "${CI_JOB_TOKEN}" | docker login -u "gitlab-ci-token" --password-stdin "${CI_REGISTRY}"
  else
    : "${CI_REGISTRY_USER:?Missing value for CI_REGISTRY_USER}"
    : "${CI_REGISTRY_PASSWORD:?Missing value for CI_REGISTRY_PASSWORD}"
    : "${CI_REGISTRY:?Missing value for CI_REGISTRY}"
    echo -n "${CI_REGISTRY_PASSWORD}" | docker login -u "${CI_REGISTRY_USER}" --password-stdin "${CI_REGISTRY}"
  fi
}