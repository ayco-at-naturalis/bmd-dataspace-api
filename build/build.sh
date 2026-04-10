#!/bin/bash

# Utility script for doing local builds.

set -e

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

"${BASE_DIR}/build/mvn-install.sh"
"${BASE_DIR}/build/generate-version-info.sh"
"${BASE_DIR}/build/create-image.sh"
"${BASE_DIR}/build/tag-image.sh"
