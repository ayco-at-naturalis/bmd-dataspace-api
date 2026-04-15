#!/bin/sh

set -eu

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

if ! is_gitlab_pipeline; then
  if [ ! -f "${BASE_DIR}/build/build.env" ]; then
    echo 'o ---> Local builds require presence of build.env' >&2
    echo 'o ---> Copy build.env.template to build.env and modify as appropriate' >&2
    exit 1
  fi
  . "${BASE_DIR}/build/build.env"
fi

. "${BASE_DIR}/build/functions.sh"

echo "o ---> Publishing Javadocs"

rm -rf "${BASE_DIR}/public"
mkdir "${BASE_DIR}/public"

run_maven 'javadoc:javadoc'

cp -r "${BASE_DIR}/target/site/apidocs/"* "${BASE_DIR}/public/v1/javadocs"

echo
echo 'o ---> Javadocs published successfully. See https://naturalis.gitlab.io/bii/bmd/bmd-dataspace-api/v1/javadocs'
echo