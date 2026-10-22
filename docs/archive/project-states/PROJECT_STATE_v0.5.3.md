# Rogler TeamWars v0.5.3 – Project state / truth ledger

Base: `feat/v0.5.2-update` at SHA `367ccdc2` (hotfixed; 229 Java policy checks, green NeoForge CI, user has tested briefly). This separate v0.5.3 status file deliberately leaves the historical upstream `docs/PROJECT_STATE.md` intact because old offline snapshots differ from that GitHub blob.

Current v0.5.3 SOURCE CANDIDATE: server-synced, team-bound 5-tab dashboard via right-click / `/teamwars base menu` / `/teamwars guide`, quiet menu upgrade purchases, 9-slot inventory, existing FE/kinetic features, FE consumer/AE2 adjacency readout and detected real manuals. Client sends only action IDs; all purchases remain authoritative in CoreUpgradeService.

Verified: 252 local dependency-free Java assertions (229 existing + 23 dashboard policy); Git-format patch applies to an isolated v0.5.2 baseline and repeats tests successfully.

Not verified: NeoForge v0.5.3 compile, actual client menu rendering, AE2 runtime energy receipt, 2-client security, save/reload UI regression. No fabricated JAR. Dedicated-server / Create compatibility must pass real GitHub Actions and a disposable Minecraft world.

Deferred: direct AE2 ME network node, ME drive/terminal, Sophisticated upgrades, separate FTB force-load tickets, Xaero map revocation, team voice, events and market. The FUTURE screen is a placeholder only.

Required: MC 1.21.1, NeoForge 21.1.249, Create 6.0.10, FTB Teams/Chunks/Library as in prior release. No additional mods. Preserve all v0.5.2 SavedData.

Next: apply v0.5.3 patch on actual commit `367ccdc2`, inspect NeoForge CI; after green build test GUI, inventory, material costs, FE side indicators, Create and no ownership bypass. See `docs/releases/v0.5.3.md`, `docs/CORE_DASHBOARD_ARCHITECTURE.md`, `docs/archive/test-plans/TEST_PLAN_v0.5.3.md`.
