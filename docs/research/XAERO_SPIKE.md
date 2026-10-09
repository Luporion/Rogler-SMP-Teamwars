# P0 spike — revocable team exploration in Xaero

Status **UNKNOWN — high risk**, not proven in Minecraft as of 2026-10-09.

## Research sources (public URLs, check before implementing)
- Xaero World Map, ARR: https://modrinth.com/plugin/xaeros-world-map
- XaeroPlus source (MIT): https://github.com/rfresh2/XaeroPlus
- Unified Xaero Worldmap (global additive sync, not membership-aware revocation): https://www.curseforge.com/minecraft/mc-mods/unified-xaero-worldmap
- xaerotools (MIT, research on region/tile data): https://github.com/dekrom/xaerotools
- Xaero Map Regions (example client overlay/fog-aware regions): https://www.curseforge.com/minecraft/mc-mods/xaero-map-regions
- xaero-autosync_r (older Fabric source/method reference): https://github.com/Conflux-Union/xaero-autosync_r

## Confirmed boundary
Xaero's World Map keeps local rendered terrain imagery. Server authoritative `player entered chunk` only tracks **mask/provenance**; to show a teammate's truly undiscovered terrain, map **pixels/tiles** must be supplied by a player/renderer, or generated on demand by another lawful pathway. Neither vanilla chunks nor Chunky pregeneration automatically supplies an Xaero-compatible pixel tile.

## Spike experiments in order
1. Pin exact current triple: WM 1.45.0, MM 26.4.2, XaeroPlus 2.36.0; backup Xaero user map folder.
2. Inspect public XaeroPlus 1.21.1 source compatibility layer, local tile cache/swap entrypoints, client worldmap events, and possible APIs without copying ARR code.
3. Isolate rendering seam: inject a 1-chunk *temporary rendered tile* onto native full-screen Xaero map and minimap, independent of personal cache.
4. Remove it and reopen GUI + relog -> verify it is gone from BOTH views. Verify normal personal tile remains.
5. Test full A/B union and B losing only A-exclusive tiles.
6. Verify no cross-server/dimension leak; trim packet size and rate-limit uploads.

## Fast fail criterion
If by Saturday morning no safe dual-Xaero render+revoke seam is identified, do not spend the weekend building it. Keep native personal map for launch, and prioritize server-playable bases. Continue branch `spike/xaero-map` after release.

## Important caution
Xaero's main project publicly warns about XaeroPlus interoperability; the current modpack already uses XaeroPlus/Sable bridges. Do not upgrade only one component. Upgrading WM to 1.46.0 also requires matching Minimap, XaeroPlus, and bridge compatibility test.
