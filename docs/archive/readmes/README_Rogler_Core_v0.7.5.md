# Rogler Core v0.7.5 – optional HUD, Respawn/Base navigation and Xaero markers

**Status:** Source candidate, not yet built against NeoForge in this environment. Based on v0.7.4.

## Player experience

- **HUD OFF by default**, with zero chat overlap. Set up via client command `/roglerhud` (no OP permission). Configuration is per client, saved under `config/roglercore-hud.properties` with a temporary file + atomic replace where supported.
- Toggle worldspawn, personal respawn point, placed team base core and teammate distances individually; respawn/base positions come from the server's authoritative own-player/own-FTB-team state. Different dimensions show an explicit unavailable indicator. The worldspawn is only shown in the Overworld.
- HUD anchor: `TOP_LEFT`, `TOP_RIGHT` (default), `BOTTOM_LEFT`, `BOTTOM_RIGHT`. Change X/Y margin in the GUI in 8px steps. No external HUD library or additional mod required. Ctrl/keyboard drag is **not** part of this release.
- **Xaero team markers ON by default**: online, server-authorized teammates (T) and own **placed** base core (B). Each is inserted as a temporary custom Xaero waypoint inside a dedicated `roglercore_team_navigation` table. No user-managed Xaero waypoints are edited. Logout, disconnect, dimension changes, and stale server snapshots clear markers. There is no enemy packet and no enemy marker.
- FTB→Xaero waypoint mirror now OFF by default because an arbitrary number of duplicated `F` markers blocked the minimap and world rendering. Can explicitly enable it in `/roglerhud`; this is only a local read-only mirror. **It does not provide Xaero→FTB or bidirectional waypoint editing/sharing.** Existing private FTB/Xaero waypoint files are untouched.

## Implementation / security

- Still uses FTB Teams 2101.1.11 as sole authoritative team membership, and the existing NeoForge-native team-intel snapshots. The team-intel payload now includes Y for accurate teammate markers. `TeamNavigationPayload` is an additional private, bounded two-second snapshot with personal respawn and team-owned base position. **All clients and server must run v0.7.5** due to network protocol update to `2`.
- The base is resolved from existing `BaseCoreSavedData` only when the viewer belongs to its current, independently verified FTB party. No new SavedData schema, world edits, or claim operations.
- Leave the `FtbVisibilityPacketMixin` Architectury-compatible, keeping the v0.7.4 login fix and server privacy filtering. Xaero radar stays disabled via the server profiles; Cave Mode remains available.
- v0.7.5 does **not** implement shared terrain tiles, revocable Xaero exploration display, Xaero<=>FTB bidirectional waypoint sync, a draggable HUD editor, or full minimap feature parity across incompatible Xaero versions.
- HUD UI uses NeoForge `RegisterGuiLayersEvent` + Vanilla `Screen`/`GuiGraphics`/`Button`. No extra UI dependency is needed for 4 lines of optional text. For future draggable dashboards evaluate third-party EasyGUI after verifying actual artifacts/license/dependencies; do not add a new hard dependency solely for distance text.

## Installation / verification

1. `git switch feat/v0.7.4-update`, `git status --short` (must be clean).
2. `./PATCH.cmd "M:\Downloads\Rogler-Core-v0.7.5-update.patch"` in Windows PowerShell.
3. Wait for **both** workflows (Core & NeoForge Build); then install the **new JAR on server and every client** (remove old v0.7.4 duplicates).
4. Before enabling on a real world, back up server and verify two-team privacy including OP, FTB map and long-range tracker. Keep FTB Chunks `long_range_tracker_interval: 0` and existing teams' location/claim visibility private.
5. `/roglerhud`: HUD OFF, worldspawn / own respawn / own base / teammate toggles, 4 anchor positions, offsets, team/base marker options, FTB F icon mirror OFF. Create a FTB waypoint and opt in to mirror if desired.
6. Two online party members should appear as T in Xaero (Minimap and, subject to Xaero 1.45.0 rendering, World Map); opposing players and enemy bases must not appear. Test OP and party switch. Disable special markers if any other mod renders them twice.
7. Move base via authorized pack/place: marker should disappear when packed and follow the new placed location. Reconnect, switch dimensions and disconnect while the markers are visible; verify cleanup.
8. No third-party mod JAR has been bundled. The existing versions of FTB Chunks, Xaero World Map/Minimap and XaeroPlus remain unchanged.

**Validation in this environment:** All standalone `bash scripts/test-core.sh` tests and static API shape checks pass; real NeoForge compile and the multiplayer acceptance test are still outstanding. The custom waypoint table API signature was checked by `javap` against uploaded Xaero Minimap 26.4.2; actual display in World Map 1.45.0 must be confirmed in game.
