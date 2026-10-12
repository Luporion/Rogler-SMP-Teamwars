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

## v0.2.1 update (2026-10-10)
**New third-party mods: NONE.** Required: Minecraft 1.21.1, NeoForge 21.1.249, FTB Teams 2101.1.11, compatible FTB Library (latest observed 2101.1.37), and **Rogler TeamWars v0.2.1**. Remove the old TeamWars 0.2.0 JAR; never load both. FTB Chunks/Chunky/Xaero/Sophisticated/Sable integrations remain deferred. See `docs/releases/v0.2.1.md`.

## v0.2.2 Update (2026-10-11)
- **Neue Drittanbieter-Mods: keine.**
- Pflicht (Server und Client): Minecraft **1.21.1**, NeoForge **21.1.249**, **FTB Teams 2101.1.11**, dazu eine kompatible **FTB Library** (im letzten Diagnoseprofil 2101.1.37), **Rogler TeamWars v0.2.2** statt der alten JAR.
- Schon vorhanden, aber in v0.2.2 noch nicht als Basis-Kern-Adapter verwendet: FTB Chunks 2101.1.22, Chunky, Xaero, Sophisticated Core, Simple Voice Chat, Sable/Create Aeronautics.
- **Achtung:** Die Version v0.2.2 ist zunächst ein Quellcode-Kandidat; eine CI-grüne JAR ersetzt keine Laufzeit- und Server-Abnahmetests.

## v0.3.0 additions – required on client and dedicated server
- Minecraft 1.21.1, NeoForge 21.1.249, Rogler TeamWars 0.3.0 (replace 0.2.2).
- FTB Teams 2101.1.11, compatible FTB Library (user 2101.1.37), **FTB Chunks 2101.1.22 now REQUIRED**.
- All dependencies present in latest uploaded DEV client `mods.csv` (2026-10-12); server profile not yet verified.
- No NEW download needed on this DEV client; FTB Chunks changed from optional to a required dependency in v0.3.0.
- Xaero, Chunky Friends, Simple Voice Chat, Sable, Sophisticated upgrades remain OPTIONAL for this code; integration not yet present.

IMPORTANT: uploaded FTB config `def_claim_visibility="public"`. CoreGuard works with auto_ftb_claims=false; change FTB visibility to private before enabling FTB auto claims.

## v0.3.1
No new runtime dependency relative to v0.3.0. FTB Teams 2101.1.11, FTB Library 2101.1.37, FTB Chunks 2101.1.22, NeoForge 21.1.249 and MC 1.21.1 remain required. FTB Ranks permissions are OPTIONAL if operators choose to restrict player-initiated disband/leave actions.

## v0.4.0
No new mods vs v0.3.1: NeoForge 21.1.249, Minecraft 1.21.1, FTB Teams 2101.1.11, FTB Library 2101.1.37, FTB Chunks 2101.1.22, Rogler TeamWars 0.4.0. Replace the previous TeamWars JAR; client and server must agree. Xaero, Tree Physics, Create, Sable and other mods are not required by the new code, but must be regression-tested when installed.
