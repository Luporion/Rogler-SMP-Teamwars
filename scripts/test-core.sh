#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
mkdir -p build/classes
find core/src/main/java core/src/test/java -name '*.java' -print > build/sources.txt
javac --release 21 -encoding UTF-8 -d build/classes @build/sources.txt
java -ea -cp build/classes dev.rogler.teamwars.core.CoreTests
