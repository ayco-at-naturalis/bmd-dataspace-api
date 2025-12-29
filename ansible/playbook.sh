#!/bin/bash

docker run \
    -v "${BASE_DIR}/:/runner/" \
    -v "${BASE_DIR}/ansible/ssh/:/root/.ssh" \
    -w /runner \
    -e ANSIBLE_CONFIG=/runner/ansible/ansible.cfg \
    -e IMAGE_VERSION \
    -e CI_REGISTRY \
    -e CI_REGISTRY_USER \
    -e CI_REGISTRY_PASSWORD \
    -t \
    registry.gitlab.com/naturalis/lib/ansible/docker-ansible:latest ansible-playbook \
    -i "ansible/inventory/hosts.yml" \
    -l "${DEPLOY_HOST}" \
    ansible/deploy.yml
