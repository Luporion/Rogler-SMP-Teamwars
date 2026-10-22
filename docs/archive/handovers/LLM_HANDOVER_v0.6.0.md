# LLM handover — v0.6.0 (read first)

Base: `feat/v0.5.4-update`, commit `b447cd43`.
Read: `AGENTS.md`, `docs/releases/v0.6.0.md`, `docs/archive/test-plans/TEST_PLAN_v0.6.0.md`.
Goal: modded nuclear bypass-safe core recovery + visible Create through-shaft. No Xaero maps implemented (render-cache revocation still unsolved). No additional required mods.

Core model: `assets/roglerteamwars/models/block/base_core.json` is X-axis through-bore; Z rotates it with blockstate. Create shaft renderer spins the real partial. Only CI and game can confirm actual model result.

Core integrity: `CoreIntegrityPolicy` has isolated pure decision rules; `BaseGuardService.verifyPhysicalCores` adapts to server/world, runs every 20 ticks and on admin audit. A PLACED ledger entry gets restored ONLY into air at saved location in LOADED Overworld chunk; blocked positions ESCROW. `BaseProtectionSavedData.Record.coreAxis` optional saves orientation (older saves default X). `FtbClaimBridge.release` validates provenance and owner before unclaiming. No recreation of items, no general event nuking. Reconcile won't recreate claims in a loaded location lacking its core.

Tests: Java offline suite includes 9 new integrity checks. Status of NeoForge build/in-game nuke test: UNVERIFIED until user supplies CI logs and actual tests. `PATCH.cmd` should be used with Git-format patch from exact base. Source ZIP produced via user `git archive` after applying patch is authoritative for originals.
