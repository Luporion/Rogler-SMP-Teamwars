# v0.5.2 Acceptance checklist

**Before:** world and FTB data backup; use copied DEV world; Create 6.0.10 and matching NeoForge/FTB baseline; verify GitHub build.

1. `teamwars status`: 0.5.2 and all installed mods listed. Existing 0.5.1 Base Core world load does not remove blocks, duplicate core, or reset team data.
2. Model: industrial metal frame, front panel, copper energy edges, actual Create shaft visible on its X/Z axis. Observe model from all directions and with shaders/Voxy.
3. No purchased Create upgrade: connected Create shaft shows no 16 RPM generator (0 RPM).
4. Team leader near placed core, survival with exactly 16 Create Andesite Alloy and 8 Create Shafts: `/teamwars base upgrade create` consumes exactly these items. Attaching a Create shaft on the shown side transmits 16 RPM and 256 SU capacity. Try a Gearbox, Mechanical Press or mixer. Expect output only for small load, warnings about additional generation.
5. Old team core packed & replaced: orientation still correct and kinetic drive restored without another payment. FE buffer/9 slots unchanged, no duplicated blocks or items.
6. Shift-right-click with empty MAIN hand: packs. Shift-right-click with connector / shaft / wrench: uses the held item, never packs. Ordinary right-click with Book: TeamWars guide; empty hand: private chest.
7. Adjacent AE2 Energy Acceptor receives FE; `/teamwars base energy` reports generation and buffer; cable can sit on all six sides except occupied shaft side; actual connectors depend on the external mod.
8. `/teamwars guide`, `guide books`, `guide give ars_nouveau` if matching guide exists, and `guide give unknownmod`. Check no fake unregistered items granted; guide only near own placed core.
9. World restart: FE storage, 9-slot inventory, Create upgrade persisted; no double generation, no client crash.
10. Two clients: hostile Team Blue can't pack, use GUI, acquire guide books at Red core, steal items or bypass native CoreGuard. Claims not visible to untrusted client (FTB privacy remains an open security gate).
11. Score/Raids and Orphan adoption regressions. Nothing auto-force-loads chunks. Monitor TPS/MSPT, Create + Sable compatibility.

**Status:** offline policy checks only; true NeoForge Compile + in-game verification to be provided by the user after applying patch.
