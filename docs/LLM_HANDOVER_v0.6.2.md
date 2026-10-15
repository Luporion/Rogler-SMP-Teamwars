# Rogler Core v0.6.2 — LLM handover

Base SHA `48433f4` (v0.6.1 including watch-actions.ps1 fix). This release must keep legacy MOD_ID `roglerteamwars` and SavedData IDs to protect existing worlds. Core tests 491 assertions offline, NeoForge CI / dedicated server not verified at source packaging time.

Read `docs/releases/v0.6.2.md` and `docs/TEST_PLAN_v0.6.2.md` FIRST. No hard-coded Warium APIs introduced; v0.6.1's opt-in FusionBlast recovery is untouched.

Changes: `CoreExplosionIndex` (pure Java, event-local primitive chunk hash table; explosion `removeIf` uses O(1) lookup, no per-position core-ledger scan); `CoreRecoveryPolicy` (5-second observed-air window and 2 restorations per 2 minutes, in-memory counts); `CoreClaimCleanupPolicy` (clear expired/foreign receipts without unclaim); `BaseGuardService` integration with handler-only microsecond counters, read-only coreaudit, explicit corerepair apply. New P0 policy tests both Windows and Linux. No save-schema changes.

Risks: Warium direct block edits may bypass `ExplosionEvent.Detonate`; main server remains blocked during 60-second explosion, so recovery is not a watchdog cure. Recovery attempt limit resets on server restart; no persistent throttling (ESCROW is persisted once reached). FTB same-team reclaim of former auto-managed chunk cannot be reliably distinguished with old SavedData receipt schema. FTB absent/team deleted can leave pending receipt until admin decision. Benchmark on real heavy explosion before claiming speedup. No loaded chunks should be forced by audits.

Future: verify WariumCE 1.0.3/1.0.4 EntityType IDs, tested FusionBlast recovery, FTB manual reclaim provenance, Sable/Mahou coordinate mixing, optional explosion admission control, Xaero incremental shared exploration, voice range and team marker systems. Progressive nukes remain experimental.

Release contract: exact full Git archive including all original GitHub/LLM files produced by user's `PATCH.cmd`; source ZIP bundled here is offline assembled and may contain historical snapshots not byte-identical to GitHub. Always keep patch based on actual v0.6.1 GitHub file blobs, never overwrite README.md or docs/PROJECT_STATE.md from an outdated snapshot. CI-only built JAR: `roglercore-0.6.2.jar` after success, not offline.
