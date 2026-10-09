# External integration research — non-bundled dependencies

- NeoForge 1.21.1 reference development kit: https://github.com/NeoForgeMDKs/MDK-1.21.1-NeoGradle
- FTB Teams 2101.1.11 release includes `FTBTeamsAPI.api().addPartyCreationValidator`, useful for team creation gating: https://www.curseforge.com/minecraft/mc-mods/ftb-teams-forge/files/8724782
- FTB Chunks 2101.1.21 already installed (from diagnostic), `def_claim_visibility: "public"` needs private policy and manual claim limits. API/version binding not yet tested.
- XaeroPlus MIT source: https://github.com/rfresh2/XaeroPlus
- Xaero Map Regions example of custom overlays: https://www.curseforge.com/minecraft/mc-mods/xaero-map-regions
- Chunky Friends 1.1.2 installed; there is no guarantee of a stable external plugin API. Investigate source or propose upstream hook rather than patching blindly.
- Simple Voice Chat API: https://modrepo.de/minecraft/voicechat/api ; `OPEN` groups might be adequate, verify enemy proximity and private channel behavior in real test.
- Sophisticated Core: examine storage wrapper + upgrade handler API against pinned 1.5.1.2341; no generic all-upgrades guarantee.

**Do not introduce TeamPins, Unified Worldmap or a second claims/map mod to the real pack before conflict tests.** TeamPins is ARR and separate, only an optional candidate.
