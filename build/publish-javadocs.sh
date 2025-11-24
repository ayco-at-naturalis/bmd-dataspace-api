#!/usr/bin/env bash

set -euo pipefail

# Are we running inside a GitLab pipeline? Then we have CI_JOB_ID in our env.
GITLAB_PIPELINE="${CI_JOB_ID:-}"

if [[ "${GITLAB_PIPELINE}" ]]; then
  BASE_DIR="${CI_PROJECT_DIR:-}"
  # MAVEN_OPTS and MAVEN_CLI_OPTS defined via .gitlab-ci.yml
else
  BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
  MAVEN_OPTS="-Dmaven.repo.local=\"${BASE_DIR}/build/mvn-repo\" --enable-native-access=ALL-UNNAMED"
  MAVEN_CLI_OPTS="-B -V --no-transfer-progress --file=\"${BASE_DIR}/pom.xml\""
fi

echo "Publishing Javadocs"

rm -rf "${BASE_DIR}/public"
mkdir "${BASE_DIR}/public"

"${BASE_DIR}/mvnw" ${MAVEN_CLI_OPTS} javadoc:javadoc

cp -r "${BASE_DIR}/target/site/apidocs/"* "${BASE_DIR}/public/"

echo 'Javadocs published successfully! See https://naturalis.gitlab.io/bii/bmd/bmd-dataspace-api/'
