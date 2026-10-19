# LATEST v0.7.2 SOURCE CANDIDATE

Read `docs/releases/v0.7.2.md` and `docs/LLM_HANDOVER_v0.7.2.md`. The new client team intel HUD is **not a native Xaero map layer or terrain import**. FTB team membership is checked for both sender and each teammate; no enemy coordinates are emitted. Java-only tests do not establish NeoForge compilation or in-game privacy. Full-source ZIP + Git-format PATCH.cmd patch are required.

# Rogler Core v0.6.2 — latest source handover

Read `docs/releases/v0.6.2.md`, `docs/LLM_HANDOVER_v0.6.2.md`, `docs/TEST_PLAN_v0.6.2.md`, and `docs/PROJECT_STATE_v0.6.2.md`. This source candidate optimizes per-event explosion protection, bounded core recovery and stale FTB receipt cleanup, with handler-only measurements. **Do not claim Warium watchdog elimination or real performance gains without a dedicated-server A/B test.** Base SHA `48433f4`. No new mods or data schema. Keep original GitHub/LLM files in the exact post-patch Git archive.

---

# Rogler Core v0.6.1 — newest project handover

**START HERE:** `docs/releases/v0.6.1.md`, `docs/LLM_HANDOVER_v0.6.1.md`, `docs/TEST_PLAN_v0.6.1.md`, `docs/PROJECT_STATE_v0.6.1.md`. The user-facing project was renamed Rogler Core without touching registry IDs or SavedData. Warium recovery is strictly opt-in and unverified against real Warium crash-loop saves. No generic nuke fix is claimed. Preserve original workflows, compiled mod requirements, world/FTB backups and Git history.

---

# v0.6.0 critical handover

**Must read:** `docs/releases/v0.6.0.md`, `docs/TEST_PLAN_v0.6.0.md`, `docs/LLM_HANDOVER_v0.6.0.md`. v0.6.0 implements a nuclear-bypass core integrity watchdog and a visible Create shaft aperture. No generic third-party nuclear immunity, map sync, voice chat or guaranteed production-readiness. Read CI and two-client tests before accepting.

---

# LATEST v0.5.3 GUI handover

**Before editing** read `docs/releases/v0.5.3.md`, `docs/PROJECT_STATE_v0.5.3.md`, `docs/CORE_DASHBOARD_ARCHITECTURE.md`, `docs/TEST_PLAN_v0.5.3.md` and `docs/LLM_HANDOVER_v0.5.3.md`. v0.5.3 replaces chat floods with an owner-only server-authoritative core dashboard. `CoreInfrastructureSavedData` schema, FTB claims, raid gates and team ownership are unchanged. Build target 1.21.1 NeoForge 21.1.249 Create 6.0.10; GUI compile is NOT proven by pure Java tests.
---

# Current v0.5.2 handover

For new LLM sessions read `docs/releases/v0.5.2.md`, `docs/TEST_PLAN_v0.5.2.md`, and `docs/research/CORE_ONBOARDING.md` first. Current codebase uses Create 6.0.10 native kinetic generator API; Create is newly **required** in v0.5.2 (already present in the actual modpack). No Minecraft runtime claim before GitHub Actions and DEV-world test. Source ZIP does not mean compiled/verified JAR. Avoid name-based team ownership, blanket item capabilities, free infinite tech output, and irreversible world migrations.

---

# AGENTS.md — Binding collaboration rules for GPT-6, Codex and human contributors

## Mission and deadline
Create a playable team-based NeoForge 1.21.1 Minecraft server for 8–12 players by **Monday 2026-10-12**. Never claim a Minecraft mod works unless it compiled and ran in the target profile. Deadline > optional polish; make incremental working slices.

## Session start checklist (MANDATORY)
1. Read `docs/PROJECT_STATE.md`, this file, `docs/ARCHITECTURE.md`, and active plan in `docs/plans/`.
2. Review `git status --short`, last commits, targeted source files and changes; never reset or overwrite another contributor's work.
3. Select ONE bounded workstream, define an acceptance test, implement, test, update PROJECT_STATE + plan + HANDOVER.
4. If unable to build due to missing dependencies, label it UNVERIFIED, do not invent logs.
5. Preserve data compatibility or supply a migration. Branch names `feat/...`, `fix/...`, `spike/...` with normal Git commits.

## Immutable gameplay requirements
- Exactly one player-facing map: Xaero Minimap + Xaero World Map; FTB Chunks handles only claims/tickets in background.
- Exploration permanently owned by **individual UUIDs**; team map = union of the CURRENT members' personal exploration; quitting revokes all ex-member-only areas while overlap and personal history remain.
- Chunky world generation NEVER counts as discovery. Per-dimension exploration provenance.
- Team core may be placed only in Overworld and between admin-configurable min/max radial distances from worldspawn.
- Carrier sees world border effects/HUD and zones on Xaero; transport 1 hour, then safe placement or return; NEVER place into liquids/foreign claims/invalid blocks just because coordinates fit.
- Mobile core may exceed outer radius for a **cumulative 10-minute** budget, then siren/countdown + controlled self-destruct. Prevent nuke as cheap griefing weapon.
- Core has independent FTB-loaded chunk quota, separate from manual FTB tickets. Integrate Chunky Friends for pregen.
- Core inventory expandable; investigate Sophisticated Core upgrade handler, don't assert all upgrades work.
- Optional cross-team item trading; paid team rename fees distributed to OTHER teams. Persistent team UUID, mutable display name.
- Configurable rules and live admin controls whenever safe. No unbounded chunkloading/tick-acceleration.

