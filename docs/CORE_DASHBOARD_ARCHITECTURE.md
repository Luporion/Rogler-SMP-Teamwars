# Core Dashboard architecture and long-term roadmap

## Player journey

1. Team creates its FTB party. A single signed Base Core is registered to a persistent party UUID. Team can place it in allowed Overworld zone.
2. Right-click opens the permanent **TeamWars Core dashboard**, replacing flood of guide chat messages. The Core is the central starting hub; no new game UI library dependency is needed.
3. Player sees live status: team's armor level, raid HP, raid active flag, Grace minutes, automatic FTB claims count and battery capacity. No enemy core coordinates are disclosed.
4. Player buys three real upgrades: armor (8 diamonds; next 16 diamonds+1 netherite), FE (16 redstone+8 iron; next 32 redstone+8 gold), Create starter engine (16 Create Andesite Alloy + 8 shafts). Server counts and consumes actual player inventory.
5. Player connects cable or AE2 Energy Acceptor on **any** side of the Core for FE. GUI displays stored FE, current FE/t, accepting FE neighbors and adjacent AE2 block count. FE extraction capacity is limited to 160 FE per call / per tick of automatic transfer. AE2 power is not an inbuilt ME storage node.
6. Create connector is the **actual horizontal rotational shaft** after purchasing upgrade: 16 RPM / 256 SU. Explain that the team should build its own waterwheel/steam generation early.
7. Guide panel surfaces loaded mod manuals when recognizable by registry names. Create and AE2 use their native in-game guides; do not issue fake items. No more six-line welcome spam.

## Clean module boundaries

- `core/` contains pure Java policies and tests; absolutely no Minecraft, Create, FTB or AE2 imports.
- `CoreInfrastructureSavedData`: persistent team inventory / battery / purchased create drive. Schema unchanged in v0.5.3.
- `CoreDashboardMenu`: authoritative inventory slot handling, clickMenuButton action dispatch and synchronized short-range read-only status. Reject remote use, unloaded cores, wrong party and unauthorized actions.
- `CoreDashboardScreen`: client-only layout with tabs. No server writes or client-declared upgrade state; reads only vanilla-synced data.
- `CoreUpgradeService`: remains the only actual currency and upgrade authority; CLI still supported, GUI calls same methods quietly.
- `CoreGuideService`: registry-aware book discovery; server validates grant.
- `CoreInfrastructureService`: existing FE transport and owner detection; UI reads FE-neighbor diagnostics, never auto loads chunks.

## Deferred module contracts — do not claim these shipped

| Module | Proposed GUI state and later contract | Blockers |
|---|---|---|
| Sophisticated upgrades | Module sockets, whitelisted upgrade items, server side item handlers and ownership | Inventory access isolation; handler migration; API version |
| AE2 ME Node / Drive | Small ME monitor and optional accessible ME storage, gated by team and paid upgrades | AE2 internal grid API, energy costs, ME channel limits, multiplayer security |
| Create extended power | 512 / 1024 SU tiers, visible coupling and actual Create network | Stress balancing, topology and Create event tests |
| FE expansion | FE output face selector, bank/buffer upgrades, per-side limits, redstone control | Server capability invalidation and load/transfer budget |
| Base defenses | Raid HP, Grace and active OP raid status from server | Other modded attack types and dedicated-server acceptance |
| Xaero map | Team exploration, revocation on leave, marker and ring display | Client map caches/privacy, 2-client packet tests |
| Radar | Bounded-range update/visible scan credits | No enemy coordinate leak; tick cap |
| Crops/plants | Low tick-budget vegetation growth bonuses | Ownership/grief prevention; no general tick acceleration |
| Chunkload | Dedicated quota distinct from manual FTB Chunks tickets | Strict budget, force-load release, save/reload audit |
| Team voice | Team radio beyond proximity, opponent proximity voice | Simple Voice Chat API/version |
| Team economy | Trading, item escrow, paid rename with money distributed to other teams | Consistency, anti-duplication, pricing |
| Events | CTF, sector control, convoys, drops, bosses | Server lifecycle, scheduling, anti-exploit |
| Sable mobile cores | Transform core into moving ship coordinates, transport countdown | Complex physics & SavedData safety, high impact |

## UI usability principles

- Default first screen is STATUS. Material costs and a Buy action appear together. Use text feedback in menu, not full chat dumps.
- Keep 9+36 inventory slots always visible; player can take resources or store items without closing the dashboard.
- Never show module as usable if implementation is missing. FUTURE page is explanatory only.
- User may always use the old commands. GUI is additive, not a save migration.
- Server authoritative for permissions, tokens, prices, FE balances and purchased tiers.
- Compatible with GUI scales and modded fonts; prioritize legibility, compact cards, minimal network payload and low CPU usage.
