#!/bin/bash

# get-vault-token.sh
# Retrieves an HC Vault token and saves it to ansible/.vault-token. If that
# file already already exists and the token inside it is still valid, no new
# token is requested. The location of the token file (ansible/.vault-token)
# is stored in variable TOKEN_FILE, which is defined in functions.sh.

set -euo pipefail

echo "o ---> Retrieving HC Vault token"

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

source "${BASE_DIR}/ansible/functions.sh"

: "${VAULT_SERVER_URL:?Missing value for VAULT_SERVER_URL}"

if is_gitlab_pipeline; then
  # Rely on variables being set in .gitlab-ci.yml
  JSON_BODY=$(jq -nc --arg role "$VAULT_AUTH_ROLE" --arg jwt "$VAULT_ID_TOKEN" '{role:$role,jwt:$jwt}')
  # NB Content-Type header has no space after the colon to
  # avoid YAML parsing issues if we ever want to inline this
  # again into .gitlab-ci.yml!
  RESP=$(curl -fsS -H 'Content-Type:application/json' --data "$JSON_BODY" "$VAULT_SERVER_URL/v1/auth/jwt/login")
  VAULT_TOKEN=$(printf '%s' "$RESP" | jq -r '.auth.client_token')
  if [[ -z "$VAULT_TOKEN" ]] || [[ "$VAULT_TOKEN" == "null" ]]; then
    printf '%s\no ---> Failed to retrieve vault token\n' "${RESP}" >&2
    exit 1
  fi
  mkdir -p "$(dirname "$TOKEN_FILE")"
  printf '%s' "$VAULT_TOKEN" > "$TOKEN_FILE"
  chmod 600 "$TOKEN_FILE"
  echo "o ---> Vault token ready to be used"
  exit 0
fi

# Rely on variables being set in deploy.env
echo "o ---> MAKE SURE EduVPN IS ACTIVE AND YOU ARE LOGGED INTO YOUR AZURE AD ACCOUNT!"

if ! curl "$VAULT_SERVER_URL" -m 1 &> /dev/null; then
  echo 'o ---> Failed to retrieve vault token. HC Vault not reachable' >&2
  exit 1
fi

# Make sure we have the HC Vault client.
ensure_vault_cli

export VAULT_ADDR="$VAULT_SERVER_URL"
if [[ -f "${TOKEN_FILE}" ]]; then
  if VAULT_TOKEN=$(<"${TOKEN_FILE}") vault token lookup > /dev/null 2>&1; then
    echo "o ---> Vault token still valid"
    exit 0
  fi
fi

VAULT_TOKEN="$(vault login -method=oidc -token-only)"
mkdir -p "$(dirname "$TOKEN_FILE")"
printf '%s' "$VAULT_TOKEN" > "$TOKEN_FILE"
chmod 600 "$TOKEN_FILE"
echo
echo "o ---> Vault token ready to be used"
exit 0
