#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
python3 scripts/check-mixin-boundaries.py
python3 scripts/check-ftb-xaero-compat.py
mkdir -p build/classes
find core/src/main/java core/src/test/java -name '*.java' -print > build/sources.txt
javac --release 21 -encoding UTF-8 -d build/classes @build/sources.txt
java -ea -cp build/classes dev.rogler.teamwars.core.CoreTests
java -ea -cp build/classes dev.rogler.teamwars.core.teams.TeamAssignmentTests
java -ea -cp build/classes dev.rogler.teamwars.core.teams.TeamManagementPolicyTests
java -ea -cp build/classes dev.rogler.teamwars.core.base.BaseCoreLedgerTests
java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreTransportClockPolicyTests
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

java -ea -cp build/classes dev.rogler.teamwars.core.TeamMapPrivacyTests

java -ea -cp build/classes dev.rogler.teamwars.core.TeamMapSessionTests
java -ea -cp build/classes dev.rogler.teamwars.core.TeamMapIncrementalTests
java -ea -cp build/classes dev.rogler.teamwars.core.TeamIntelPolicyTests

java -ea -cp build/classes dev.rogler.teamwars.core.FtbMapVisibilityPolicyTests

java -ea -cp build/classes dev.rogler.teamwars.core.HudPreferencesTests
java -ea -cp build/classes dev.rogler.teamwars.core.HudPositionPolicyTests

python3 scripts/check-waypoint-bridge.py
java -ea -cp build/classes dev.rogler.teamwars.core.WaypointSyncPolicyTests

java -ea -cp build/classes dev.rogler.teamwars.core.WaypointReconcilePolicyTests

java -ea -cp build/classes dev.rogler.teamwars.core.WaypointLinkPolicyTests
java -ea -cp build/classes dev.rogler.teamwars.core.WaypointLinkJournalTests
python3 scripts/check-waypoint-link.py

java -ea -cp build/classes dev.rogler.teamwars.core.ui.AdminActionPolicyTests
java -ea -cp build/classes dev.rogler.teamwars.core.ui.ControlCenterPolicyTests
python3 scripts/check-control-center.py
python3 scripts/check-v084-control-center.py
python3 scripts/check-v085-ui.py
python3 scripts/check-v086-core-timer.py
python3 scripts/check-v087-performance.py
python3 scripts/test-v085-hud-screen.py

python3 scripts/check-v083-safety.py
python3 scripts/check-mod-version.py
python3 scripts/test-core-kinetic-save.py

# v0.8.8: bounded diagnostic authority and real optional FTB reflection reader.
java -ea -cp build/classes dev.rogler.teamwars.core.ui.DiagnosticPolicyTests
javac --release 21 -encoding UTF-8 -cp build/classes -d build/classes \
  mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/protection/FtbChunkStatusReader.java \
  mod-neoforge/src/test/java/dev/rogler/teamwars/neoforge/protection/FtbChunkStatusReaderTests.java
java -ea -cp build/classes dev.rogler.teamwars.neoforge.protection.FtbChunkStatusReaderTests
python3 scripts/check-v088-diagnostics.py
python3 scripts/test-v088-reports.py
