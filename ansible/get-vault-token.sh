#!/usr/bin/env bash
# shellcheck disable=SC2155

set -euo pipefail

# Are we running inside a GitLab pipeline?
GITLAB_PIPELINE=$(if [[ -z "${CI_JOB_ID:-}" ]]; then echo false; else echo true; fi)

if [[ ${GITLAB_PIPELINE} = true ]]; then
  BASE_DIR="${CI_PROJECT_DIR}"
else
  BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fi

: "${VAULT_SERVER_URL:?Missing value for VAULT_SERVER_URL}"
: "${VAULT_AUTH_ROLE:?Missing value for VAULT_AUTH_ROLE}"
: "${VAULT_ID_TOKEN:?Missing value for VAULT_ID_TOKEN}"

JSON_BODY=$(jq -nc --arg role "$VAULT_AUTH_ROLE" --arg jwt "$VAULT_ID_TOKEN" '{role:$role,jwt:$jwt}')
# Note: Content-Type header has no space after the colon to avoid
#       YAML parsing issues when inlined into .gitlab-ci.yml
RESP=$(curl -fsS -H 'Content-Type:application/json' --data "$JSON_BODY" "$VAULT_SERVER_URL/v1/auth/jwt/login")
VAULT_TOKEN=$(printf '%s' "$RESP" | jq -r '.auth.client_token')
if [[ -z "$VAULT_TOKEN" ]] || [[ "$VAULT_TOKEN" = "null" ]]; then
  printf >&2 'Error: did not receive a Vault token. Response was:\n%s\n' "$RESP"
  exit 1
fi

echo "$VAULT_TOKEN" > "${BASE_DIR}/.vault-token"
