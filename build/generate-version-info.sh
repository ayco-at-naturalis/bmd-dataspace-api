#!/bin/sh

# generate-version-info.sh
# Generates version-info.json, which will be copied into the classpath
# inside the Docker container. This file will be read by the /version
# endpoint.

set -eu

# Are we running inside a GitLab pipeline?
is_gitlab_pipeline() {
  [ -n "${CI_JOB_ID-}" ]
}

if is_gitlab_pipeline; then
  BASE_DIR="${CI_PROJECT_DIR}"
  # Ensure tags are available in pipeline. Not really necessary - already ensured within
  # .gitlab-ci.yml (with GIT_DEPTH variable set to 0):
  git fetch --force --tags > /dev/null 2>&1 || true
else # Resolve using this script's location
  BASE_DIR=$(cd "$(dirname "$0")/.." && pwd)
fi

. "${BASE_DIR}/build/functions.sh"

if ! is_gitlab_pipeline; then
  if [ ! -f "${BASE_DIR}/build/build.env" ]; then
    echo 'Local builds require presence of build.env'
    echo 'Copy build.env.template to build.env and modify as appropriate'
    exit 1
  fi
  . "${BASE_DIR}/build/build.env"
fi

OUTPUT_DIR="${BASE_DIR}/build/app"
OUTPUT_FILE="${OUTPUT_DIR}/version-info.json"
mkdir -p "${OUTPUT_DIR}"

if ! is_gitlab_pipeline; then
  CI_COMMIT_SHORT_SHA="$(git rev-parse --short HEAD 2> /dev/null)"
  CI_COMMIT_TAG="$(git describe --tags --exact-match HEAD 2> /dev/null || echo "")"
fi

echo "Generating version-info.json"

if [ ! -d "${BASE_DIR}/build/app" ]; then
  echo "Missing directory build/app. Run mvn-install.sh first" >&2
  exit 1
fi

BRANCH="$(git rev-parse --abbrev-ref HEAD)"
COMMIT="${CI_COMMIT_SHORT_SHA:-}"
BUILD_DATE="$(date -u '+%Y-%m-%d %H:%M:%S UTC')"

VERSION="${CI_COMMIT_TAG:-}"
# If not a tag pipeline, pick the most recent reachable tag
if [ -z "${VERSION}" ]; then
  VERSION="$(git describe --tags --abbrev=0 HEAD 2> /dev/null || echo "")"
fi

if [ -f "${OUTPUT_FILE}" ]; then
  rm "${OUTPUT_FILE}"
fi

echo
cat > "${OUTPUT_FILE}" <<EOF
{
  "api": "${VERSION}",
  "commit": "${COMMIT}",
  "branch": "${BRANCH}",
  "buildDate": "${BUILD_DATE}"
}
EOF

cat "${OUTPUT_FILE}"

echo
echo "---> version-info.json generated successfully"
echo