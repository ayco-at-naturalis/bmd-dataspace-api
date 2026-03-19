#!/bin/bash

# Orchestrates the deployment. Calls the other shell scripts in
# the proper order.

set -euo pipefail

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

echo
echo "o ---> Starting deployment"
echo "o ---> Target host: ${DEPLOY_HOST}"

echo "o ---> Performing pre-deployment cleanup"
"${BASE_DIR}/ansible/cleanup.sh"
trap '"${BASE_DIR}/ansible/cleanup.sh"' EXIT
"${BASE_DIR}/ansible/get-vault-token.sh"
"${BASE_DIR}/ansible/pull-vault-secrets.sh"
"${BASE_DIR}/ansible/set-image-version.sh"
"${BASE_DIR}/ansible/playbook.sh"
echo "o ---> Performing post-deployment cleanup"
"${BASE_DIR}/ansible/cleanup.sh"

echo 'o ---> Done'
