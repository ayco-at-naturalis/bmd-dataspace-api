#!/usr/bin/env bash

set -euo pipefail

# Are we running inside a GitLab pipeline? Then we have CI_JOB_ID in our env.
GITLAB_PIPELINE="${CI_JOB_ID:-}"

if [[ -n "${GITLAB_PIPELINE}" ]]; then
  BASE_DIR="${CI_PROJECT_DIR}"
  if [ -z "${COMMIT_HASH_IMAGE:-}" ]; then
    echo 'Missing variable COMMIT_HASH_IMAGE in .gitlab-ci.yml'
    exit 1
  fi
else
  BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
  if [[ ! -f "${BASE_DIR}/build/build.env" ]]; then
    echo 'Local builds require presence of build.env'
    echo 'Copy build.env.template to build.env and modify as appropriate'
    exit 1
  fi
  source "${BASE_DIR}/build/build.env"
fi

source "${BASE_DIR}/build/functions.sh"

if [[ -n "${GITLAB_PIPELINE}" ]]; then
  if [[ -z "${COMMIT_HASH_IMAGE:-}" ]]; then
    echo 'Missing variable COMMIT_HASH_IMAGE in .gitlab-ci.yml'
    exit 1
  fi
else
  CI_COMMIT_REF_SLUG="$(get_commit_ref_slug)" # defined in functions.sh
  CI_COMMIT_SHA=$(git -C "${BASE_DIR}" log -n 1 --pretty=format:'%H')
  CI_COMMIT_TAG=$(git describe --tags --exact-match HEAD 2>/dev/null || echo "")
  COMMIT_HASH_IMAGE="${CI_REGISTRY_IMAGE}:${CI_COMMIT_REF_SLUG}_${CI_COMMIT_SHA}"
fi

echo "Tagging Docker image for commit \"${CI_COMMIT_SHA}\""

echo '---> Authenticating with GitLab container registry'
echo -n "${CI_REGISTRY_PASSWORD}" | docker login -u "${CI_REGISTRY_USER}" --password-stdin "${CI_REGISTRY}"

echo "---> Pulling ${COMMIT_HASH_IMAGE}"
docker pull "${COMMIT_HASH_IMAGE}"

echo "---> Adding tag \"${CI_COMMIT_REF_SLUG}\""
docker tag "${COMMIT_HASH_IMAGE}" "${CI_REGISTRY_IMAGE}:${CI_COMMIT_REF_SLUG}"
docker push "${CI_REGISTRY_IMAGE}:${CI_COMMIT_REF_SLUG}"

if [[ -n "${CI_COMMIT_TAG:-}" ]]; then
  echo "---> Adding tag \"${CI_COMMIT_TAG}\""
  docker tag "${COMMIT_HASH_IMAGE}" "${CI_REGISTRY_IMAGE}:${CI_COMMIT_TAG}"
  docker push "${CI_REGISTRY_IMAGE}:${CI_COMMIT_TAG}"
  exit 0
fi

# EXTRA_IMAGE_TAG can optionally be defined in build.env
if [[ -n "${EXTRA_IMAGE_TAG:-}" ]]; then
  tag=${EXTRA_IMAGE_TAG}
  echo "---> Adding tag \"${tag}\""
  docker tag "${COMMIT_HASH_IMAGE}" "${CI_REGISTRY_IMAGE}:${tag}"
  docker push "${CI_REGISTRY_IMAGE}:${tag}"
fi

if [[ "${CI_COMMIT_REF_SLUG}" = "main" ]]; then
  echo "---> Adding tag \"latest\""
  docker tag "${COMMIT_HASH_IMAGE}" "${CI_REGISTRY_IMAGE}:latest"
  docker push "${CI_REGISTRY_IMAGE}:latest"
fi

echo
