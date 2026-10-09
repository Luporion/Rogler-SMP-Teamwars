# Exploration data and renderer contract

## Requirements (acceptance-critical)
- One UI stack: Xaero Minimap (corner overlay) + Xaero World Map (full screen). Neither FTB's minimap nor a second full map is a player-facing solution.
- Provenance belongs to individual discoverer UUID and dimension; team vision is calculated from current roster.
- Leave = remove ONLY exclusively contributed map areas from old team. Player retains all own discoveries and receives new team's permitted areas.
- Existing personally explored Xaero data MUST NEVER be removed when revoking team data.
- Map tiles can be seen in BOTH Xaero views, not as a disconnected custom screen.
- Team core (own team only), shared waypoints, spawn distance, spawn inner/outer circles, carrier's 3D world border warning.

## Server-side core API (already implemented)
```java
boolean ExplorationIndex.discover(UUID, ChunkKey)
Set<ChunkKey> ExplorationIndex.personal(UUID)
Set<ChunkKey> ExplorationIndex.visibleTo(Set<UUID> currentMembers)
VisibilityDelta ExplorationIndex.delta(Set<UUID> before, Set<UUID> after)
```

## Adapter SPI proposed (NOT implemented)
```
ClientMapProvider:
  onJoin(serverIdentity, currentMembership, sessionGeneration)
  applyPersonalTiles(dimension, tileBatch)
  applyTeamTiles(dimension, tileBatch)
  revokeTeamTiles(dimension, regionSet, sessionGeneration)
  redrawMinimapAndWorldMap()
  purgePriorSessionTeamData()
```

### Critical unknowns: P0 XAERO SPIKE
1. How to read/serialize rendered Xaero tile data (not just chunk coverage) for current exact trio WM1.45.0 MM26.4.2 XP2.36.0.
2. How to overlay/supply team tile imagery without writing permanent personal cache.
3. How to revoke tiles from both UI surfaces even after logout/relogin, minimap cache and map dimension switch.
4. How to deal with XaeroPlus, `sable_xaero_bridge`, `sablexaeromaps`, and `XaeroHeadTracker` without incompatibility.
5. Whether mandatory client-side mod is required (likely yes). No claiming success until tested on two real clients.
6. Tile data vs chunk coverage granularity: chunk coverage is coarse; exact shading/textures must preserve local existing map pixels where possible.

## Proof scenario (two clients, one test server)
- A explores unvisited area `U1`; B explores `U2`; A+B join same team.
- Both Xaero map U1 ∪ U2, including **terrain textures**, same in minimap and full world map.
- A leaves, B loses U1 in BOTH UIs, while U2 and overlap remain.
- B relogs; U1 STILL absent. A's own U1 remains. Re-joining restores U1.
- Existing personally seen pixels unaffected; other dimensions isolated; negative chunk coords correct.
- Disable/re-enable adapter; no permanent corruption to standard Xaero maps.

## Legal / trust model
Xaero's World Map appears under All Rights Reserved distribution; don't ship its binaries/decompiled source. We can study public APIs, independently implement adapters or talk to its author. Can't cryptographically erase screenshots/local copies; client erasure is best-effort UI/cache management, server permissions are enforceable.
