$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
New-Item -ItemType Directory -Force build/classes | Out-Null
$files = @(Get-ChildItem -Recurse core/src -Filter '*.java' | ForEach-Object { $_.FullName })
& javac --release 21 -encoding UTF-8 -d build/classes $files
if ($LASTEXITCODE -ne 0) { throw "javac failed: $LASTEXITCODE" }
& java -ea -cp build/classes dev.rogler.teamwars.core.CoreTests
if ($LASTEXITCODE -ne 0) { throw "tests failed: $LASTEXITCODE" }

& java -ea -cp build/classes dev.rogler.teamwars.core.teams.TeamAssignmentTests
if ($LASTEXITCODE -ne 0) { throw "team policy tests failed: $LASTEXITCODE" }

& java -ea -cp build/classes dev.rogler.teamwars.core.base.BaseCoreLedgerTests
if ($LASTEXITCODE -ne 0) { throw "base core ledger tests failed: $LASTEXITCODE" }

& java -ea -cp build/classes dev.rogler.teamwars.core.base.BaseProtectionPolicyTests
if ($LASTEXITCODE -ne 0) { throw "protection policy tests failed: $LASTEXITCODE" }

& java -ea -cp build/classes dev.rogler.teamwars.core.base.RaidPolicyTests
if ($LASTEXITCODE -ne 0) { throw "raid policy tests failed: $LASTEXITCODE" }
& java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreUpgradePolicyTests
if ($LASTEXITCODE -ne 0) { throw "core upgrade policy tests failed: $LASTEXITCODE" }
& java -ea -cp build/classes dev.rogler.teamwars.core.base.CorePowerPolicyTests
if ($LASTEXITCODE -ne 0) { throw "core energy tests failed: $LASTEXITCODE" }

& java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreKineticPolicyTests
if ($LASTEXITCODE -ne 0) { throw "Create kinetic policy tests failed: $LASTEXITCODE" }
& java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreDashboardPolicyTests
if ($LASTEXITCODE -ne 0) { throw "dashboard policy tests failed: $LASTEXITCODE" }
& java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreManualTradePolicyTests
if ($LASTEXITCODE -ne 0) { throw "manual book trade tests failed: $LASTEXITCODE" }

& java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreIntegrityPolicyTests
if ($LASTEXITCODE -ne 0) { throw "integrity policy tests failed: $LASTEXITCODE" }

& java -ea -cp build/classes dev.rogler.teamwars.core.compat.WariumRecoveryPolicyTests
if ($LASTEXITCODE -ne 0) { throw "Warium recovery policy tests failed: $LASTEXITCODE" }

$settingsFiles = @(
    'mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/teams/TeamWarsSettings.java',
    'mod-neoforge/src/test/java/dev/rogler/teamwars/neoforge/teams/TeamWarsSettingsTests.java'
)
& javac --release 21 -encoding UTF-8 -cp build/classes -d build/classes $settingsFiles
if ($LASTEXITCODE -ne 0) { throw "settings javac failed: $LASTEXITCODE" }
& java -ea -cp build/classes dev.rogler.teamwars.neoforge.teams.TeamWarsSettingsTests
if ($LASTEXITCODE -ne 0) { throw "settings tests failed: $LASTEXITCODE" }

& java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreExplosionIndexTests
if ($LASTEXITCODE -ne 0) { throw "explosion index tests failed: $LASTEXITCODE" }
& java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreRecoveryPolicyTests
if ($LASTEXITCODE -ne 0) { throw "recovery policy tests failed: $LASTEXITCODE" }
& java -ea -cp build/classes dev.rogler.teamwars.core.base.CoreClaimCleanupPolicyTests
if ($LASTEXITCODE -ne 0) { throw "claim receipt tests failed: $LASTEXITCODE" }

& java -ea -cp build/classes dev.rogler.teamwars.core.compat.SableCompatibilityPolicyTests
if ($LASTEXITCODE -ne 0) { throw "SableCompatibilityPolicyTests failed: $LASTEXITCODE" }

& java -ea -cp build/classes dev.rogler.teamwars.core.compat.SableSplitConfigAuditTests
if ($LASTEXITCODE -ne 0) { throw "SableSplitConfigAuditTests failed: $LASTEXITCODE" }

& java -ea -cp build/classes dev.rogler.teamwars.core.compat.ExplosionObservationCounterTests
if ($LASTEXITCODE -ne 0) { throw "ExplosionObservationCounterTests failed: $LASTEXITCODE" }

& java -ea -cp build/classes dev.rogler.teamwars.core.compat.SableLifecyclePolicyTests
if ($LASTEXITCODE -ne 0) { throw "SableLifecyclePolicyTests failed: $LASTEXITCODE" }
