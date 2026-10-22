# Rogler Core v0.7.3 – Xaero Cave Mode, FTB Team Map Privacy, Waypoint Bridge (test candidate)

## Important version boundary

This is a SOURCE test candidate for Minecraft 1.21.1 / NeoForge 21.1.249.
Java rule tests pass; GitHub Actions NeoForge compile and a 2-team multiplayer run **MUST** pass before deploying to the live server. Requires both server and client JAR update.

## v0.7.2 FTB leak: confirmed gap

v0.7.2 filters only the custom Rogler TeamIntel packets. It does **not** control FTB Chunks' own visible-player lists. In FTB Chunks 1.21.1 `PlayerVisibilityPacket.syncToPlayers()` permits bypass if the viewer is OP or the target team's Location Visibility is public. Consequently enemy player heads can be displayed in the FTB large map. The default `def_player_visibility` does not retroactively fix existing teams.

## v0.7.3 changes

1. **Server-side FTB Chunks list interception**. Target: `dev.ftb.mods.ftbchunks.net.PlayerVisibilityPacket.syncToPlayers(List)` on 1.21.1. The original is cancelled and an individually filtered packet is sent to each viewer; OP bypass and public/ally settings no longer grant enemy visibility. Policy is based on current, independently read FTB effective-party UUID + reciprocal membership. On API/reflection failure no FTB visibility packet is sent. The Mixin is required against the pinned FTB target. Use the test world; do not deploy unknown FTB versions blindly.
2. **Client-side extra filter** intercepts `VisibleClientPlayers.isPlayerVisible(Player)` and suppresses all foreign-team markers using only fresh server-authorized Rogler TeamIntel state. No teammate coordinates are written to disk. Before the first TeamIntel packet arrives, only the viewer is visible.
3. **Xaero Cave Mode**: removed the v0.7.0–v0.7.2 forced Fair-Play chat marker; it disabled cave mode as collateral damage. Server profile templates now explicitly include `minimap_cave_mode_allowed = true` and `cave_mode_allowed = true`, keeping entity radar OFF and disabling tracked players. Copy the templates to the actual dedicated-server `config/xaero/` paths and verify that the Xaero server-side mods enforce the profiles. Profile changes can require reconnect/restart.
4. **FTB -> Xaero waypoint mirror (experimental)**: on the client, reads the current FTB Chunks waypoint manager (documented API) and tries to put non-death, visible waypoints into Xaero's published namespaced custom-waypoint Collection. No personal or world map tile files are rewritten. Private FTB waypoints never leave the client through Rogler Core. If the installed Xaero version does not expose the expected API or its table is not a Collection, the mirror disables itself and logs `WAYPOINT_BRIDGE`, without crashing the game. It refreshes every 3 seconds, and removes only its own transient entries on disconnect. It is a read-only mirror, *not* a full waypoint sync.

## Still not implemented: full bidirectional waypoint synchronization

- Xaero's own private manually created waypoints are not automatically imported into FTB Chunks.
- In the currently implemented FTB -> Xaero mirror, only waypoints visible in FTB's local waypoint manager are eligible; FTB's in-GUI waypoint sharing remains the way to deliberately share a waypoint with party members.
- Share/unshare and changes to private Xaero waypoints are **not** handled by Rogler Core itself. No new full CRUD/GUI/permission mechanism is asserted as complete.
- TeamPins or Server Waypoint can be evaluated as third-party waypoint ownership systems, but **do not** install global waypoint-sharing mods without first verifying strict party-only visibility.

A future v0.7.4 can integrate exact 1.21.1 Xaero Minimap/World Map and FTB Chunks client APIs after testing the real JARs and build to safely import Xaero-origin waypoints, avoid duplicates and support explicit per-waypoint private/team/public state.

## FTB Chunks privacy settings (server + existing teams)

Use `world/serverconfig/ftbchunks-world.snbt` (world-specific override):

```
location_mode_override: false
long_range_tracker_interval: 0
```

Inside `team_prop_defaults`:

```
def_player_visibility: "private"
def_claim_visibility: "private"
```

**Important:** `long_range_tracker_interval = 0` turns off FTB long-range tracking to avoid a second independent coordinate feed that the PlayerVisibilityPacket filter does not control. With this disabled, FTB may not show team members at arbitrary distances; Rogler's own TeamIntel HUD still shows verified teammates. This is a deliberate privacy/performance trade-off.

Existing teams must also set **Location Visibility = Private** and **Claim Visibility = Private** in FTB Teams' settings. Server operators with permission to see hidden information are restricted by the v0.7.3 FTB map marker Mixin, but other admin tools and mods may still show positions; do not mistake this for general server anti-cheat.

### Waypoints: initial usable workflow

- Open FTB Chunks waypoint manager. Create a waypoint as usual.
- Decide **whether to share** using FTB Chunks' built-in Share menu: Party / specific players / server (disable server-wide sharing in the FTB config if undesired).
- On a client with the supported Xaero custom waypoint API, the bridge will mirror that player's locally visible FTB waypoints into Xaero without changing ownership or disclosure.
- Normal, manually created Xaero waypoints stay private unless explicitly shared through supported Xaero tools. They do not automatically appear in FTB Chunks yet.

## Mandatory test matrix

1. Dedicated NeoForge build and start with exact installed 1.21.1 FTB/Xaero versions. Check mixin warnings or errors. If Mixin target is incompatible, roll back to v0.7.2, do not force-start on production.
2. Two FTB parties with at least two members and one OP on each side: both FTB map and Xaero show no enemy icons, including in same chunks, after relog and a team switch. Test no other mods expose markers.
3. Xaero's Minimap **and** World Map Cave Mode work; both still have entity radar disabled.
4. FTB waypoint created/edited/deleted, shared with party vs private: assess whether mirrored and removed correctly without modifying existing Xaero waypoints. Test dimension and server switches.
5. Verify no repeated `WAYPOINT_BRIDGE` error spam, memory leaks, or core/claim regressions.

## Notes

No FTB team, claim or world files are migrated or rewritten by v0.7.3. No extra special item metadata/region/entity writes. Back up the server and client Xaero waypoint folders prior to new mod rollout nevertheless.
