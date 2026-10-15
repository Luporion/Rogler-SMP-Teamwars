# v0.6.4 — Acceptance / regression checklist

**Baseline:** git `c2250f90` / NeoForge 21.1.249 / MC 1.21.1 / Sable 2.0.6 / Shtreimel / Tree Physics / Create Aeronautics. `sub_level_splitting = true`. **Do NOT install Sable: Destructive for the primary test.** Back up full world, FTB data, server configs, `world/sublevels`, and `world/data/sable_sub_level_occupancy.dat`. Use a disposable copy.

## Build and startup
- [ ] `PATCH.cmd` on `feat/v0.6.3-update` clean commit `c2250f90`; core tests (558 expected) and actual NeoForge CI SUCCESS, download `roglercore-0.6.4.jar`.
- [ ] Only ONE Rogler Core jar present; `roglerteamwars` remains the internal mod ID.
- [ ] Start **without Sable installed**: no `NoClassDefFoundError` or fatal optional mixin target error.
- [ ] Start **with Sable 2.0.6** but without Destructive: `/rogler admin compat lifecycle` reports ACTIVE / ON; check loading log to ensure mixin is applied.
- [ ] Unknown Sable version: guard OFF, warning; do not conduct destructive tests until verified.
- [ ] With **Sable: Destructive** (separate OPTIONAL A/B profile), own guard OFF; no double cancellation/duplicate processing. Primary pack remains WITHOUT it.
- [ ] With `enabled=false` in `world/serverconfig/roglercore-sable-lifecycle.properties`, reload and confirm OFF; restore true afterwards.

## Functionality and data integrity
- [ ] Plain ship split (200+ cycles), both halves exist and have correct blocks, block-entities, passengers, velocity and inventories. No generic splitting shutdown.
- [ ] Targeted removed-owner tick and split with test instrumentation: preflight increments removed-owner counter, no access to removed plot.
- [ ] Independent removed-anchor plot while heatmap owner is live: preflight increments removed-anchor counter; no failed assembly or partial migrations.
- [ ] Shtreimel / Tree Physics / Aeronautics / Big Cannons mixins load; inspect startup mixin logs and confirm no target collision.
- [ ] Mianbao Nuclear Dynamite next to (and then on) Sable ship on disposable saved copy; check `already removed`, queue size and `Body has been removed` separately. Do NOT use Warium on production.
- [ ] Restart server 3 times after splits; validate sublevel UUIDs, plots, Physics Body handles, inventories, FTB claims, Core and Create/FE.
- [ ] `/rogler admin compat report`, `lifecycle`, `explosions`, `/rogler admin coreaudit`: no unnecessary chunk generation, no exceptions.

## Security/ops
- [ ] In a failed attempt, inspect Sable/Shtreimel logs and saved world contents; absence of a crash alone is NOT success if structures vanish.
- [ ] No modification to any MCA or ownership mode; if external tools touch regions, preserve UID/GID/mode/ACL/xattrs atomically with backup and test under `crafty`.
- [ ] Keep `max-tick-time` finite, no blanket item/entity deletion, no `sub_level_splitting=false` workaround.

## Known residual
Some SplitListeners may remove a parent AFTER the preflight; this guard deliberately does not intercept mid-migration. Such a real reproduced failure should be fixed upstream or by a verified transactional Sable 2.0.6-specific change, not by swallowing the exception. Until validated, **do not install this on the live SMP world**.
