#!/usr/bin/env bash

# get-vault-token.sh
# Retrieves an HC Vault token

echo "Retrieving HC Vault token"

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

source "${BASE_DIR}/ansible/functions.sh"

if ! is_gitlab_pipeline; then
  if [[ ! -f "${BASE_DIR}/ansible/deploy.env" ]]; then
    echo 'Local builds require presence of deploy.env'
    echo 'Copy deploy.env.template to deploy.env and modify as appropriate'
    exit 1
  fi
  source "${BASE_DIR}/ansible/deploy.env"
fi

TOKEN_FILE="${BASE_DIR}/ansible/.vault-token"

if is_gitlab_pipeline; then
  # Rely on variables being set in .gitlab-ci.yml
  JSON_BODY=$(jq -nc --arg role "$VAULT_AUTH_ROLE" --arg jwt "$VAULT_ID_TOKEN" '{role:$role,jwt:$jwt}')
  # NB Content-Type header has no space after the colon to
  # avoid YAML parsing issues if we ever want to inline this
  # again into .gitlab-ci.yml!
  RESP=$(curl -fsS -H 'Content-Type:application/json' --data "$JSON_BODY" "$VAULT_SERVER_URL/v1/auth/jwt/login")
  VAULT_TOKEN=$(printf '%s' "$RESP" | jq -r '.auth.client_token')
  if [[ -z "$VAULT_TOKEN" ]] || [[ "$VAULT_TOKEN" == "null" ]]; then
    printf '%s\nFailed to retrieve vault token\n' "${RESP}" >&2
    exit 1
  fi
  mkdir -p "$(dirname "$TOKEN_FILE")"
  printf '%s' "$VAULT_TOKEN" > "$TOKEN_FILE"
  chmod 600 "$TOKEN_FILE"
  echo "Vault token ready to be used"
  exit 0
fi

# Rely on variables being set in deploy.env
echo "---> MAKE SURE EduVPN IS ACTIVE!"
echo "---> Local deployments may require you to login via Azure AD and use your 2F app"

if ! curl "$VAULT_SERVER_URL" -m 1 &> /dev/null; then
  echo 'Failed to retrieve vault token. HC Vault not reachable' >&2
  exit 1
fi

ensure_vault_cli # defined in functions.sh

export VAULT_ADDR="$VAULT_SERVER_URL"
if [[ -f "${TOKEN_FILE}" ]]; then
  if VAULT_TOKEN=$(<"${TOKEN_FILE}") vault token lookup > /dev/null 2>&1; then
    echo "Vault token still valid"
    exit 0
  fi
fi

VAULT_TOKEN="$(vault login -method=oidc -token-only)"
mkdir -p "$(dirname "$TOKEN_FILE")"
printf '%s' "$VAULT_TOKEN" > "$TOKEN_FILE"
chmod 600 "$TOKEN_FILE"
echo "HC Vault token ready to be used"
exit 0



