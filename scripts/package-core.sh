#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
./scripts/test-core.sh
mkdir -p dist
printf 'Main-Class: dev.rogler.teamwars.core.Main\n' > build/manifest.mf
jar cfm dist/rogler-teamwars-core-P0.jar build/manifest.mf -C build/classes dev/rogler/teamwars/core
java -jar dist/rogler-teamwars-core-P0.jar
