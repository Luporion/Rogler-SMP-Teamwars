#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
mkdir -p build/classes
find core/src/main/java core/src/test/java -name '*.java' -print > build/sources.txt
javac --release 21 -encoding UTF-8 -d build/classes @build/sources.txt
java -ea -cp build/classes dev.rogler.teamwars.core.CoreTests
java -ea -cp build/classes dev.rogler.teamwars.core.teams.TeamAssignmentTests
java -ea -cp build/classes dev.rogler.teamwars.core.base.BaseCoreLedgerTests
java -ea -cp build/classes dev.rogler.teamwars.core.base.BaseProtectionPolicyTests
java -ea -cp build/classes dev.rogler.teamwars.core.base.RaidPolicyTests
java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreUpgradePolicyTests
java -ea -cp build/classes dev.rogler.teamwars.core.base.CorePowerPolicyTests
java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreKineticPolicyTests
java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreDashboardPolicyTests
java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreManualTradePolicyTests
java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreIntegrityPolicyTests
java -ea -cp build/classes dev.rogler.teamwars.core.compat.WariumRecoveryPolicyTests

# Settings do not import Minecraft; verify their file/validation behaviour offline.
javac --release 21 -encoding UTF-8 -cp build/classes -d build/classes \
  mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/teams/TeamWarsSettings.java \
  mod-neoforge/src/test/java/dev/rogler/teamwars/neoforge/teams/TeamWarsSettingsTests.java
java -ea -cp build/classes dev.rogler.teamwars.neoforge.teams.TeamWarsSettingsTests

java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreExplosionIndexTests
java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreRecoveryPolicyTests
java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreClaimCleanupPolicyTests

java -ea -cp build/classes dev.rogler.teamwars.core.compat.SableCompatibilityPolicyTests

java -ea -cp build/classes dev.rogler.teamwars.core.compat.SableSplitConfigAuditTests

java -ea -cp build/classes dev.rogler.teamwars.core.compat.ExplosionObservationCounterTests

java -ea -cp build/classes dev.rogler.teamwars.core.compat.SableLifecyclePolicyTests
