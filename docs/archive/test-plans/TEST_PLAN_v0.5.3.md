# v0.5.3 — Dedicated test plan

Status: **offline tests pass; NeoForge build and Minecraft acceptance pending**.

## Mandatory CI

`PATCH.cmd` from clean `feat/v0.5.2-update` commit `367ccdc2`, GitHub Actions `P0 Core` + `NeoForge v0.5.3 Compile`. If NeoForge fails, paste full failed-step logs, not only Actions screenshot. Do not install a failed JAR.

## Solo smoke test (disposable copy of world)

- [ ] Put only `roglerteamwars-0.5.3.jar` in mods, with NeoForge 21.1.249 + Create 6.0.10. Open world and run `/teamwars status`.
- [ ] Right-click own placed core: custom industrial GUI opens (not generic chest). Five tabs work, no six-line welcome chat.
- [ ] Nine core slots and all normal inventory slots work; move items with mouse, shift-click, close/reopen; keep items across relocation and restart.
- [ ] `/teamwars base menu`, `/teamwars guide`, book-in-hand right-click open the hub within eight blocks.
- [ ] Armor/FE/Create menu buttons show exact next costs; no materials -> rejected with **no item consumption**; sufficient materials -> server deducts exact items and updates synced UI.
- [ ] Try Creative upgrade: denied; inactive / enemy / remote core: menu cannot be opened or used.
- [ ] FE tab shows output tier 20/40/80 FE/t, buffer 0–20000. Connect adjacent FE cable, Energy Acceptor and load: FE-neighbor count updates and actual power transfer can be checked on consumer.
- [ ] Create tier unlocks real shaft (16 RPM / 256 SU); tooltips and overlay match GUI, restarts and relocating preserve it.
- [ ] Help page contains Ponder / AE2 guide tips, allows claimed known registered manual; nonexistent manual refused.
- [ ] Packing with empty hand + sneak still works. With Crafts & Additions connector or wrench + sneak, the Core does not pack.
- [ ] v0.5.2 save loads normally; no different IDs, ownership, claim, Grace, raid or war-score state.

## Future dedicated multiplayer

- [ ] Other player in another party cannot open GUI and cannot trigger actions, even using modified menu click packets.
- [ ] Team member can open GUI, but final party member leaving closes it and can't spend team materials / alter core.
- [ ] Two team members purchasing simultaneously cannot double-spend.
- [ ] AE2 power and Create torque deliver as expected under server latency/claim pressure. No item capability exposed to opponents.

## Rollback

Back up world plus FTB Teams and FTB Chunks data. v0.5.3 only changes GUI/menu registration; keeps v0.5.2 disk data schema. Roll back **jar and world backup** if unexpectedly migrated/corrupted.
