#!/usr/bin/env bash

# Utility script for doing local builds.

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

"${BASE_DIR}/docker/mvn-install.sh"
"${BASE_DIR}/docker/create-image.sh"
"${BASE_DIR}/docker/tag-image.sh"
