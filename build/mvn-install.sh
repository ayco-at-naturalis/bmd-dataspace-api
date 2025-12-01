#!/usr/bin/env bash

set -euo pipefail

# Are we running inside a GitLab pipeline? Then we have CI_JOB_ID in our env.
GITLAB_PIPELINE="${CI_JOB_ID:-}"

if [[ "${GITLAB_PIPELINE}" ]]; then
  BASE_DIR="${CI_PROJECT_DIR:-}"
else
  BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fi

echo 'Executing Maven build'

if [[ ! -d "${BASE_DIR}/build/mvn-repo" ]]; then
  echo '---> Creating local Maven repository'
  mkdir "${BASE_DIR}/build/mvn-repo"
fi

echo '---> Creating Java classpath directory'
rm -rf "${BASE_DIR}/build/dependencies"
mkdir "${BASE_DIR}/build/dependencies"

echo '---> Starting build'
MAVEN_OPTS="-Dmaven.repo.local=\"${BASE_DIR}/build/mvn-repo\" --enable-native-access=ALL-UNNAMED"
MAVEN_CLI_OPTS="-B -V --no-transfer-progress --file=\"${BASE_DIR}/pom.xml\""
"${BASE_DIR}/mvnw" ${MAVEN_CLI_OPTS} clean install

echo
