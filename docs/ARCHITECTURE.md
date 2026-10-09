# Architecture v0 (P0)

## Module layering (one-way dependencies)

```
                           SERVER (authoritative)
 +------------------------------------------------------------------+
 | mod-neoforge                                                       |
 |  CommandAdapter / PlayerExplorationTracker / FtbTeamsAdapter      |
 |  BaseBlockAdapter / FtbChunksAdapter / ShipAdapter / Storage       |
 +------------------------------|-----------------------------------+
                                v
 +------------------------------------------------------------------+
 | core (plain Java 21)                                               |
 | TeamDirectory   ExplorationIndex   BaseZone    CoreLifecycle       |
 | ChunkLoadingBudget          ExplorationStore                       |
 +------------------------------------------------------------------+
                  | request-specific snapshots / deltas
                  v
 +------------------------------------------------------------------+
 | client adapters: Xaero Minimap + Xaero World Map                   |
 | separate personal vs team texture/mask layers; wipe when revoked |
 +------------------------------------------------------------------+
```

No cross-dependency from core to Minecraft, Xaero, FTB or NeoForge. No client can assert a team membership, discovered terrain, core position or item transaction without server validation. Client renderer is an output port, not the authority.

## Key identities
- Player `UUID` (stable across display-name changes).
- Team `UUID` (stable across team rename; never key claims by display name).
- Core `UUID` unique per team (not yet implemented); physical block/item only carries reference to server record.
- Exploration `ChunkKey(dimension,x,z)`, owned by player UUID; overlapping discovery ownership retained.

## Exploration pipeline
1. Server validates authoritative player's current dimension and coordinates.
2. Upon actually visiting a chunk, `ExplorationIndex.discover(player, chunk)`.
3. Teammates' visibility is union(currentMembers). Personal exploration is **never copied** to other players.
4. Upon team departure: compute `delta(oldMembers, newMembers)` and send revocation to remaining members' client renderer. Also purge removed member's team-only cache.
5. World map client must obtain/retain/render terrain tile data only while permitted; boolean exploration mask alone cannot make unseen terrain appear.
6. On re-login, reconstruct permitted map from authoritative membership; never trust cached shared worldmap files.
7. Chunky pregeneration is independent: it creates terrain but never marks it discovered.

## Model caveats
- `CoreLifecycle` P0 is a *rule decision* engine only. Its `AUTO_PLACE` / `RETURN_TO_LAST_SAFE` is an intent, not proof a surface is safe. World adapter must locate a real legal block and call `placeStationary` AFTER successful placement.
- Time budgets are advanced by caller-provided elapsed seconds, currently clamped to 60 per tick call to avoid large shocks; online/offline policy belongs to adapter. Persist budgets with every transition and periodic checkpoints.
- `DETONATE` means controlled effect should execute after validation. The current P0 model is NOT a durable exactly-once detonation transaction; implement operation IDs and recovery before real world mutation.
- ExplorationStore P0 snapshots buffer entire data and are not asynchronous; future work needs server-thread snapshot/copy + background I/O or incremental region files. Bound network payloads.

## Versioned durable storage plan
World saved data owned by TeamWars under `world/data/roglerteamwars/` (or native SavedData path):
- `teams`: membership, roles, rename cost cooldown, stable team UUID
- `exploration`: per-player dimension/cell provenance, schema version
- `cores`: one record per team; state, timers, position, claims/tickets, inventory ID
- `scoring`: seasons, points, grace end timestamps and raid windows
- `market`: escrow ledger, transaction IDs, idempotent delivery
- `audit`: admin and money-moving operations

Never synchronously rewrite a huge map on every player movement. Persist changed cells with a safe journal or periodically snapshotted state. Always support backup/restore and migration.

## Performance envelope
12 players × frequent movement can revisit 10k+ chunks; map sync requires dedupe and view-distance rate limits. No whole-team union every server tick; recompute only affected memberships or new visited cells. Batch packets; dimension-aware client invalidation. Disallow unbounded pregeneration during play. No mass TNT chunk destruction.

## Live config contract
Authoritative server config model, versioned & validated; admin commands update values transactionally, with audit log. Vanilla/FTB UI may need an addon. Unsafe parameters (registry, block entities, datapacks) remain restart-gated.
