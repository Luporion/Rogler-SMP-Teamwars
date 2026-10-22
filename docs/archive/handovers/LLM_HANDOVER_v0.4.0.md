# LLM / Agent Handover — TeamWars v0.4.0

Repository: Luporion/Rogler-SMP-Teamwars. Base branch `feat/v0.3.1-stability`, base SHA `a84e92a6c680dbda7215777db964c5ef834c46f4`. This v0.4.0 zip is complete tracked source snapshot, NOT a tested binary.

Read in order: `AGENTS.md`, `docs/PROJECT_STATE.md`, `docs/releases/v0.4.0.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/TEST_MATRIX.md`, `docs/MOD_REQUIREMENTS.md`, this file.

Key modules:
- `core/.../base/RaidPolicy.java`: pure java 10 HP, 750ms, first-attacker party locked, defensive/timeout/Grace predicates. No Minecraft classes.
- `mod-neoforge/.../raid/RaidSavedData.java`: saved raiding + team scores in `roglerteamwars_raids.dat`.
- `mod-neoforge/.../raid/RaidService.java`: OP window and core-only hit path, registered through `BaseGuardService.onLeftClick` and `RoglerTeamWarsMod`.
- `mod-neoforge/.../protection/FtbClaimBridge.java`: FTB property PRIVATE via reflection; fail closed. `BaseGuardService` auto/reconciliation and 3x3 guard.
- `core/.../base/BaseCoreLedger.java`: authoritative core state transition `defeatPlaced` to ESCROW; old token revoked by recovery.

CAUTIONS: No old FTB team UUID rebinding by name. Never permit general break permissions during raids. World backups and 2-player privacy check are mandatory. Claim visibility could leak during team GUI property edits between automatic reconciles. If reflecting FTB errors, keep native guard. Do not treat offline Java assertions as NeoForge compiler proof.

Workflow: deliver (a) full SOURCE ZIP with ALL tracked files inc. AGENTS/docs/LLM/workflows and (b) .patch; verify patch from exact baseline with `git apply --check` then `git am --no-3way`; use `gc.auto=0` and `maintenance.auto=false` on user's Windows clone with recurring Git object locks; run VERIFY.cmd; push branch; watch-actions monitor; check successful real GitHub Actions before installing JAR. No third-party JARs redistributed.

Remaining: actual NeoForge CI, multiplayer Raid/FTB visibility tests, improved anti-grief modded world edit paths, auto raid policy / faction scheduling, force loaded chunk quotas, Xaero shared-map system.

The v0.4.0 update also amends `scripts/apply-update.ps1` to export an exact, complete source ZIP using `git archive HEAD` in the real Windows repo, beside the downloaded patch.
