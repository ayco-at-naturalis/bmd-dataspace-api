#!/bin/bash

# pull-vault-secrets.sh
# Reads secrets from HC Vault for host $DEPLOY_HOST. DEPLOY_HOST must be
# defined in .gitlab-ci.yml (for GitLab pipelines) or in deploy.env (for
# local deployments).

set -euo pipefail

echo "o ---> Retrieving secrets"

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
INVENTORY_DIR="${ANSIBLE_DIR}/inventory"

if ! is_gitlab_pipeline; then
  if [[ ! -f "${BASE_DIR}/ansible/deploy.env" ]]; then
    echo 'o ---> Local builds require presence of deploy.env' >&2
    echo 'o ---> Copy deploy.env.template to deploy.env and modify as appropriate' >&2
    exit 1
  fi
  source "${ANSIBLE_DIR}/deploy.env"
fi

source "${ANSIBLE_DIR}/functions.sh"

: "${VAULT_SERVER_URL:?Missing value for VAULT_SERVER_URL}"
: "${VAULT_PATH:?Missing value for VAULT_PATH}"
: "${DEPLOY_HOST:?Missing value for DEPLOY_HOST}"

# TOKEN_FILE is defined in functions.sh
if [[ ! -f "${TOKEN_FILE}" ]]; then
	echo "o ---> Missing ${TOKEN_FILE}" >&2
	echo "o ---> Run get-vault-token.sh first" >&2
	exit 1
fi

echo "o ---> Deleting old secrets"
rm -rf "${INVENTORY_DIR}/group_vars" \
	"${INVENTORY_DIR}/host_vars" \
	"${ANSIBLE_DIR}/ssh"

mkdir -p "${INVENTORY_DIR}/group_vars" \
	"${INVENTORY_DIR}/host_vars" \
	"${ANSIBLE_DIR}/ssh"

echo "o ---> Retrieving Ansible groups for host \"${DEPLOY_HOST}\""
ANSIBLE_GROUPS="$(get_ansible_groups "${DEPLOY_HOST}")"
echo "o ---> Host \"${DEPLOY_HOST}\" is listed under these groups in hosts.yml : ${ANSIBLE_GROUPS:-(none)}"

# group_vars (we always check for the presence of an "all" secret in HC Vault)
get_secret_as_json "cicd/group_vars/all" "${INVENTORY_DIR}/group_vars/all.json"
for group in ${ANSIBLE_GROUPS:-}; do
	get_secret_as_json "cicd/group_vars/${group}" "${INVENTORY_DIR}/group_vars/${group}.json"
done

# host_vars
get_secret_as_json "cicd/host_vars/${DEPLOY_HOST}" "${INVENTORY_DIR}/host_vars/${DEPLOY_HOST}.json"

# AWS credentials. These are not managed and maintained by devs (inside the cicd
# folder) but by infra (inside the tenants/letsencrypt folder). NOTE: aws.json
# matches the name of an Ansible group (aws) in hosts.yml. Therefore Ansible will
# read and expose aws.json if it is present in the group_vars directory.
output_file="${INVENTORY_DIR}/group_vars/aws.json"
get_secret_as_json "tenant/letsencrypt" "${output_file}"
[[ ! -f "${output_file}" ]] && echo "o ---> ERROR: Missing AWS credentials" >&2 && exit 1

# SSH key for Ansible
output_file="${ANSIBLE_DIR}/ssh/ssh.json"
get_secret_as_json "cicd/ssh" "${output_file}"
[[ ! -f "${output_file}" ]] && echo "o ---> ERROR: Missing SSH key" >&2 && exit 1

# Generate an id_ed25519 file using ssh.json
ANSIBLE_DEPLOY_KEY="$(jq -e -r ".ANSIBLE_DEPLOY_KEY" "${output_file}")"
[[ -z "${ANSIBLE_DEPLOY_KEY:-}" ]] && echo "o ---> ERROR: Unable to read SSH key" >&2 && exit 1
echo "${ANSIBLE_DEPLOY_KEY}" > "${ANSIBLE_DIR}/ssh/id_ed25519"
chmod 0400 "${ANSIBLE_DIR}/ssh/id_ed25519"

echo 'o ---> All secrets retrieved and processed'