## Architecture guardrails
- `core` has **zero Minecraft/NeoForge/FTB/Xaero imports**. Deterministic gameplay rules take explicit inputs and produce decisions; adapters own effects.
- Server is source of truth; client/UI untrusted. All trade, claim, permissions and map membership enforced server-side.
- Never merge Xaero team tiles into the player's personal map cache. Revocation must remove visible team-only tiles; test cache persistence across logout/reconnect.
- No global world-border modification; no indiscriminate huge vanilla TNT chains.
- Add tests for overlapping map ownership, negative coordinates, teams changing, server-restart recovery, duplicate core attempts and raids before claiming done.
- Don't silently replace or upgrade Xaero trio (`World Map`, `Minimap`, `XaeroPlus`) or Sable bridges. Confirm exact compatibility first.
- Do not embed or redistribute third-party binary JARs or copy ARR game mod sources into this MIT repo.

## Completion contract per task
- Describe scope, files changed, tests run with actual outcomes, untested points, rollout and rollback instructions, next step.
- Update `docs/PROJECT_STATE.md` (truth ledger) and relevant file under `docs/plans/`.
- If work is only a research spike, put results in `docs/research/` and explicitly state yes/no/unknown.
- Prefer small integrated PRs and feature flags; preserve compatibility with existing modpack diagnostic baseline.

## Forbidden shortcuts
- Never publish `core` demo JAR as a Minecraft mod.
- Never say Xaero UI support works just because the exploration union algorithm passes.
- Never disable all protection on a public multiplayer server to make raiding work.
- Never connect to production world before a backup and throwaway-world acceptance test.
- Never assume Gradle plugin binaries were obtained in this offline environment.

## v0.1.0 handover requirement (2026-10-10)
- FTB Teams 2101.1.11 owns memberships; do not let a secondary TeamDirectory override/duplicate the FTB roster.
- Test the FTB reader reflection against the EXACT running version; source-level verification is not runtime proof.
- The initial nametag implementation projects online player membership to vanilla scoreboard. Other mods can interfere. **Never claim universal name hiding** until full-pack client HUD tests.
- v0.1 `max_parties` is observational; only `max_party_size` in FTB config truly restricts party size. Enforce creation validators in a later tested feature branch.
- Build CI is present but unrun; use the first GitHub Actions result as evidence, not as an assumed success.

## v0.2.0 source branch handover
- Read `docs/MOD_REQUIREMENTS.md` on every release and ALWAYS report REQUIRED and OPTIONAL mods with any new dependency explicitly flagged.
- Base Core signed item + state in `BaseCoreLedger` and `BaseCoreSavedData`. Do not clone issue/create logic in another adapter.
- The 1h carry deadline first attempts a standard `BlockItem.place` (only when hand-held on supported nearby terrain), otherwise expires into admin escrow. No raw `setBlock` claims bypass; no out-of-zone item teleport yet.
- `BaseCoreService.info` is ADMIN ONLY. Never introduce an unprivileged command that leaks other teams' core coordinates.
- No FTB claim protection, grace period, chunkloader, Xaero render or Sable ship controls yet. Mark these as PENDING even if mod compiles.
- Never rename/version-bump without keeping `neoforge.mods.toml`, Gradle properties, CI artifact, release docs aligned.

## v0.2.1 patch handover
- See `docs/releases/v0.2.1.md`. Radius default is now `0..5000`; only untouched v0.2.0 generated configs migrate automatically.
- Do not auto-reissue because an item is not visible in player inventory: it may be in an unloaded chest/offline inventory. Only genuine event-triggered hazard ESCROW or explicit OP `reclaimlost` revokes the token.
- All auto-placement attempts use `BlockItem.place` attributed to a real nearby team player; avoid bypassing claim hooks. Fallback is persistent ESCROW with `/teamwars base recover`.
- Real NeoForge compile and Minecraft verification are mandatory before release.

