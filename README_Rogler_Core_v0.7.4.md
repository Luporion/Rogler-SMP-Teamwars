# Rogler Core v0.7.4 – FTB Architectury login hotfix + Xaero 26.4.2 waypoint adapter

## Confirmed original failure (server 2026-10-20)

Server debug logs show repeated `EncoderException: Failed to encode packet clientbound/minecraft:custom_payload`, caused by `ftbchunks:player_visibility_packet` with `ClassCastException: PlayerVisibilityPacket cannot be cast to Architectury NetworkAggregator$BufCustomPacketPayload`.

v0.7.3's `FtbVisibilityPacketMixin` wrongly used `net.neoforged.neoforge.network.PacketDistributor.sendToPlayer` when sending a packet that **FTB Chunks registers and transmits through Architectury's NetworkManager**. FTB Chunks **2101.1.22** bytecode confirms `NetworkManager.sendToPlayer(ServerPlayer, CustomPacketPayload)` is the original transmission path. NeoForge's native sender bypasses Architectury's wrapper, so encoding fails and every joining player is disconnected.

## Changes

1. `FtbVisibilityPacketMixin`: replace NeoForge-native dispatcher with reflective invocation of exactly Architectury's `NetworkManager.sendToPlayer(ServerPlayer, CustomPacketPayload)`. The FTB per-recipient, current FTB-party privacy filter and cancellation-on-error remain. Reflection avoids adding a new hard Gradle compile dependency on the Architectury JAR; it is already a required FTB Chunks runtime dependency. If the method is unavailable or the operation fails, do **not** send an unfiltered visibility packet. This is a narrow, fail-closed hotfix.
2. `FtbXaeroWaypointMirror`: Xaero Minimap **26.4.2** bytecode shows `WaypointsManager.getCustomWaypoints(String)` returns `Hashtable<Integer, Waypoint>`, not `Collection`. Update adapter to use namespaced `Map<Integer, Object>` with own-key tracking and identity-checked cleanup. Other mods' and personal waypoints are never removed. The mirror remains **experimental, FTB → Xaero only**; this version does NOT implement Xaero → FTB or a new share permissions menu.
3. Version 0.7.4, a regression guard in `scripts/check-ftb-xaero-compat.py` automatically run by `scripts/test-core.sh`.

## Safety and privacy

- Keep the v0.7.3 FTB team-filtering Mixin. Never 'fix' this by allowing FTB's original OP/public bypass unrestricted.
- FTB Chunks config `long_range_tracker_interval: 0` must remain to disable its independent long-distance coordinate feed. Confirmed in the real installed JAR: LongRangePlayerTracker.tick() returns when interval is zero. Existing FTB teams need `Location Visibility = Private` and `Claim Visibility = Private`; new defaults are not retroactive. `location_mode_override: false` is not enough on its own.
- Xaero radar remains off via the server profiles; Cave Mode remains allowed. Private team exploration and Core SavedData are not rewritten.
- All clients and server should use identical Rogler Core v0.7.4; test with OP on a **throwaway or backed-up** DEV server.
- **CI passing does not prove live encoding or display**. Verify dedicated server login, client rendering, team membership change, FTB map, Xaero cave, FTB waypoint mirror, other mod compatibility.
- Do not delete/mod/reset team, claim, world, Xaero personal map or waypoint files. Rollback v0.7.2 if v0.7.4 still errors, with FTB player/claim visibility locked down separately. v0.7.2 does not have the FTB OP privacy filter.

## Local validation

- `bash scripts/test-core.sh`
- `python3 scripts/check-ftb-xaero-compat.py`
- On the user-supplied pinned JAR, `javap -cp ftb-chunks-neoforge-2101.1.22.jar -c -p dev.ftb.mods.ftbchunks.net.PlayerVisibilityPacket` demonstrates Architectury dispatch.
- On Xaero Minimap `26.4.2`, `javap -cp xaerominimap-neoforge-1.21.1-26.4.2.jar xaero.common.minimap.waypoints.WaypointsManager` shows `Hashtable<Integer, Waypoint> getCustomWaypoints(String)`.

## Next architecture work (not part of emergency hotfix)

- Use documented FTB Teams & FTB Chunks APIs for membership/waypoints, pin their exact versions and test with the user's real JARs.
- Avoid mixin changes to FTB's packet factories where possible. Consider an Architectury-compatible upstream integration or an event hook, but do not send unregistered FTB payloads via native NeoForge paths.
- To enable native two-way Xaero ↔ FTB waypoints with user-controlled private/team visibility, develop dedicated client-side API adapters and explicit server authorization. Never share all local Xaero waypoints automatically; revocation must clear only mod-owned transient markers. Test the actual Xaero 26.4.2/World Map 1.45.0 APIs first.
- For player/claim privacy, test OP, public, allied, nearby/distant tracking, relog, team transfer and other mods that reveal positions.
