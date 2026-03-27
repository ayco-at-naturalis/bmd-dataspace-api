#!/bin/bash

# set-image-version.sh
# Identifies the tag of the Docker image to be deployed and writes it to
# ansible/.image-version.

set -eu

echo "o ---> Identifying Docker image to be deployed"

# Are we running inside a GitLab pipeline?
is_gitlab_pipeline() {
  [ -n "${CI_JOB_ID-}" ]
}

if is_gitlab_pipeline; then
  BASE_DIR="${CI_PROJECT_DIR}"
else # Resolve using this script's location
  BASE_DIR=$(cd "$(dirname "$0")/.." && pwd)
fi

source "${BASE_DIR}/build/functions.sh"
source "${BASE_DIR}/ansible/functions.sh"

if ! is_gitlab_pipeline; then
  if [[ ! -f "${BASE_DIR}/ansible/deploy.env" ]]; then
    echo 'o ---> Local builds require presence of deploy.env' >&2
    echo 'o ---> Copy deploy.env.template to deploy.env and modify as appropriate' >&2
    exit 1
  fi
  source "${BASE_DIR}/ansible/deploy.env"
fi

if ! is_gitlab_pipeline; then
  CI_COMMIT_REF_SLUG="$(get_commit_ref_slug)" # defined in build/functions.sh
  CI_COMMIT_SHA=$(git -C "${BASE_DIR}" log -n 1 --pretty=format:'%H')
  CI_COMMIT_TAG=$(git describe --tags --exact-match HEAD 2>/dev/null || echo "")
fi

# Only auto-compute IMAGE_VERSION if it is not set already in
# the environment. Allow IMAGE_VERSION to be set (ad hoc) in:
#   - .gitlab-ci.yml
#   - as a pipeline variable
#   - in deploy.env
if [ -z "${IMAGE_VERSION-}" ]; then
  if [ -n "${CI_COMMIT_TAG-}" ]; then
    IMAGE_VERSION="${CI_COMMIT_TAG}"
  else
    IMAGE_VERSION="${CI_COMMIT_REF_SLUG}"
  fi
fi

mkdir -p "$(dirname "$VERSION_FILE")" # VERSION_FILE is defined in ansible/functions.sh
printf '%s' "$IMAGE_VERSION" > "$VERSION_FILE"

echo "o ---> Image version to be deployed: \"${IMAGE_VERSION}"
