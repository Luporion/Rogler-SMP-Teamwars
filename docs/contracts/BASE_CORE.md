# Base Core contract (not yet a registered game block)

## Zone and HUD
- Only Overworld; core legal in annulus [minRadius, maxRadius] around world-spawn X/Z.
- P0 default min=300, max=2000 blocks, configurable.
- Display signed distance-to-border and radial distance-to-spawn, own-core waypoint on team map, ring overlays in Xaero.
- Client-side carrier-only 3D warning when within configurable threshold; world border is NOT modified.

## Carrying
- 3600 active-carrier seconds, persisted across relog/server restart/team switch without reset.
- At 0 and within annulus: attempt safe world placement, ACK only if real block can be placed.
- At 0 and outside annulus: return to last VALIDATION-VERIFIED safe placement or escrow with admin alert.
- Support container/death/logout; duplicate cores rejected by server identity registry.

## Ships
- Obtain world-space position from ship local-to-world transform.
- Out-of-zone cumulative 600 seconds; return inside reduces budget slowly (P0 half speed), preventing boundary bouncing exploit.
- Last minute alarm; at timeout server validates own-core identity and executes controlled self-destruction.
- Explosion must be capped, scoped, protect neutral spawn/other teams by default, log/audit and not create free attack points.
- Only after tested on disposable world and Sable 2.0.5 can mobile mode be enabled.

## Additional design
- Separate FTB tickets: manual+core quotas, count overlap once for actual chunk load.
- Primary chunk claims need explicit adapter to FTB rules and anti-bypass handlers.
- Built-in inventory starts 9 slots, future Sophisticated Core upgrades. No generic upgrade compatibility promise.
- FE, Create stress, AE2 interfaces optional plug-ins after MVP.
