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
