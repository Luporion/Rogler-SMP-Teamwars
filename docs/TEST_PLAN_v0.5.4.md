# v0.5.4 test checklist

**Mandatory first:** green GitHub Actions for *both* Core Tests and NeoForge Compile; stop if not green. Backup DEV world, including TeamWars, FTB Teams and FTB Chunks SavedData.

- [ ] Starting from an already placed v0.5.3 core with the Create drive already purchased, upgrade jar to v0.5.4. Without moving core: attached shaft spins, goggles say 256 SU at current speed, Stressometer agrees and a few basic Create machines run.
- [ ] Fresh test team: buy Create directly on a placed core; connected shaft starts within one second without pack/place; fresh load and chunk unload/load both work.
- [ ] Unupgraded core remains 0 RPM/0 SU. A different Create generator cannot create free TeamWars stress or enable unpurchased drive.
- [ ] Core inventory displays 27 correct slots (3×9) and player inventory 36 slots. Move items including slot #9 and #26, shift-click, close/reopen.
- [ ] Relocate core, reload world and verify items in new slots remain; FE/Create/armor tier and Grace retained. Never downgrade without emptying new slots.
- [ ] GUI guide without normal Minecraft BOOK: reject; with exactly 1 BOOK: consume 1, give requested registered manual; duplicate held: reject without consuming input.
- [ ] Command `/teamwars guide give <modid>` applies identical paid exchange; full player inventory with multiple books cannot lose book without receiving manual.
- [ ] FE/AE2 neighboring Energy Acceptor and Create Crafts & Additions Connector still receive power; FE buffer and claim synchronization unchanged.
- [ ] Enemy player cannot open or shift-click core inventory or obtain books from enemy core. Solo check at least no-team/Creative, multiplayer later.
