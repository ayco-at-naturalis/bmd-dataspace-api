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

if [[ ! -d "${BASE_DIR}/docker/mvn-repo" ]]; then
  echo '---> Creating local Maven repository'
  mkdir "${BASE_DIR}/docker/mvn-repo"
fi

echo '---> Creating Java classpath directory'
rm -rf "${BASE_DIR}/docker/dependencies"
mkdir "${BASE_DIR}/docker/dependencies"

echo '---> Starting build'
if mvn clean install --settings "${BASE_DIR}/docker/maven-settings.xml" "--file=${BASE_DIR}/pom.xml";
then
  echo '---> Maven build completed successfully'
  exit 0
fi

echo '---> Maven build failed!'
exit 1
