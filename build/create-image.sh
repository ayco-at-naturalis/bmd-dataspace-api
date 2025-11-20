#!/usr/bin/env bash

set -euo pipefail

# Are we running inside a GitLab pipeline? Then we have CI_JOB_ID in our env.
GITLAB_PIPELINE="${CI_JOB_ID:-}"

if [[ "${GITLAB_PIPELINE}" ]]; then
  BASE_DIR="${CI_PROJECT_DIR}"
else
  BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
  if [[ ! -f "${BASE_DIR}/build/build.env" ]]; then
    echo 'Local builds require presence of build.env'
    echo 'Copy build.env.template to build.env and modify as appropriate'
    exit 1
  fi
  source "${BASE_DIR}/build/build.env"
  CI_COMMIT_SHA="$(git log -n 1 --pretty=format:'%H')"
fi

echo "Creating Docker image for commit \"${CI_COMMIT_SHA}\""

APP_IMAGE="${CI_REGISTRY_IMAGE}:${CI_COMMIT_SHA}"
KANIKO_CONFIG="{ \"auths\": { \"${CI_REGISTRY}\": { \"username\": \"${CI_REGISTRY_USER}\", \"password\": \"${CI_REGISTRY_PASSWORD}\" }}}"

if [[ "${GITLAB_PIPELINE}" ]]; then
  mkdir -p /kaniko/.docker
  echo "${KANIKO_CONFIG}" >> /kaniko/.docker/config.json
  /kaniko/executor \
    --context "${BASE_DIR}/build" \
    --dockerfile "${BASE_DIR}/build/Dockerfile" \
    --destination "${APP_IMAGE}" \
    --custom-platform linux/amd64 \
    --cache=true \
    --cache-repo=${CI_REGISTRY_IMAGE}/cache

else
  # NB The kaniko directory is a git-ignored directory
  rm -rf "${BASE_DIR}/build/kaniko"
  mkdir -p "${BASE_DIR}/build/kaniko/.docker"
  echo "${KANIKO_CONFIG}" >> "${BASE_DIR}/build/kaniko/.docker/config.json"
  docker run --rm \
    -v "${BASE_DIR}/build:/workspace" \
    -v "${BASE_DIR}/build/kaniko/.docker:/kaniko/.docker" \
    gcr.io/kaniko-project/executor:debug \
      --context /workspace \
      --dockerfile "/workspace/Dockerfile" \
      --destination "${APP_IMAGE}" \
      --custom-platform linux/amd64 \
      --cache=true \
      --cache-repo=${CI_REGISTRY_IMAGE}/cache
  rm -rf "${BASE_DIR}/build/kaniko"
fi

echo
echo '---> Image created and pushed'
echo