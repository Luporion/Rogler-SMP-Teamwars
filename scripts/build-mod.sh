#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../mod-neoforge"
if [[ -x ./gradlew ]]; then
  ./gradlew build --stacktrace --no-daemon
elif command -v gradle >/dev/null 2>&1; then
  gradle build --stacktrace --no-daemon
else
  echo 'Gradle missing. Install Gradle 8.10.2 or build through GitHub Actions.' >&2
  exit 2
fi
