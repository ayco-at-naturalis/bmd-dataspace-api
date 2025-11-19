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

image_url="${CI_REGISTRY_IMAGE}:${CI_COMMIT_SHA}"
echo "---> Image URL: ${image_url}"

echo '---> Deleting older version of image (if present)'
docker image rm --force ${image_url} &> /dev/null

echo '---> Creating image'
docker build \
      --no-cache \
      --pull \
      --tag "${image_url}" \
      -f "${BASE_DIR}/build/Dockerfile" \
      "${BASE_DIR}/build"

echo '---> Authenticating with GitLab container registry'
echo -n ${CI_REGISTRY_PASSWORD} | docker login -u ${CI_REGISTRY_USER} --password-stdin ${CI_REGISTRY}

echo '---> Pushing image to container registry'
docker push "${image_url}"

echo '---> Image created and pushed'