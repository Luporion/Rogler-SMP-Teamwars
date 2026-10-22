# Rogler Core v0.6.1 LLM/maintainer handover

**Project:** Same existing repository, not a replacement mod. Original GitHub repository: `Luporion/Rogler-SMP-Teamwars`; original commit `a10c7d4e` (v0.6.0). v0.6.1 is a compile-candidate unless GitHub Actions has subsequently passed.

Read `AGENTS.md`, `docs/releases/v0.6.1.md`, `docs/archive/test-plans/TEST_PLAN_v0.6.1.md`, `docs/ROADMAP.md`, `docs/ARCHITECTURE.md`, and user technical handover from 2026-10-15. Never claim the Handover's raw crash-ZIPs were inspected in the v0.6.1 source build unless actually verified.

**IDs DO NOT CHANGE:** `@Mod("roglerteamwars")`, `mod_id`, Java packages `dev.rogler.teamwars`, Resources `assets/roglerteamwars`, `roglerteamwars_basecores`, `roglerteamwars_protection`, `roglerteamwars_infrastructure`, old serverconfig names. Only name, JAR basename (`roglercore`), command alias (`/rogler`), UI title, log name and release metadata are renamed. `/teamwars` stays the full command tree. No migration tool needed and none may be invented.

**Warium recovery:** `core/.../compat/WariumRecoveryPolicy` contains pure Java decisions; `mod-neoforge/.../compat/WariumRecoveryConfig` loads early per-world properties; `WariumRecoveryGuard` intercepts `EntityJoinLevelEvent`, only if Warium installed, only exact observed class `FusionBlastEntityEntity` + mod registry namespace `wariumce`, optionally also expected registry ID. No direct Warium library import. Default OFF; `BLOCK_DISK_LOADED` only with backups. No `.mca` manipulation or entity `discard()`; a cancelled join may reappear from disk at next restart.

**Never call chunk retrieval, level.getBlockState(), synchronous biome lookup, or filesystem reads inside EntityJoinLevelEvent.** Lifecycle config loads in `ServerAboutToStartEvent`. Bounds of last 20 log records; aggregate first five and every hundred, no packet leak to normal players, OP 4 only.

**Next:** v0.6.2 address exact `BaseGuardService.onExplosion` hotspot: current `ev.getAffectedBlocks().removeIf(pos -> owner(...pos)!=null)` calls `owner()` and scans every registered Base Core per affected block. Build a world-scoped, dimension/ChunkPos index from ledger state (not from all-world chunk loads), invalidate on placement/pack/recover/raid. Create meaningful before/after benchmarks on identical Warium test copies and check claims at radius boundaries. Keep FTB ownership strictly UUID-based and do not unclaim manual chunks.

**Open research:** WariumCE 1.0.3/1.0.4 binary entity registry ID and join behavior; Sable/Mahou huge AABB hypothesis; Aether `RemoveSeedsModifier` biome access; safe progressive nukes (experimental and not yet technically verified); invalid Anvil region `r.-1.-3.mca` must be diagnosed with independent read-only tool.

**Artifacts every build:** complete, ideally `git archive`-derived source ZIP (all tracked scripts/LLM docs), git-am patch on pinned base, install/test plan, actual CI artifact only after success. Validate changed old file Git blob hashes against pinned upstream first.
