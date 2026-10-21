# Current milestone: v0.7.6 — Explicit shared pins + optional two-way private import SOURCE CANDIDATE (2026-10-21)

- Based on v0.7.5. Added bounded team-scoped server SavedData and play payloads, opt-in per-waypoint team share/revoke, privacy-filtered transient Xaero/FTB markers, and **manual create-only** FTB↔Xaero private import.
- No automatic rename/edit/delete synchronization or native Xaero map terrain revocation. Keep privacy model and v0.7.4 FTB Architectury fix. New protocol `3`: all clients and server require the exact same 0.7.6 version.
- Local Java tests and source guards passed; NeoForge Actions and multiplayer/runtime acceptance **NOT YET VERIFIED**. See `docs/releases/v0.7.6.md` and `docs/plans/2026-10-21-v0.7.6-waypoints.md`.

---

# Current milestone: v0.7.5 optional client HUD + own-base/respawn + Xaero team markers SOURCE CANDIDATE (2026-10-20)

- Base: v0.7.4 (CI green in user logs; gameplay join confirmed). New v0.7.5 CI/real multiplayer **PENDING**.
- New protocol version `2`: add private `TeamNavigationPayload` with own respawn + team-owned base core. Existing `TeamIntelPayload` now includes Y for Xaero teammate waypoints. Client HUD hidden by default. `/roglerhud` opens standalone client settings and changes corners/offsets and individual distances.
- Xaero Minimap 26.4.2 mod-owned custom waypoint map for teammates and home core; ephemeral, filtered, reconnect cleanup. Disable FTB F marker mirror by default to address clutter. No terrain sync or bidirectional private/team waypoint sharing yet.
- Existing FTB privacy Mixin + Architectury sender from v0.7.4 retained. FTB long-range tracker must still be explicitly disabled for strict team secrecy.
- Java tests and static shape checks pass offline; **do not claim compile or world-map rendering before CI and 2-client run**.
- See `README_Rogler_Core_v0.7.5.md` and `docs/releases/v0.7.5.md`.

---

# Current milestone: v0.7.2 — Team Intel HUD SOURCE CANDIDATE (2026-10-19)

New server-authoritative teammate-only presence packets and volatile client HUD. Pure Java tests added. Previous v0.7.1 CI success: commit `e2fddd57`; no 0.7.2 CI/runtime evidence yet. Xaero terrain import/revocation, native minimap icons and FTB<->Xaero waypoints still NOT implemented. See `docs/releases/v0.7.2.md`. No SavedData schema migration.

# LATEST — v0.3.0 SOURCE CANDIDATE (2026-10-12)

Branch base: `feat/v0.2.2-core-relocation`, commit `545eca8` upstream. Do NOT assume runtime is verified. See `docs/releases/v0.3.0.md`.

- NeoForge 21.1.249, FTB Teams 2101.1.11, Library 2101.1.37, Chunks 2101.1.22 confirmed in user session mods.csv (client, not yet dedicated-server inventory).
- Fixed Creative item re-use and recovery-time reset. Configurable 3×3 CoreGuard + persisted 24h first-base Grace + FTB claim bridge + safe cleanup/diagnostic commands.
- **Offline Java policy tests passing**; NeoForge Gradle CI build and 2-client runtime tests still PENDING. Destructive raiding, separate FTB chunkloader quota, Xaero exploration, voice, Sable and scoring NOT implemented.
- New scripts: `PATCH.cmd` automatically calls `VERIFY.cmd`, optionally pushes and calls existing `scripts/watch-actions.ps1` from upstream repo. Downloads can be redirected to `M:\Downloads`.
- Next: Push patch, inspect CI; run full 2-client safeguards test BEFORE production. Keep `raids_enabled=false`.

---

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

---
## LATEST — 2026-10-11, v0.2.2 CORE RELOCATION SOURCE CANDIDATE
- Aktueller Entwicklungsstand: v0.2.1 wurde in Minecraft erfolgreich platziert (Screenshot, Session); v0.2.1 NeoForge-CI grün. Laden/Abbauen fehlte absichtlich (Blockhärte -1).
- v0.2.2 implementiert autorisierten Team-Bergungsvorgang durch Shift+Rechtsklick oder `/teamwars base pack`, Tokenrotation, bewahrte Transportzeit, Fallback bei vollem Inventar, fehlender-Block-Admin-Recovery und Logging.
- Java-Tests des Core-Regelmoduls erfolgreich; **NeoForge-CI- und Multiplayer-Laufzeit-Ergebnis v0.2.2 steht noch aus**.
- **NICHT IMPLEMENTIERT:** FTB Claim/Grace/Force-Load, Raid/Kriegspunkte, Xaero-Mapsharing, Simple Voice Chat Teamkanal, Sophisticated Core, Sable, Handel, Events, UI.
- Keine neuen Drittanbieter-Mods erforderlich; `docs/MOD_REQUIREMENTS.md` lesen.
- Nächste Schritte: CI Compile, 2-Team-Rechtsklick-Tests, Weltbackup/Restart/Lava, danach Claims+Grace bzw. Admin-MVP-Fallback für 12.10.2026.
- Gesamtplanung: `docs/ROADMAP.md` | Umzug: `docs/plans/2026-10-11-v0.2.2-relocation.md`.

## v0.3.1 source candidate (2026-10-12)
- OP orphanlist/adoptcore transfer for verified deleted FTB UUID, no automatic name takeover, grace/timer preserved. FTB last-member deletion NOT prevented.
- Higher-priority left-click & break-speed guard for physics compatibility (runtime unverified).
- Creative placed-token inventory sweep once per second (runtime unverified).
- No new dependencies; claims remain opt-in, raids disabled.
- See docs/releases/v0.3.1.md and docs/plans/2026-10-12-v0.3.1-stability.md.
