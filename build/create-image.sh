#!/bin/sh

# create-image.sh
# Creates the original, commit-based Docker image, which will later be decorated
# with other tags using tag-image.sh. Note that we cannot use bash because the
# Kaniko container does not support it.

set -eu

# Are we running inside a GitLab pipeline?
is_gitlab_pipeline() {
  [ -n "${CI_JOB_ID-}" ]
}

if is_gitlab_pipeline; then
  BASE_DIR="${CI_PROJECT_DIR}"
else # Resolve using this script's location
  BASE_DIR=$(cd "$(dirname "$0")/.." && pwd)
fi

. "${BASE_DIR}/build/functions.sh"

if ! is_gitlab_pipeline; then
  if [ ! -f "${BASE_DIR}/build/build.env" ]; then
    echo 'Local builds require presence of build.env'
    echo 'Copy build.env.template to build.env and modify as appropriate'
    exit 1
  fi
  . "${BASE_DIR}/build/build.env"
fi

if ! is_gitlab_pipeline; then
  CI_COMMIT_REF_SLUG="$(get_commit_ref_slug)" # defined in functions.sh
  CI_COMMIT_SHA=$(git -C "${BASE_DIR}" log -n 1 --pretty=format:'%H')
fi

echo "Creating Docker image for commit \"${CI_COMMIT_SHA}\""

if [ ! -f "${BASE_DIR}/build/app/version-info.json" ]; then
  echo "Missing file version-info.json. Check generate-version-info.sh" >&2
  exit 1
fi

COMMIT_HASH_IMAGE="${CI_REGISTRY_IMAGE}:${CI_COMMIT_REF_SLUG}_${CI_COMMIT_SHA}"

KANIKO_CONFIG="{ \"auths\": { \"${CI_REGISTRY}\": { \"username\": \"${CI_REGISTRY_USER}\", \"password\": \"${CI_REGISTRY_PASSWORD}\" }}}"

if is_gitlab_pipeline; then
  mkdir -p /kaniko/.docker
  echo "${KANIKO_CONFIG}" > /kaniko/.docker/config.json
  /kaniko/executor \
    --context "${BASE_DIR}/build" \
    --destination "${COMMIT_HASH_IMAGE}" \
    --custom-platform linux/amd64 \
    --cache=true \
    --cache-repo="${CI_REGISTRY_IMAGE}/cache"

else
  # NB The kaniko directory is a git-ignored directory
  KANIKO_TMP="${BASE_DIR}/build/kaniko"
  rm -rf "${KANIKO_TMP}"
  mkdir -p "${KANIKO_TMP}/.docker"
  # Nevertheless, ensure it gets deleted when the script exits:
  trap 'rm -rf "$KANIKO_TMP"' EXIT
  echo "${KANIKO_CONFIG}" > "${KANIKO_TMP}/.docker/config.json"
  docker run --rm \
    -v "${BASE_DIR}/build:/workspace" \
    -v "${KANIKO_TMP}/.docker:/kaniko/.docker" \
    gcr.io/kaniko-project/executor \
      --context /workspace \
      --destination "${COMMIT_HASH_IMAGE}" \
      --custom-platform linux/amd64 \
      --cache=true \
      --cache-repo "${CI_REGISTRY_IMAGE}/cache"
fi

echo
echo '---> Image created and pushed'
echo
