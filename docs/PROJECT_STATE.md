# PROJECT_STATE — Source of truth for all subsequent sessions

## Developer tooling: GitHub Actions Monitor

Added `scripts/watch-actions.ps1` (append-only workflow status polling and safe artifact download). Requires locally installed GitHub CLI. Does not change the NeoForge mod, CI or runtime verification status; Windows end-to-end use is pending. See `docs/plans/2026-10-10-actions-build-monitor.md` and `docs/HANDOVER_BUILD_MONITOR.md`.

Updated: **2026-10-10 Europe/Berlin** | Current milestone: **v0.2.0 Base Core SOURCE CANDIDATE** | Server target: 2026-10-12

## LATEST (READ FIRST)
- Branch `feat/v0.2.0-base-core` extends v0.1.0 FTB/nametag base plus Gradle 8.14 fix. The v0.1.0 JAR was already built in GitHub Actions and user reported first runtime test 'sah gut aus', but 2-player permission testing remains open.
- Added Base Core block/item, admin issuance + escrow recovery, Overworld radius (300–2000), team-signed item with canonical token, overworld SavedData and per-second carried time with safe-placement attempt.
- **89 standalone Java checks pass**: 41 P0 + 15 team + 8 settings + 25 new BaseCoreLedger. NeoForge v0.2.0 compiler and real client/server are **NOT YET TESTED**.
- **SECURITY:** regular `/teamwars base status` only shows own team; `/teamwars admin corelist` requires OP2. FTB team creation is authoritative; spoofed items rejected by ledger.
- **NO FTB CLAIM PROTECTION / NO SEPARATE CORE CHUNKLOADING yet.** Do not expose this version as raid-ready.
- New mod dependencies: NONE. Client diagnosis 2026-10-10 confirms FTB Teams 2101.1.11 and FTB Library 2101.1.35, plus FTB Chunks and optional tech mods. Replace TeamWars 0.1.0 JAR with v0.2.0 when actually built.
- See `docs/MOD_REQUIREMENTS.md`, `docs/plans/2026-10-10-v0.2.0-base-core.md`, `docs/releases/v0.2.0.md`.

## Remaining before Monday
1. Run NeoForge v0.2.0 CI build, fix ALL compiler/API errors.
2. Disposable world: admin issue, placement rings, invalid item dupe, restart, timeout to auto place / escrow, own-only diagnostics.
3. FTB claim and grace enforcement branch, or manually manage base protection/raids until audited.
4. Test two clients and all required mods on dedicated-server profile.

---

# PROJECT_STATE — Source of truth for all subsequent sessions

Updated: **2026-10-10 (Europe/Berlin)** | Current milestone: **v0.1.0 source candidate (NOT a tested JAR)** | Target server day: **2026-10-12**

## START HERE (latest)
- Branch `feat/v0.1.0-ftb-teams`, based on P0 `7041003`.
- 64 Java checks succeeded: P0 core 41, FTB team assignment policy 15, settings 8.
- Authored NeoForge FTB reader + nametag scoreboard projection + commands + GitHub compile workflow.
- **Minecraft Gradle compile, launcher startup, 2-player gameplay and full-pack compatibility remain UNVERIFIED**.
- Immediate next step: push the feature branch, collect real CI result, fix actual compile errors, test in staging. Do not tag release as stable yet.

---
## v0.2.1 candidate — 2026-10-10 (LATEST FOLLOW-UP)
- Based on v0.2.0 source; updated 0–5000 radius defaults with narrowly scoped migration of unmodified old v0.2.0 generated config (backup).
- Dropped item hazard rescue: real-player BlockItem placement in valid nearby location, ESCROW fallback, revocation of old tokens. Self-service `/teamwars base recover`, OP old-world repair `/teamwars admin reclaimlost <player>`.
- Precise placement warnings and `OUTSIDE ALLOWED BASE ZONE`. Core ledger tests increased from 25 to 42; total **106 standalone Java assertions PASS**.
- **UNVERIFIED:** real NeoForge compile, 2-player acceptance, lava world test. FTB claims/grace/chunkloading still NOT IMPLEMENTED. No new third-party mods.
- Source of truth: `docs/releases/v0.2.1.md`, `docs/plans/2026-10-10-v0.2.1-rescue.md`, existing `scripts/watch-actions.ps1` from live GitHub branch.
- Next: apply patch on `feat/v0.2.0-base-core`, push, run `scripts/watch-actions.ps1`, smoke test, then FTB protection.