## v0.2.2 handover (2026-10-11)
- `docs/ROADMAP.md` ist die aktuelle Produkt-/Prioritäten-Roadmap. `docs/plans/2026-10-11-v0.2.2-relocation.md` ist der konkrete Plan.
- Basiskern bleibt unzerstörbar: **kein** gewöhnliches Mining, sondern serverseitig autorisiertes `Shift+Rechtsklick` bzw. `/teamwars base pack`. Nicht durch normale Drops ersetzen.
- Bei Umzug `PLACED -> ISSUED`, immer neuer Token, Transportzeit erhalten; `/teamwars admin salvagemissing` ist nur für fehlenden physischen Block. Admin darf damit keine echte gegnerische Base zerstören.
- Beim späteren Raidmodul den TODO-Guard in `BaseCoreService.pack` für *aktive Raids* implementieren, bevor Raids öffentlich freigegeben werden.
- V0.2.2 gilt erst mit CI- und Minecraft-Laufzeitbeleg als releasefähig.

## v0.3.0 handover (2026-10-12)
- CoreGuard native block/interact/explosion rules are supplemental to FTB Chunks, NOT a claim of universal modded explosion/automation protection.
- FTB claim bridge uses reflected upstream methods and records only chunks it created; on relocation only those may be automatically unclaimed. Never delete manual/team pre-existing claims.
- Grace timers are persisted UTC epochMillis per team and cannot refresh on pack/recover.
- Never enable destructive raids in v0.3.0. `raids_enabled=true` intentionally fails config parsing.
- Claim snapshot and core snapshot persist in different SavedData files. Back up both and FTB Chunk claim files together.
- Read `docs/releases/v0.3.0.md` and complete acceptance checks before editing v0.4.

- SECURITY: FTB claim visibility in user-provided config is PUBLIC. `auto_ftb_claims=false` by default. Never enable until per-team visibility is verified PRIVATE in two-client test.

## v0.4.0 contract (2026-10-12)
- For the latest handover FIRST read `docs/PROJECT_STATE_V0.4.0.md`, `README_v0.4.0.md`, and `docs/LLM_HANDOVER_v0.4.0.md`; upstream historical README/PROJECT_STATE differ from the offline baseline and must not be overwritten by an old-blob Git patch.
- Deliver TWO artifacts every future build: complete source ZIP containing *all* tracked source, AGENTS/LLM/docs/workflows and a Git-format update patch. JAR only after actual NeoForge CI success.
- Never assume claims are secret merely because `def_claim_visibility=private`. Team-specific Claim Visibility is authoritative. New automatic FTB claims are fail-closed unless FTB property PRIVATE can be confirmed. Two opposing clients must validate map and packet behavior.
- Raids have ONLY direct registered-core hits during OP-approved windows. BaseGuard and native explosion protection remain ON even for active raids. No automatic general block destruction or offline raids.
- Core victory -> recoverable ESCROW -> new token upon team recovery. Exactly one persisted score per unique successful victory, preserved after restart. Preserve legacy SavedData and avoid fake team names as identities.
- Windows Git object folder locking remains unrooted. Never repeat blind `git am`, delete `.git/objects`, or run aggressive GC on user's machine.

## v0.5.0 LLM handover (2026-10-13)
- Read `docs/PROJECT_STATE_v0.5.0.md`, `docs/releases/v0.5.0.md` and `docs/HANDOVER_v0.5.0.md` for the newest milestone before relying on old historical roadmaps.
- Always deliver a complete source ZIP including tracked scripts/LLM files in addition to Git patch. The local `PATCH.cmd` creates the exact tracked snapshot after patch.
- Team UUIDs are canonical; never auto-claim orphan ownership by recreated display name.
- FTB Chunks `PRIVATE` is a stock map UI protection, not an unmodified-packet location confidentiality guarantee.

## v0.5.1 LLM contract (2026-10-13)
- Start with `docs/PROJECT_STATE_v0.5.1.md`, `docs/releases/v0.5.1.md`, and `docs/research/CORE_CREATE_AE2_SOPHISTICATED_DESIGN.md`.
- Nine-slot core inventory is SERVER/UUID-owned SavedData, never in movable item NBT; no generic item capability until machine caller authorization is solved.
- Native FE output 20/40/80 FE/t with world config, 20k buffer and 160/t max push; do not claim actual AE2 ME-grid terminal or Create rotational API integration.
- Build remains UNVERIFIED until CI green, and runtime remains UNVERIFIED until user tests. No silent new dependencies or unsanctioned server world modifications.
- Every release: downloadable full-source ZIP incl. AGENTS and all LLM/docs/scripts; Git patch; installation/verification instructions; list partial/unimplemented integrations honestly.

## v0.6.4 Sable-lifecycle guard (2026-10-15)
- Primary pack WITHOUT Sable: Destructive; keep Sable `sub_level_splitting=true`.
- Narrow preflight only against exactly Sable 2.0.6, no post-mutation `assembleBlocks` cancellation, no raw MCA rewriting and no forced chunk load. Unknown versions and Destructive installed => guard inactive.
- Read `docs/LLM_HANDOVER_v0.6.4.md`, `docs/releases/v0.6.4.md`, `docs/TEST_PLAN_v0.6.4.md`; v0.6.4 policy tests alone do not verify Mixin application or world stability.
- Always return format patch AND full source ZIP with all repository docs and LLM handovers; exact source after patch via user's git archive, not condensed offline snapshot.
