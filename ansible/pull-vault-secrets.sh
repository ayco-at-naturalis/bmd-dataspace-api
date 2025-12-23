#!/usr/bin/env bash

# pull-vault-secrets.sh
# Reads secrets from HC Vault for host $DEPLOY_HOST. DEPLOY_HOST must be
# defined in .gitlab-ci.yml (for GitLab pipelines) or in deploy.env (for
# local deployments).

set -euo pipefail

echo "Retrieving secrets"

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

source "${BASE_DIR}/ansible/functions.sh"

: "${VAULT_SERVER_URL:?Missing value for VAULT_SERVER_URL}"
: "${VAULT_PATH:?Missing value for VAULT_PATH}"
: "${DEPLOY_HOST:?Missing value for DEPLOY_HOST}"

# TOKEN_FILE is defined in functions.sh
if [[ ! -f "${TOKEN_FILE}" ]]; then
	echo "Vault token not generated. Check get-vault-token.sh" >&2
	exit 1
fi

mkdir -p "${BASE_DIR}/ansible/inventory/group_vars" \
	"${BASE_DIR}/ansible/inventory/host_vars" \
	"${BASE_DIR}/ansible/secrets"

echo "Retrieving Ansible groups for host \"${DEPLOY_HOST}\""
ANSIBLE_GROUPS="$(get_ansible_groups "${DEPLOY_HOST}")"
echo "---> Host \"${DEPLOY_HOST}\" belongs to these groups: ${ANSIBLE_GROUPS:-(none)}"

# group_vars
get_secret_as_json 'cicd/group_vars/all' 'ansible/inventory/group_vars/all.json'
for group in ${ANSIBLE_GROUPS:-}; do
	get_secret_as_json "cicd/group_vars/${group}" "ansible/inventory/group_vars/${group}.json"
done

# host_vars
get_secret_as_json "cicd/host_vars/${DEPLOY_HOST}" "ansible/inventory/host_vars/${DEPLOY_HOST}.json"

# AWS credentials
get_secret_as_json 'tenant/letsencrypt' 'ansible/inventory/host_vars/aws.json'
FULL_PATH="${BASE_DIR}/ansible/inventory/host_vars/aws.json"
[[ ! -f "${FULL_PATH}" ]] && echo "ERROR: Missing AWS credentials" >&2 && exit 1

# SSH key for Ansible
get_secret_as_json 'cicd/ssh' 'ansible/secrets/ssh.json'
FULL_PATH="${BASE_DIR}/ansible/secrets/ssh.json"
[[ ! -f "${FULL_PATH}" ]] && echo "ERROR: Missing SSH key" >&2 && exit 1

# Generate an id_ed25519 file using ssh.json
ANSIBLE_DEPLOY_KEY="$(jq -e -r ".ANSIBLE_DEPLOY_KEY" "${FULL_PATH}")"
[[ -z "${ANSIBLE_DEPLOY_KEY:-}" ]] && echo "ERROR: Unable to read SSH key" >&2 && exit 1
echo "${ANSIBLE_DEPLOY_KEY}" > "${BASE_DIR}/ansible/secrets/id_ed25519"
chmod 0400 "${BASE_DIR}/ansible/secrets/id_ed25519"
