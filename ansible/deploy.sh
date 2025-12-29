#!/bin/bash

# Orchestrates the deployment. Calls the other shell scripts in
# the proper order.

set -euo pipefail

echo "Retrieving HC Vault token"

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

echo
echo "Starting deployment"
echo "---> Target host: ${DEPLOY_HOST}"

"${BASE_DIR}/ansible/get-vault-token.sh"
"${BASE_DIR}/ansible/pull-vault-secrets.sh"
"${BASE_DIR}/ansible/set-image-version.sh"

echo 'Executing Ansible playbook ...'
source "${BASE_DIR}/ansible/playbook.sh"

echo 'Deleting secrets ...'
# These directories are git-ignored, but let's delete them anyway.
rm -rf "${BASE_DIR}/ansible/inventory/group_vars"
rm -rf "${BASE_DIR}/ansible/inventory/host_vars"
rm -rf "${BASE_DIR}/ansible/secrets"
echo 'Done'
