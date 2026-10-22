# Rogler Core v0.6.1 acceptance checklist

## Source/build

- [ ] Apply `Rogler-TeamWars-v0.6.1-update.patch` to **clean v0.6.0 commit `a10c7d4e`**; ensure no `git am` state, no force-push, no object-store cleanup.
- [ ] `VERIFY.cmd` passes all Java tests, including `WariumRecoveryPolicyTests`.
- [ ] GitHub Actions NeoForge Build passes on NeoForge 21.1.249 + Create 6.0.10, artifact named `roglercore-0.6.1.jar`.
- [ ] `M:\Downloads\Rogler-TeamWars-v0.6.1-full-source-exact.zip` (first transitional run of existing PATCH.cmd) exists and includes AGENTS.md, LLM handover, workflows.

## Branding/compatibility

- [ ] With prior TeamWars JAR **removed**, Minecraft loads the new Rogler Core JAR, with the same `roglerteamwars` registry IDs.
- [ ] `/rogler status` and `/teamwars status` both work. `/rogler base menu` and `/teamwars base menu` open the same inventory.
- [ ] Old bases: 27-slot inventory, 16 RPM/256 SU Create, FE transfer, FTB auto-claims, Grace, upgrades and recovery survive restart.
- [ ] On a world copy, no new/modded block or item turns into a missing registry entry; no duplicate mod ID warning.

## Quarantine, in a copy only

- [ ] Without Warium CE installed, server starts; `/rogler admin compat report` shows optional integration unavailable; no NoClassDefFoundError.
- [ ] With Warium CE and default `OFF`, normal entities unaffected and zero blocking.
- [ ] In `LOG_ONLY` on a controlled Warium test (avoid dangerous nukes), matching type reported; `detected_id` reflects the runtime registry. If no type appears, do not assume compatibility.
- [ ] Prepare recovery config **before** server starts with `BLOCK_DISK_LOADED`; start a **crash-affected copied save**, check if `blocked > 0` without ticking its saved FusionBlast entity.
- [ ] Verify 2–3 restarts; no duplicate/regenerated explosions, unrelated Warium entities survive. Verify core inventories and FTB claims.
- [ ] Set `OFF`, stop & restart; verify default mechanics. `reload` does not retroactively evaluate already loaded entities.
- [ ] Wrong type, wrong registry namespace or mismatched exact ID are always untouched.
- [ ] Any `.mca` parsing failure before entity construction is **not** classified as recovered; no corrupted-region modifications.

## Not in this release

- [ ] Explosion performance optimization and Core lifecycle benchmarks: v0.6.2.
- [ ] Sable/Mahou/Aether fixes and progressive nukes: require isolated reproductions and versioned tests.
