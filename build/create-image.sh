#!/bin/sh

# create-image.sh
# Creates the original, commit-based Docker image, which will later be decorated
# with other tags using tag-image.sh.

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
    echo 'o ---> Local builds require presence of build.env' >&2
    echo 'o ---> Copy build.env.template to build.env and modify as appropriate' >&2
    exit 1
  fi
  . "${BASE_DIR}/build/build.env"
fi

echo "o ---> Creating Docker image for commit $(get_commit_short_sha)"
base_image="$(get_base_image)"
echo "o ---> Image URL: ${base_image}"
echo 'o ---> Deleting older version of image (if present)'
docker image rm --force "${base_image}" > /dev/null 2>&1
docker build \
    --no-cache \
    --pull \
    --tag "${base_image}" \
    -f "${BASE_DIR}/build/Dockerfile" \
    "${BASE_DIR}/build"
echo 'o ---> Image created. Pushing image to container registry'
docker_login
docker image push "${base_image}"
echo
echo 'o ---> Image successfully created and pushed'
echo

if ! is_gitlab_pipeline && is_repository_dirty; then
  echo
  echo "********************************************************"
  echo "** WARNING: You are building from a dirty repository! **"
  echo "********************************************************"
  echo
fi
