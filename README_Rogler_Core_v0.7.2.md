# Rogler Core v0.7.2 — Team Intel HUD (SOURCE CANDIDATE)

**Base:** v0.7.1 (branch `feat/v0.7.1-update`, CI success observed for commit `e2fddd57`). Target Minecraft 1.21.1, NeoForge 21.1.249, Java 21.

## Included

- A private, **server-to-one-viewer** NeoForge payload of online team member positions. It is assembled from the live *effective* FTB Teams membership of BOTH viewer and each candidate teammate, not from mutable display names, public scoreboard teams or Xaero radar entities.
- Every two seconds the client receives the current authorized list. No enemy players are sent. Removed team members are removed on the next update. Missing/inconsistent FTB identity means an empty list.
- A small bottom-left companion HUD next to Xaero's existing minimap, showing 0–3 teammates and their horizontal distance/world-cardinal direction, and Overworld spawn-zone distance. The HUD is NOT a second map.
- Per-login in-memory client state with monotonic packet sequence, expiry and clearing on login/logout. Coordinates are never persisted in new files.
- Existing Xaero fair-play radar disabling from v0.7.0 stays enabled, along with v0.7.1 personalized exploration provenance and membership session tests; v0.6.6 oversized-party permission fix stays incorporated.
- No new hard third-party mod dependency or data schema migration. Clients and dedicated server **must both install the same v0.7.2 JAR** due to a new registered payload.

## Important NOT YET DONE

- **No native Xaero minimap/world map teammate icons yet.** The small companion HUD is a real in-game overlay, but is not injected into Xaero's render pipeline.
- **No Xaero terrain upload/download or tile rendering/revocation yet.** `TeamMapSession` still computes and guards revocation in server data only. Client Xaero map caches are not changed. Do not deploy globally-shared Xaero map addons on the team server.
- **No direct bidirectional FTB/Xaero native waypoint file synchronization.** TeamPins remains a separate candidate for explicitly shared team pins, not a proven general FTB<->Xaero waypoint migration.
- No native chunk-/claim-boundary circle on Xaero, no mobile Sable base-border logic in this release.

## How to test

1. GitHub Actions NeoForge compile; install identical JAR on the staging dedicated server and TWO 1.21.1 clients. Verify both sides register payload type `roglerteamwars:team_intel` without exception.
2. Create two opposing FTB parties with two players in the same party; verify each client sees only team members on the bottom-left HUD. Test negative X/Z, overworld/Nether, join/leave, switching teams and reconnects.
3. Ensure old teammate disappears within the next 2-second update; a network outage longer than 10 seconds removes stale markers; logout clears all markers.
4. Verify Xaero's normal player/entity radar is disabled on clients and that neither TeamPins nor other map mods reintroduce opponent markers.
5. Test `/rogler map status`, `/rogler map session`, `/rogler map zone`, core ownership, placement, mining and FTB claims. Confirm no Core SavedData or claims were altered.
6. Record client/server logs and a 2-party test video or screenshots; CI SUCCESS alone is not runtime verification.

## Build and rollback

Apply `Rogler-Core-v0.7.2-update.patch` on `feat/v0.7.1-update` with `PATCH.cmd`, wait for P0 Core + NeoForge Build workflows. This patch was built as a **Git-format** patch for `git am`. Roll back to verified `roglercore-0.7.1.jar` if the first test fails; no world schema change and no `/data` deletion is required. As always back up world/FTB/claims before deploying.
