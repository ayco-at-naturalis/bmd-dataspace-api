#!/bin/sh

# tag-image.sh
# Adds various extra tags to the image created by create-image.sh

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

echo "o ---> Tagging Docker image for commit \"$(get_commit_short_sha)\""

docker_login

base_image="$(get_base_image)"
echo "o ---> Pulling ${base_image}"
docker pull "${base_image}"

# Add tag named after the git branch
add_tag "$(get_commit_ref_slug)"

# If this is a tagged commit, add an image tag named after the git tag
tag="$(get_commit_tag)"
if [ -n "${tag}" ]; then
  add_tag "${tag}"
fi

# EXTRA_IMAGE_TAG can optionally be specified in .gitlab-ci.yml, or as
# as pipeline variable, or in build.env
tag="${EXTRA_IMAGE_TAG-}"
if [ -n "${EXTRA_IMAGE_TAG-}" ]; then
  add_tag "${tag}"
fi

if [ "$(get_commit_ref_slug)" = "main" ]; then
  add_tag "latest"
fi

echo
echo "o ---> Image tags successfully added"
echo

if ! is_gitlab_pipeline && is_repository_dirty; then
  echo
  echo "********************************************************"
  echo "** WARNING: You are building from a dirty repository! **"
  echo "********************************************************"
  echo
fi
