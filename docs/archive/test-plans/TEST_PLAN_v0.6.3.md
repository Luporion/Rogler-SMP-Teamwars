# Rogler Core v0.6.3 — Implementation + Acceptance

Source base: v0.6.2 branch `feat/v0.6.2-update`, full commit `1321a76ac55fbacd7ff7497c0d450781f43c5fc3`.

### CI gate

1. Apply `Rogler-Core-v0.6.3-update.patch` to **clean** base; run `VERIFY.cmd`/`bash scripts/test-core.sh`. Expect 491 old + 45 new = **536** assertions.
2. GitHub workflows P0 Core and NeoForge Build must both succeed before the JAR is installed. Ensure artifact `roglercore-0.6.3-unverified-runtime` contains `roglercore-0.6.3.jar`. Ingame is still unverified.
3. Create an exact full tracked source with `git archive`; include `AGENTS.md`, docs, CI, scripts, old releases and LLM handovers.

### Ingame tests AFTER CI, on a backed-up throwaway world

- [ ] Without Sable/Sable: Destructive (if profile allows), server starts and `/rogler admin compat sable` states mod not confirmed. No optional-mod `ClassNotFound` errors.
- [ ] With Sable 2.0.6 + Sable: Destructive 2.2.0, verify both installed registry IDs and versions in `/rogler admin compat mods`; compare with each jar's own `META-INF/neoforge.mods.toml`.
- [ ] Confirm `config/sable-common.toml` STILL says `sub_level_splitting = true`. `/rogler admin compat sable` reports ENABLED, heatmap steps; if MISSING check actual server working directory.
- [ ] `/rogler admin compat report` shows existing Warium status and separate Sable status; no world mutation.
- [ ] Default `explosion_observation_enabled=false`; `/rogler admin compat explosions` shows OFF.
- [ ] On test server only, write `world/serverconfig/roglercore-sable.properties` with `explosion_observation_enabled=true`, run `/rogler admin compat reload`; TNT; `compat explosions` shows event counts (the event can come from other mods). Confirm counters clear after reload.
- [ ] With both Destructive and Collision Damage installed, look for A/B_TEST warning and compare impact behaviour with the latter disabled; DO NOT disable splitting.
- [ ] Sable Destructive 2.2.0 should protect removed-sublevel processing per release notes; verify its own Mixin logs and 200+ safe split/merge/unload cycles.
- [ ] Vanilla explosions, Create/CBC then Mianbao and Warium in a **throwaway world only**. Check CoreGuard, claims, no ghost claims, no item loss. Collect spark/CraftProbe, `explosionstats`, `compat explosions` as separate metrics.
- [ ] Three restarts: no duplicate Core, inventory/FE/Create/FTB consistency; Warium quarantine OFF unless explicitly enabled on a copy.

No implementation claims yet for Mianbao-to-Sable native API, Mahou AABB, progressive Warium, or Sable removed-sublevel bug itself. Those need actual mod JAR and dedicated-server integration tests.
