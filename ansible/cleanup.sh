#!/bin/bash

# cleanup.sh
# Cleans up directories and files created during the deployment. These
# directories and files are git-ignored, but let's delete them anyway.

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

ANSIBLE_DIR="${BASE_DIR}/ansible"

rm -rf \
      "${ANSIBLE_DIR}/.vault-token" \
      "${ANSIBLE_DIR}/.image-version" \
      "${ANSIBLE_DIR}/.inventory.json" \
      "${ANSIBLE_DIR}/inventory/group_vars" \
      "${ANSIBLE_DIR}/inventory/host_vars" \
      "${ANSIBLE_DIR}/ssh"
