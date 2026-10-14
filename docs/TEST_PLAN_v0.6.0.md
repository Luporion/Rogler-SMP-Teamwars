# Rogler TeamWars v0.6.0 test checklist

**Use a throwaway world copy with complete FTB data backup; do NOT test nuclear weapons on production.**

- [ ] NeoForge CI builds successfully, v0.6.0 JAR is present, core policy suite passes.
- [ ] With Create upgrade already bought, shaft appears physically connected through the industrial core casing on X-axis.
- [ ] Turn core placement direction; check Z-axis connection. Use Create Goggles + Stressometer: 16 RPM, 256 SU, a basic machine works.
- [ ] Pack/replace with shafts nearby; no duplicated shaft or missing motor; rotation and existing upgrades persist.
- [ ] Normal right-click GUI, 27 team slots and inventory, FE output, paid manuals, claims and Grace still work.
- [ ] On throwaway copy while chunk loaded, `/setblock <core-x> <core-y> <core-z> minecraft:air` (OP) -> check `/teamwars admin coreaudit`; registered core is reconstructed without duplicate item.
- [ ] Repeat with a non-air test block replacing core using `/setblock` (after backup): core must ESCROW and auto-claims disappear; manual claims remain.
- [ ] With another ordinary claimed chunk, exercise `coreaudit` again: no manual chunk ownership should change.
- [ ] Relocate normally, then `/teamwars admin coreaudit`: no phantom core restored at old position.
- [ ] Warium Nuke on throwaway copy: observe whether third-party terrain is affected. Core either repaired at its registered air position or moved to recoverable ESCROW; no permanent silent PLACED+missing+claims mismatch.
- [ ] Restart and load affected chunk: no duplication, persisted state and cleanup retries.
- [ ] Two enemy players: BaseGuard/FTB ownership, private maps and core access unchanged.

**Rollback:** restore world and FTB backups together before reverting JAR, especially after placing a repaired core. Source archive is NOT a Minecraft runnable JAR.
