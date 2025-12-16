#!/bin/bash

if [[ ${GITLAB_PIPELINE} == false ]];then
  if [[ -z ${IMAGE_VERSION} ]]; then
    IMAGE_VERSION="latest"
  fi
# Only auto-compute Docker image version if it's not set yet
# in the environment. Allow IMAGE_VERSION to be set ad hoc in
# .gitlab-ci.yml or by user as pipeline variable when manually
# starting pipeline
elif [[ -z ${IMAGE_VERSION+x} ]]; then
  if [[ -n ${CI_COMMIT_TAG+x} ]]; then
    IMAGE_VERSION=${CI_COMMIT_TAG}
  elif [[ ${CI_COMMIT_BRANCH} == develop ]]; then
    IMAGE_VERSION=${CI_COMMIT_SHA}
  else
    IMAGE_VERSION=${CI_COMMIT_BRANCH}
  fi
fi

export IMAGE_VERSION
