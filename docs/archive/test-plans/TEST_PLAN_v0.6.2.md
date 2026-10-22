# v0.6.2 acceptance tests — do not run destructive cases on production

## Before testing

- [ ] `git status --short` clean, starting branch `feat/v0.6.1-update`, **HEAD `48433f4`**, patch is applied to new `feat/v0.6.2-update` branch.
- [ ] `VERIFY.cmd` pure Java tests passed and **NeoForge 21.1.249 CI compiled** `roglercore-0.6.2.jar` (run-time validation still outstanding).
- [ ] Backup **world, entity regions, FTB Chunks and FTB Teams** together. Restore fresh identical copies between destructive A/B tests.
- [ ] Remove the older Rogler Core / Rogler TeamWars JAR; exactly ONE `roglercore-*.jar` installed on client and server. Warium recovery mode stays `OFF` for regular gameplay.

## Baseline: equivalent protection

- [ ] 2 teams on Overworld with core, auto-claim radius=1: TNT at *all 9 protected chunks*, at a claim boundary, and in an unprotected chunk. No protection leaks; other terrain behaves normally.
- [ ] Repeat with radius 0 and 3, with negative X/Z coordinates; test cores near each other and overlap.
- [ ] Move a core; trigger another explosion **in the same game tick if practical**, or immediately next tick. Old zone no longer protected; new zone protected. No stale event cache.
- [ ] Different dimension: no Overworld index applied to another dimension.
- [ ] `/rogler admin explosionstats`: counters increase, timings plausible; confirm numbers are handler-only. Use CraftProbe/JFR/spark to measure overall MSPT and event timings separately.

## Claim repair & repeated external removal

- [ ] Read-only `/rogler admin coreaudit` does not modify blocks/claims; load/unload a core chunk and observe `unloaded_unchecked`.
- [ ] On a throwaway world, set core location to air while **the chunk stays loaded**. No immediate respawn; after >=5s the core is automatically restored *once*, without new item or resetting armor, Grace or upgrades.
- [ ] Destroy same core again twice inside 2 minutes, waiting >5s between. At most two automatic restorations total; on next disappearance the core enters recoverable ESCROW and only recorded auto-claims are released. The exact timing can depend on server ticks.
- [ ] Replace core position with stone: direct ESCROW + auto-claim release, **no overwriting of stone**.
- [ ] Team with manual unrelated FTB claim: ensure manual chunk survives. Foreign-owned previously recorded chunk: ensure foreign ownership untouched, stale receipt cleared.
- [ ] Temporarily unavailable FTB API / missing original FTB team: pending receipts reported; never blindly unclaim foreign-owned chunks.
- [ ] `/rogler admin corerepair apply`: explicit reconciliation pass, still subject to 5-second wait for air restoration.
- [ ] Relocate normally and restart: no ghost core on old position, no duplicate items, private 27-slot inventory and Create generator persist.

## Dedicated multiplayer and high-load Warium test

- [ ] Two actual clients in opposing FTB Teams; enemy cannot open core, modify protected blocks or cross ownership.
- [ ] Compare v0.6.1 and v0.6.2 with **same snapshot**, same nuke, same loaded chunks, same mod versions; capture `explosionstats`, JFR and CraftProbe/spark timings. Do not report speedup from a single watchdog stack.
- [ ] Trigger Warium FusionBlast on disposable copy, watch CPU/tick latency, heavy item drops, and log `EXPLOSION_GUARD_SLOW`. 60-second Watchdog from Warium itself may still occur; no claim of universal fix.
- [ ] Restart after test 2–3 times. Confirm no additional core restoration after ESCROW, no phantom claims and no data corruption.

## Rollback

Stop server, restore matching world+FTB backups, reinstall v0.6.1; never roll back only the jar after changing claims/ESCROW in a destructive experiment.
