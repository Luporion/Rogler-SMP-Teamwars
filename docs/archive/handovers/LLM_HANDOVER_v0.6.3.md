# LLM handover: Rogler Core v0.6.3

**Always read `AGENTS.md`, `docs/PROJECT_STATE.md`, this file, and `docs/releases/v0.6.3.md`.** Repo: `Luporion/Rogler-SMP-Teamwars`; Minecraft 1.21.1 / NeoForge 21.1.249; Create 6.0.10. Previous real GitHub build: v0.6.2 `1321a76a` green. User wants patch + entire source ZIP, including all LLM docs, for each release.

v0.6.3 is a **low-risk, non-invasive compatibility-readiness update** based on handover `Rogler_Core_Sable_Destructive_Explosion_Crashpraevention_Handover_2026-10-15(2).md`: retains `sub_level_splitting = true` and accepts Sable: Destructive 2.2.0's own removed-sublevel guards. New Java-only `SableCompatibilityPolicy`, `SableSplitConfigAudit`, `ExplosionObservationCounter`; NeoForge adapter `SableCompatibilityService`. No mixins, cross-world coordinate conversions, optional class dependencies, entity-region writes, dynamic explosion splitting, or item deletion.

The user has **not yet done in-game validation of v0.6.2/0.6.3**. CI compile and runtime are separate acceptance gates. The exact Sable: Destructive API signatures and Mianbao/Warium compiled classes were NOT fetched from the installed jars, so direct integration must wait. Do not infer mod IDs for those addons from user-facing jar names.

Read-only command overview: `/rogler admin compat report|sable|mods|explosions|reload`. Existing `/rogler admin explosionstats` remains the **own handler** timing measure. `roglercore-sable.properties` observation default false; config check reads `config/sable-common.toml` only. Watch for process working-directory mismatches on Crafty. Version name v0.6.3; internal registry namespace remains `roglerteamwars` and saves retained.

Important world issue from another chat: a root-owned, rewritten entity region (`r.-1.0.mca`) caused AccessDenied for `crafty`, later repaired to `crafty:crafty 644`. No raw region/claim-file edits in this mod; external recovery requires backup, UID/GID/mode/ACL/xattr preservation, atomic replacement and testing as server process user.

Next: NeoForge CI, smoke test with/without Sable & Destructive, 200 real split cycles, then isolated Mianbao / Warium integration planning with exact JVM bytecode and Sable Destructive API signatures. Avoid doubling up on the addon’s heatmap/tick/split guards. `BaseGuardService.onExplosion()` was indexed in v0.6.2; do not rewrite prematurely or claim the synchronous Warium watch dog is fixed.
