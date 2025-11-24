#!/bin/sh

set -eu

# Are we running inside a GitLab pipeline? Then we have CI_JOB_ID in our env.
if [ -n "${CI_JOB_ID:-}" ]; then
  GITLAB_PIPELINE=1
else
  GITLAB_PIPELINE=
fi

if [ -n "$GITLAB_PIPELINE" ]; then
  BASE_DIR="${CI_PROJECT_DIR}"
else
  # Resolve BASE_DIR from this script's location
  BASE_DIR=$(cd "$(dirname "$0")/.." && pwd)
  if [ ! -f "${BASE_DIR}/build/build.env" ]; then
    echo 'Local builds require presence of build.env'
    echo 'Copy build.env.template to build.env and modify as appropriate'
    exit 1
  fi
  # shellcheck disable=SC1090
  . "${BASE_DIR}/build/build.env"
  CI_COMMIT_SHA=$(git -C "${BASE_DIR}" log -n 1 --pretty=format:'%H')
fi

echo "Creating Docker image for commit \"${CI_COMMIT_SHA}\""

APP_IMAGE="${CI_REGISTRY_IMAGE}:${CI_COMMIT_SHA}"
KANIKO_CONFIG="{ \"auths\": { \"${CI_REGISTRY}\": { \"username\": \"${CI_REGISTRY_USER}\", \"password\": \"${CI_REGISTRY_PASSWORD}\" }}}"

if [ -n "$GITLAB_PIPELINE" ]; then
  mkdir -p /kaniko/.docker
  echo "${KANIKO_CONFIG}" > /kaniko/.docker/config.json
  /kaniko/executor \
    --context "${BASE_DIR}/build" \
    --destination "${APP_IMAGE}" \
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
      --destination "${APP_IMAGE}" \
      --custom-platform linux/amd64 \
      --cache=true \
      --cache-repo=${CI_REGISTRY_IMAGE}/cache
fi

echo
echo '---> Image created and pushed'
echo
