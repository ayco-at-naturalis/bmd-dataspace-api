#!/bin/sh

set -eu

# Are we running inside a GitLab pipeline? Then we have CI_JOB_ID in our env.
if [ -n "${CI_JOB_ID-}" ]; then
  GITLAB_PIPELINE=1
else
  GITLAB_PIPELINE=
fi

if [ -n "${GITLAB_PIPELINE}" ]; then
  BASE_DIR="${CI_PROJECT_DIR}"
else
  # Resolve BASE_DIR from this script's location
  BASE_DIR=$(cd "$(dirname "$0")/.." && pwd)
  if [ ! -f "${BASE_DIR}/build/build.env" ]; then
    echo 'Local builds require presence of build.env'
    echo 'Copy build.env.template to build.env and modify as appropriate'
    exit 1
  fi
  . "${BASE_DIR}/build/build.env"
fi

. "${BASE_DIR}/build/functions.sh"

echo "Publishing Javadocs"

rm -rf "${BASE_DIR}/public"
mkdir "${BASE_DIR}/public"

run_maven 'javadoc:javadoc'

cp -r "${BASE_DIR}/target/site/apidocs/"* "${BASE_DIR}/public/"

echo
echo '--> Javadocs published successfully. See https://naturalis.gitlab.io/bii/bmd/bmd-dataspace-api/'
echo