## Historical P0 — Verified complete
- [x] Initialized independent Git-ready project with AGENTS, ADRs, interface contracts and prioritized plans.
- [x] Java 21 dependency-free deterministic rules for team membership, per-player chunk exploration and membership-sensitive union/revocation.
- [x] Negative-coordinate chunk math, radius validation with overflow-safe distance, 3600s carry budget, 600s cumulative mobile border budget.
- [x] Split core/manual chunk quota accounting including shared chunk deduplication.
- [x] Snapshot round-trip and CRC corruption detection with temporary file + atomic replace where supported.
- [x] **41 core assertions passed locally** with Java 21, `scripts/test-core.sh`.
- [x] Existing diagnostic ZIP inspected for modpack versions and FTB settings; no third-party JAR copied.

## Historical P0 — Scaffold, NOT verified
- [ ] `mod-neoforge` Gradle build against 21.1.249 (no network/Gradle wrapper here).
- [ ] NeoForge jar launches in real test profile and `/teamwars status` works.

## Not implemented (NO implication of partial readiness)
- [ ] FTB Teams binding, team create/join/leave events, role permissions.
- [ ] Xaero map tile read/write/render; separate personal/team render layer; native minimap and world map revocation.
- [ ] Player exploration sampling via server events and region rendering data transfer.
- [ ] Base Core block registration, inventory, claim protection, respawn, raids, scoreboard, admin commands, world UI.
- [ ] FTB separate force-load tickets, Chunky Friends integration.
- [ ] Sophisticated upgrade integration, FE/Create/AE2.
- [ ] Trading/rename fee transactions.
- [ ] Sable world-space movement, nuke explosion rendering and harm rules.

## BLOCKING decisions / risks
- **B1 (critical):** Xaero's World Map is ARR/client-cache-based; exactly one shared-and-revocable map might need a client mixin/overlay. Algorithm works, renderer feasibility **UNKNOWN**. See `docs/research/XAERO_SPIKE.md`.
- **B2 (critical):** NeoForge build and runtime not tested; get official wrapper, build against 21.1.249, then target test profile.
- **B3:** Vanilla server chunks do not contain rendered Xaero map tiles; sharing geometry vs sharing explored-mask is separate engineering. Server cannot simply send boolean coverage and assume client has terrain data.
- **B4:** No permissions/claims integration yet. Don't expose Raids to production without exploit tests.
- **B5:** Existing modpack has high-load chunk generation and >500 mods. Keep external features off until TPS/MSPT and backups are measured.

## NEXT single highest-priority task
**Build `mod-neoforge` and get first launch on a disposable profile** (P0-S1). Parallel: separate Xaero render feasibility spike (P0-S2). Do not merge feature branches until CI and runtime evidence recorded.

## Provenance
Diagnostic: `Rogler Alpha DEV_Session-Analyse_2026-10-09_03-38-17(1).zip`, Minecraft 1.21.1 NeoForge 21.1.249.

## Handover format for future changes
```
Milestone / branch:
Commit hash:
Changed files:
Acceptance results: (commands + actual logs)
Unverified assumptions:
Risks / rollback:
Next task:
```

---
## v0.1.0 source branch update — 2026-10-10

**Branch:** `feat/v0.1.0-ftb-teams` | **Delivery status: SOURCE CANDIDATE, NOT YET BUILT INTO A VERIFIED MOD JAR**.

### Verified
- [x] Java 21 deterministic TeamAssignmentPolicy (FTB party canonical UUID + team membership, out-of-party/oversized solo fail-closed, nametag keys). 15 new team assertions + 8 settings checks.
- [x] All P0 41 assertions still pass (total 64 offline checks).
- [x] Read-only FTB adapter, scoreboard manager, command routes, safe config and build workflow authored.

### NOT verified / blocking
- [ ] NeoForge compilation against 21.1.249 + FTB Teams 2101.1.11. Offline environment cannot fetch dependencies.
- [ ] FTB reflection invokes expected public methods at runtime.
- [ ] 2-client nametag visibility, join/leave, login, restart, larger modpack HUD compatibility.
- [ ] Native 4-party creation prevention, paid rename logic, Voice groups, Base Core, Xaero terrain sharing.

**Current NEXT task:** Push this branch, run `.github/workflows/neoforge-build.yml`, inspect the actual build log and fix compile errors; staging world smoke test before any release. See `docs/releases/v0.1.0.md`.
