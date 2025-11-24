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
  CI_COMMIT_REF_SLUG="$(git rev-parse --abbrev-ref HEAD)"
  CI_COMMIT_SHA="$(git log -n 1 --pretty=format:'%H')"
  CI_COMMIT_SHORT_SHA="$(git log -n 1 --pretty=format:'%h')"
fi

echo "Tagging Docker image for commit \"${CI_COMMIT_SHA}\""

image="${CI_REGISTRY_IMAGE}:${CI_COMMIT_SHA}"

echo "---> Pulling ${image}"
docker pull "${image}"

echo '---> Authenticating with GitLab container registry'
echo -n ${CI_REGISTRY_PASSWORD} | docker login -u ${CI_REGISTRY_USER} --password-stdin ${CI_REGISTRY}


echo "---> Adding tag \"${CI_COMMIT_REF_SLUG}\""
docker tag "${image}" "${CI_REGISTRY_IMAGE}:${CI_COMMIT_REF_SLUG}"
docker push "${CI_REGISTRY_IMAGE}:${CI_COMMIT_REF_SLUG}"

if [[ -n "${CI_COMMIT_TAG:-}" ]]; then
  echo "---> Adding tag \"${CI_COMMIT_TAG}\""
  docker tag "${image}" "${CI_REGISTRY_IMAGE}:${CI_COMMIT_TAG}"
  docker push "${CI_REGISTRY_IMAGE}:${CI_COMMIT_TAG}"
  exit 0
fi

if [[ -n "${EXTRA_IMAGE_TAG:-}" ]]; then
  tag=${EXTRA_IMAGE_TAG}
  echo "---> Adding tag \"${tag}\""
  docker tag "${image}" "${CI_REGISTRY_IMAGE}:${tag}"
  docker push "${CI_REGISTRY_IMAGE}:${tag}"
fi

if [[ "${CI_COMMIT_REF_SLUG}" = "main" ]]; then
  echo "---> Adding tag \"latest\""
  docker tag "${image}" "${CI_REGISTRY_IMAGE}:latest"
  docker push "${CI_REGISTRY_IMAGE}:latest"
fi
