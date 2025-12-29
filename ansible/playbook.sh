#!/bin/bash

set -euo pipefail

echo "Executing deployment playbook"

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
    echo 'Local builds require presence of deploy.env'
    echo 'Copy deploy.env.template to deploy.env and modify as appropriate'
    exit 1
  fi
  source "${BASE_DIR}/ansible/deploy.env"
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
