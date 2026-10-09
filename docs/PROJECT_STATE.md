# PROJECT_STATE — Source of truth for all subsequent sessions

Updated: **2026-10-09 (Europe/Berlin)** | Current milestone: **P0** | Target server day: **2026-10-12**

## Verified complete
- [x] Initialized independent Git-ready project with AGENTS, ADRs, interface contracts and prioritized plans.
- [x] Java 21 dependency-free deterministic rules for team membership, per-player chunk exploration and membership-sensitive union/revocation.
- [x] Negative-coordinate chunk math, radius validation with overflow-safe distance, 3600s carry budget, 600s cumulative mobile border budget.
- [x] Split core/manual chunk quota accounting including shared chunk deduplication.
- [x] Snapshot round-trip and CRC corruption detection with temporary file + atomic replace where supported.
- [x] **41 core assertions passed locally** with Java 21, `scripts/test-core.sh`.
- [x] Existing diagnostic ZIP inspected for modpack versions and FTB settings; no third-party JAR copied.

## Scaffold, NOT verified
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
