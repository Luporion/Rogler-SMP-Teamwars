# Target modpack baseline from session diagnostic ZIP

Source snapshot: `Rogler Alpha DEV_Session-Analyse_2026-10-09_03-38-17(1).zip`; 524 enabled mods. No binary JARs redistributed.

| Component | File/version from snapshot |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.249 |
| Java | OpenJDK 21.0.12.1 |
| FTB Teams | `ftb-teams-neoforge-2101.1.11.jar` |
| FTB Chunks | `ftb-chunks-neoforge-2101.1.21.jar` |
| Xaero World Map | `xaeroworldmap-neoforge-1.21.1-1.45.0.jar` |
| Xaero Minimap | `xaerominimap-neoforge-1.21.1-26.4.2.jar` |
| XaeroPlus | `XaeroPlus-2.36.0+neoforge-1.21.1-WM1.45.0-MM26.4.2.jar` |
| Sable Xaero | `sable_xaero_bridge-0.1.11.jar`, `sablexaeromaps-1.21.1-1.4.0.jar` |
| Xaero Head Tracker | `XaeroHeadTracker-1.0.0-neoforge-1.21.1.jar` |
| Sable | `sable-neoforge-1.21.1-2.0.5.jar` |
| Create Aeronautics | `create-aeronautics-bundled-1.21.1-1.3.2.jar` |
| Simple Voice Chat | `voicechat-neoforge-1.21.1-2.6.22.jar` |
| Sophisticated Core | `sophisticatedcore-1.21.1-1.5.1.2341.jar` |
| Sophisticated Backpacks | `sophisticatedbackpacks-1.21.1-3.26.3.2158.jar` |
| Chunky | `Chunky-NeoForge-1.4.23.jar` |
| Chunky Friends | `ChunkyFriends-NeoForge-1.21.1-1.1.2+mc1.21.1-neoforge.4.jar` |
| AE2 | installed, mod list available in diagnostic |

Current FTB Chunks server settings from snapshot:
- `max_claimed_chunks: 500`
- `max_force_loaded_chunks: 25`
- `def_claim_visibility: "public"` **leaks claim location** unless restricted.
- `force_disable_minimap: false` (FTB's map setting; do not indiscriminately enable without verifying interaction)
- `def_allow_explosions: false` (raids would need deliberate controlled exception)

Current FTB Teams config `max_party_size: 0` (unlimited). Propose 4 after team count agreed and safe migration.

The session diagnostic also reported degraded JFR capture and 7 server-thread stalls/high-MSPT events. A dedicated server performance baseline is mandatory.
