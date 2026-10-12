# Core modular integrations – initial design/audit

## Requirements and desired strengths

The user wanted early-accessible, configurable **core services**, not a replacement for late-game tech: initial **9 inventory slots**, eventual Sophisticated Core upgrade-handler integration; useful but modest **20 FE/t up to 80 FE/t**; minor Create output, illustrative **256 SU @16 RPM**, later 512 or 1024 SU considered; and AE2 terminal + ME Drive/network connections. User wants to avoid free unlimited infrastructure.

## Implemented now

v0.5.1 gives a vanilla menu + persistent 9-slot store and native FE capability with limited FE/t and finite buffer, no chunkloading. AE2 Energy Acceptor can in principle receive this FE (2 FE=1 AE according to https://guide.appliedenergistics.org/1.21/items-blocks-machines/energy_acceptor/); actual compatibility test pending. No third-party dependency.

## Create kinetic plan (not yet implemented)

1. Confirm installed Create 1.21.1 NeoForge build and whether Sable/Create Aeronautics affects kinetic registration.
2. Create separate optional `CreateKineticAdapter`/`GeneratingKineticBlockEntity` so missing Create cannot classload missing classes in TeamWars core.
3. Integrate native axle direction and proper RPM/sign, 16 RPM & 256 SU baseline, second limited tier 512 SU, config max 1024 SU. No way to multiply output by repeatedly placing temporary cores or cloning capability objects.
4. Tie generation strictly to ledger PLACED + physical block; disable kinetic when carried, in ESCROW, destroyed, in another dimension or outside permitted zone. Invalidate on move, restart, team deletion and raid end.
5. Test client visuals, contraption conversion, stress explosion/over-speed, performance; zero automatic chunkloading.

## AE2 ME network plan (not yet implemented)

- FE Power compatibility is **not** a true AE2 grid node. ME Terminal/Drive requires AE2 API (`IGridNode`, `IGridHost`, channel power accounting, multiblock lifecycle), exact AE2 19.x ABI and often a registered block entity.
- Dedicated AE2 upgrade slot must be explicit and optionally loadable, not a passive automatically free ME Drive. Choose reasonable capacity and crafting cost. Ensure team-only terminal authorization even on remote client and after team membership changes.
- **Do not** expose the nine private core slots as generic `Capabilities.ItemHandler.BLOCK` now: callbacks expose only level,pos,side; they cannot prove the identity of the requester. A hostile neighbor's import bus could otherwise read the core. Plan an authenticated bridge bound to team property and trusted connector blocks, and test cache invalidation/revocation on pack and party change.

## Sophisticated Core handler plan (not yet implemented)

- Inspect exact `sophisticatedcore` and installed `sophisticatedbackpacks` 1.21.1 NeoForge API and licensing for `StorageWrapper`/`UpgradeHandler`. Support chosen functional upgrades explicitly, not blanket compatibility. Prefer 9 -> 18 -> 27 slots with paid tiers, but confirm balance/UI before implementation; **these tier sizes are an engineering proposal, not an existing user decision**.
- Upgrades and all item stacks must serialize in team-owned server SavedData. For duplicate/recovery safety do not store full contents in the movable core item NBT. No generic auto-extract with arbitrary mod requests.

## Verification gates

Compile in GitHub Actions, integrate with user's actual NeoForge modpack, adversarial two-client/hopper/AE2 import bus, raid recovery/FTB party deletion, world restarts, and measurable MSPT. Until then only FE and simple 9-slot inventory are code candidates.
