# Rogler Core v0.7.6 — Private Waypoint Import + Explicit Team Shares

**STATUS: SOURCE CANDIDATE; NOT YET COMPILED OR RUN IN MINECRAFT.** Base: v0.7.5. No new required mod. Full two-way edit/delete synchronization is not claimed.

## Gameplay & commands

- `/roglerhud` → **Wegpunkte: Synchronisieren / Teilen** opens the client-only GUI. Tabs: FTB, Xaero, Team. Click **Mit Team teilen** on exactly the chosen waypoint. The Team tab permits the original author to click **Zuruecknehmen**. No mass-share operation exists; every new waypoint is private until explicitly shared.
- `/roglerwaypoints list` lists personal FTB and Xaero waypoints (separate indexed sources), along with the team-only shared UUIDs.
- `/roglerwaypoints sync` performs an **explicit, current-dimension, local two-way import** of *missing* personal FTB↔Xaero waypoints. Existing personal points are not removed or overwritten. This is **create/import-only**. Changes/deletions after an import are **NOT** automatically mirrored. Xaero's personal list persistence after insertion needs client verification; take a backup of the Xaero waypoint directory before using sync in a real profile.
- `/roglerwaypoints share ftb <index>` and `/roglerwaypoints share xaero <index>` explicitly publish one selected, non-transient local waypoint to the current verified FTB party.
- `/roglerwaypoints unshare <UUID>` revokes one share **only by its creator**, using the IDs listed in `/roglerwaypoints list`.
- The older v0.7.5 `F`-letter-only FTB→Xaero mirror stays **OFF by default**. It is independent of this import flow; enabling both could create visual duplicates.

## Authoritative server privacy

- The client uploads **only** an explicitly selected waypoint. The server never accepts a client-supplied team ID: it queries FTB Teams 2101.1.11 using the requesting player's UUID for every write.
- SavedData `world/data/roglerteamwars_shared_waypoints.dat` contains **only** explicitly shared pins. Each is keyed by immutable ID, FTB team UUID and original sharer UUID. No personal Xaero data is stored on the server.
- When showing a share, the server checks **both** the receiving player's current FTB membership and the original sharer's current party. Opponents, nonmembers and former teammates get no team data. Public or OP FTB visibility never bypasses the filter.
- Server snapshots are bounded to 64 pins per team, max 48-character names and 128-character dimension IDs; rate-limited writes; changes are sent to each connected recipient independently. Same-user/same-position publishing updates the existing shared record. No background bulk import from clients.
- Client renders shares only in namespaced temporary Xaero waypoints and FTB transient waypoints, cleaning only exact object instances that the Rogler adapter inserted. No user-managed entries or personal Xaero map tiles are removed when membership changes. Both servers and clients must run 0.7.6 (protocol version `3`).
- v0.7.4's Architectury-compatible FTB player-visibility fix remains unchanged. **Keep FTB `long_range_tracker_interval: 0`, existing Team Location/Claim Visibility private, and Xaero radar off.**

## Limits not solved in 0.7.6

- **No background two-way edit/delete propagation**, automatic bidirectional conflict resolution or restoration of removed client files.
- **No TeamPins interop**, nor automatic import of other clients' private waypoints. Team shares are first-class Rogler Core server records, displayed as temporary markers.
- No visual native Xaero waypoint GUI context-menu item. Team shares are managed through `/roglerhud` → Wegpunkte or `/roglerwaypoints` commands.
- Xaero World Map 1.45.0 rendering of namespaced custom points and private waypoint save behavior require real client testing.
- No team-exploration terrain sharing yet. No blanket rewrite of Xaero waypoint caches or FTB NBT.

## Deployment / acceptance

1. Back up the whole world (including FTB teams/claims and Core SavedData) and each test player's Xaero personal waypoints. Test in DEV first.
2. `git switch feat/v0.7.5-update`; `git status --short` (clean); `./PATCH.cmd "M:\Downloads\Rogler-Core-v0.7.6-update.patch"`.
3. Wait for Core + NeoForge Actions. Install **the same newly built v0.7.6 jar** on server and every client, with no old Rogler Core jar left.
4. Join with two clients on Team A and one client on Team B, including an OP client. `/roglerwaypoints list` must not upload private points. Choose and share only one FTB waypoint; Team A receives it in both maps, Team B must see nothing. Restart server, leave/rejoin Team A and check revocation, also with remote/nearby players.
5. Use `/roglerwaypoints sync` on a throwaway client profile with original backups: create waypoint in Xaero, verify it appears in FTB; create one in FTB, verify it appears in Xaero. Repeat sync → no duplicates; check colors/names/dimensions and saving after client restart. Do not assume deletes propagate.
6. Test `/roglerwaypoints unshare <UUID>` and the UI revoke button. All clients' transient shared markers must disappear within a few seconds. Test restart persistence of SHARED state and absence of any restored private shares.
7. Test no encoder exception on login; FTB Chunks PlayerVisibilityPacket remains Architectury-sent; check OP privacy and Cave Mode; HUD stays off by default.

**No automatic world migration** for previous Core data, no new third-party binaries in the source archive. Rollback: stop server, back up `roglerteamwars_shared_waypoints.dat`, replace v0.7.6 jars with 0.7.5 on all clients/server. The new SavedData remains dormant until next 0.7.6+ run; do not delete it.
