#!/usr/bin/env bash
# Run the same Checkstyle check as .github/workflows/format-check.yml locally.
set -euo pipefail

CHECKSTYLE_VERSION="13.6.0"
CHECKSTYLE_CONFIG="google_checks.xml"
DOCKER_IMAGE="eclipse-temurin:25-jre-alpine"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v docker >/dev/null 2>&1; then
  echo "error: docker is required to run Checkstyle locally" >&2
  exit 1
fi

docker run --rm \
  -v "${repo_root}:/workspace" \
  -w /workspace \
  "${DOCKER_IMAGE}" \
  sh -c "
    set -euo pipefail
    apk add --no-cache wget >/dev/null
    wget -q -O /tmp/checkstyle.jar \
      https://github.com/checkstyle/checkstyle/releases/download/checkstyle-${CHECKSTYLE_VERSION}/checkstyle-${CHECKSTYLE_VERSION}-all.jar
    java -jar /tmp/checkstyle.jar src/main/java -c ${CHECKSTYLE_CONFIG} -e /workspace/src/test
  "
