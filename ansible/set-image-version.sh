#!/bin/bash

# set-image-version.sh
# Identifies the Docker image to deploy and writes it to ansible/.image-version.

set -eu

echo "Identifying Docker image to be deployed"

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

VERSION_FILE="${BASE_DIR}/ansible/.image-version"

if ! is_gitlab_pipeline; then
  if [ ! -f "${BASE_DIR}/ansible/deploy.env" ]; then
    echo 'Locally executed deployments require presence of deploy.env'
    echo 'Copy deploy.env.template to deploy.env and modify as appropriate'
    exit 1
  fi
  # deploy.env might specify a value for IMAGE_VERSION
  source "${BASE_DIR}/ansible/deploy.env"
fi

if ! is_gitlab_pipeline; then
  CI_COMMIT_REF_SLUG="$(get_commit_ref_slug)" # defined in build/functions.sh
  CI_COMMIT_SHA=$(git -C "${BASE_DIR}" log -n 1 --pretty=format:'%H')
  CI_COMMIT_TAG=$(git describe --tags --exact-match HEAD 2>/dev/null || echo "")
fi

# Only auto-compute Docker image version if it is not set yet
# in the environment. Allow IMAGE_VERSION to be set ad hoc in
# o .gitlab-ci.yml
# o as a pipeline variable
# o in deploy.env
if [ -z "${IMAGE_VERSION-}" ]; then
  if [ -n "${CI_COMMIT_TAG-}" ]; then
    IMAGE_VERSION="${CI_COMMIT_TAG}"
  else
    IMAGE_VERSION="${CI_COMMIT_REF_SLUG}_${CI_COMMIT_SHA}"
  fi
fi

mkdir -p "$(dirname "$VERSION_FILE")"
printf '%s' "$IMAGE_VERSION" > "$VERSION_FILE"

if ! image_tag_exists "$IMAGE_VERSION"; then
  echo "Image tag \"$IMAGE_VERSION\" not found in container registry" >&2
  echo "You probably did a local commit, but did not rebuild the container" >&2
  echo "image afterwards" >&2
  exit 1
fi

echo "Docker image to be deployed: $CI_REGISTRY_IMAGE:$IMAGE_VERSION"
echo
