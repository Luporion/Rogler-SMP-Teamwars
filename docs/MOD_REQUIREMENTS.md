# Mod requirements — Rogler TeamWars v0.2.0

Validated against uploaded CraftProbe Session Analysis 2026-10-10 04:31:57 (`mods.csv`), client profile only; NOT a dedicated-server install inventory.

## REQUIRED in v0.2.0 (server and joining clients)
- Minecraft **1.21.1** with NeoForge **21.1.249**.
- **Rogler TeamWars 0.2.0** (replaces `roglerteamwars-0.1.0.jar`; NEVER load both).
- **FTB Teams 2101.1.11** (`ftb-teams-neoforge-2101.1.11.jar`).
- FTB Teams dependency **FTB Library 2101.1.35** (`ftb-library-neoforge-2101.1.35.jar`).

All of the above except the new TeamWars JAR already appear enabled in the uploaded DEV modlist. TeamWars 0.2.0 requires replacing TeamWars 0.1.0, no other download.

## PRESENT but not required by v0.2.0 code
- FTB Chunks 2101.1.21: already present, but **Base Core has NO claim/raiding protection integration yet**.
- Chunky 1.4.23 / Chunky Friends 1.1.2: pregen adapters NOT implemented.
- Xaero Minimap 26.4.2 + World Map 1.45.0 + XaeroPlus 2.36.0: boundary rings and team exploration overlays NOT implemented.
- Simple Voice Chat 2.6.22: existing standalone voice chat; team radio NOT integrated.
- Sophisticated Core 1.5.1.2341 / Sophisticated Backpacks 3.26.3.2158: Base Core upgrade inventory NOT integrated.
- Create Aeronautics + Sable: mobile core and ship nuke NOT implemented.

## New mods for THIS release
**None**. New JAR is a replacement upgrade of Rogler TeamWars only.

## Caveat
The uploaded `mods.csv` describes the DEV client, not the authoritative dedicated-server file listing. Verify all required mods on both sides before launch. No upgrade of Xaero or NeoForge is authorized by v0.2.0.
