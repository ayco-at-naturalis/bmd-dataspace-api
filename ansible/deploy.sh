#!/bin/bash

# Driver script for the Ansible deployment. START HERE.

set -u

# Are we running inside a Gitlab pipeline?
GITLAB_PIPELINE=true
[[ -z ${CI_JOB_ID+x} ]] && GITLAB_PIPELINE=false
export GITLAB_PIPELINE

if [[ ${GITLAB_PIPELINE} = true ]]; then
  BASE_DIR="${CI_PROJECT_DIR}"
else
  BASE_DIR=$(dirname "${0}")/../
  BASE_DIR=$(realpath "${BASE_DIR}")
fi
export BASE_DIR

if [[ ${GITLAB_PIPELINE} = false ]]; then
  f="${BASE_DIR}/ansible/deploy.env"
  if [[ ! -f "${f}" ]]; then
    echo 'For locally executed deployments, the following file is required:'
    echo 'ansible/deploy.env. This file will now be created. Edit the file'
    echo 'and then run deploy.sh again'
    source "${BASE_DIR}/ansible/create_deploy_env.sh"
    exit 1
  fi
  echo 'Setting up local environment for locally executed deployment ...'
  source "${f}"
  read -p "You will be deploying to \"${ANSIBLE_HOSTS}\". Continue? [Y/n] " ok
  if [[ ! -z "${ok}" && "${ok}" != 'Y' ]]; then
    echo 'Aborting deployment'
    exit 1
  fi
fi

echo 'Reading secrets ...'
source "${BASE_DIR}/ansible/pull_vault_secrets.sh"
if [[ ${?} -ne 0 ]]; then
  exit 1
fi

echo 'Determining image version ...'
source "${BASE_DIR}/ansible/export_image_version.sh"
echo "About to deploy version \"${IMAGE_VERSION}\""

echo 'Executing Ansible playbook ...'
if [[ ${GITLAB_PIPELINE} == false ]]; then
  if [[ -z "$(which ansible-galaxy)" ]]; then
    echo 'ansible-galaxy not installed; must be present for locally executed deployments'
    exit 1
  fi
  echo 'Installing extra Ansible roles ...'
  ansible-galaxy role install --force -r requirements.yml
fi
source "${BASE_DIR}/ansible/playbook.sh"

echo 'Deleting secrets ...'
# These directories are git-ignored, but let's delete them anyway.
rm -rf "${BASE_DIR}/ansible/inventory/group_vars"
rm -rf "${BASE_DIR}/ansible/inventory/host_vars"
rm -rf "${BASE_DIR}/ansible/secrets"
echo 'Done'
