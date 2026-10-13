# Core onboarding concept and boundaries

## Principle: one central terminal, native mod tutorials

A TeamWars core is an early-tech orientation station, NOT a free late-game source of power/storage. Starter workflow:

1. Place core -> `/teamwars guide` or right-click with vanilla Book.
2. Gain 20 FE/t to basic FE machine by placing Energy Acceptor or cable directly alongside core. No connector required; sneak with connector item is no longer intercepted by pack logic.
3. Purchase Create 16 RPM/256 SU generator after finding 16 Andesite Alloy and 8 shafts. Real Create kinetic endpoint is on the shaft side; connect gearbox/transmission. Warning: build own waterwheels, windmills or steam generator soon.
4. AE2: Energy Acceptor + Energy Cells, Quartz Fluix infrastructure, ME Controller/Drive and Terminal. AE2's own guide supports W-on-item and GUI help.
5. Use `/teamwars guide books` to inspect detected mod manual IDs, `/teamwars guide give <modid>` only for an actual registered handbook. Avoid fictional Create guide book. Patchouli books often require mod-specific NBT, so only return actual registry items; do not manufacture unsupported NBT books.

## Alternative existing approaches

- Create / Ponder: purpose-built animated tutorials on actual Create gadgets (hover and press W).
- AE2 built-in Guide since 1.20: context sensitive pages and GUI help.
- Patchouli: can author a **TeamWars-centric** guide as a separate data resource, but do not bundle proprietary text/assets from third-party books.
- FTB Quests: server-oriented staged progression/questbook; strong alternative for 8–12 players, not included automatically in this update. Can later create TeamWars quests as datapack.
- JEI/EMI: in-game recipe/usage hints for Create, AE2, Sophisticated tools.

## Deferred integration

- Better server-facing guide GUI with icons, dynamic language localization and pagination for large modpacks.
- Data-driven allowlist of mod manual Item IDs (currently recognized names only; intentionally not every possible book).
- AE2 native grid node/ME Drive built into the core, access-control on bridge and team-private inventories.
- Sophisticated upgrade modules with explicit whitelist.
- Configurable Create 512/1024 SU tiers only after multiplayer balancing and mechanical compatibility.
- Team base tutorial quests from FTB Quests + permission hardening to avoid claiming discovery/secret base coords.
