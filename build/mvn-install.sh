#!/usr/bin/env bash

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

. "${BASE_DIR}/build/functions.sh"

echo 'Executing Maven build'

if [[ ! -d "${BASE_DIR}/build/mvn-repo" ]]; then
  echo '---> Creating local Maven repository'
  mkdir "${BASE_DIR}/build/mvn-repo"
fi

echo '---> Creating Java classpath directories'
rm -rf "${BASE_DIR}/build/dependencies"
rm -rf "${BASE_DIR}/build/app"
mkdir "${BASE_DIR}/build/dependencies"
mkdir "${BASE_DIR}/build/app"

echo '---> Starting build'
run_maven clean install

echo
