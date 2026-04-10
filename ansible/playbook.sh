#!/bin/bash

set -euo pipefail

echo "o ---> Executing deployment Ansible playbook"

# Are we running inside a GitLab pipeline?
is_gitlab_pipeline() {
 [ -n "${CI_JOB_ID-}" ]
}

if is_gitlab_pipeline; then
  BASE_DIR="${CI_PROJECT_DIR}"
else # Resolve using this script's location
  BASE_DIR=$(cd "$(dirname "$0")/.." && pwd)
fi

if ! is_gitlab_pipeline; then
  if [[ ! -f "${BASE_DIR}/ansible/deploy.env" ]]; then
    echo 'o ---> Local builds require presence of deploy.env' >&2
    echo 'o ---> Copy deploy.env.template to deploy.env and modify as appropriate' >&2
    exit 1
  fi
  source "${BASE_DIR}/ansible/deploy.env"
fi

source "${BASE_DIR}/build/functions.sh"
source "${BASE_DIR}/ansible/functions.sh"

# VERSION_FILE is defined in ansible/functions.sh
if [ ! -f "${VERSION_FILE}" ]; then
  echo "o ---> Missing ${VERSION_FILE}" >&2
  echo "o ---> Run set-image-version.sh first" >&2
  exit 1
fi

IMAGE_VERSION="$(<"${VERSION_FILE}")"

if ! is_valid_image_tag "$IMAGE_VERSION"; then
  echo "o ---> Image tag \"$IMAGE_VERSION\" not found in container registry" >&2
  echo "o ---> You probably did a local commit, but did not build an image for it" >&2
  echo "o ---> Run build.sh first" >&2
  exit 1
fi

docker run \
    -v "${BASE_DIR}/:/runner/" \
    -v "${BASE_DIR}/ansible/ssh/:/root/.ssh" \
    -w /runner \
    -e ANSIBLE_CONFIG=/runner/ansible/ansible.cfg \
    -e IMAGE_VERSION \
    -e CI_REGISTRY \
    -e CI_REGISTRY_USER \
    -e CI_REGISTRY_PASSWORD \
    -t \
    registry.gitlab.com/naturalis/lib/ansible/docker-ansible:latest ansible-playbook \
    -i "ansible/inventory/hosts.yml" \
    -l "${DEPLOY_HOST}" \
    ansible/deploy.yml

if ! is_gitlab_pipeline && is_repository_dirty; then
  echo
  echo "*********************************************************"
  echo "** WARNING: You are deploying from a dirty repository! **"
  echo "*********************************************************"
  echo
fi
