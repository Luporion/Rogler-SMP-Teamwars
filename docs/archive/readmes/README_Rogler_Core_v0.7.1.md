# Rogler Core v0.7.1 – membership-scoped map session foundations (TEST CANDIDATE)

**Base:** v0.7.0, including the v0.6.6 oversize-party permissions fix. Does not modify existing Core identities, ownership, claims or storage.

New: `TeamMapSession` computes per-viewer team union deltas on FTB membership changes; `ExplorationIndex.revision()` makes stable-session checks cheap; new `/rogler map session` tells the calling player about authorized chunk counts, last reveal/revoke delta and generation. On logout the session is removed. Real exploration is still stored per player in the existing atomic snapshot.

**Not completed:** Xaero's Minimap/World Map do not render imported teammate terrain in this build, and revoked chunks are not cleared from Xaero's local cache. Teammate position markers and automatic FTB<->Xaero local waypoint conversion are not included. The existing Xaero fair-play protection hides radar for *all* players. These require an independently tested compatible Xaero client integration. Do not install global-map-sync mods on a competitive secret-teams server.

To share team pins on Xaero today, independently test [TeamPins 2.0.0](https://www.curseforge.com/minecraft/mc-mods/teampins-ftb-teams-map-integration) on **both** server and client. It integrates team pins and FTB claims with Xaero but has not been shown to provide bidirectional synchronization of all native FTB and Xaero waypoints. Commands: `/teampins add <name>`, `/teampins list`, `/teampins sync`. Do not combine overlapping FTB->Xaero claim overlay add-ons without conflict testing. TeamPins may show allied player tracking and visible public claims: verify permissions, claim visibility and enemy-position secrecy with two actual teams before production.

Build requirements: Minecraft 1.21.1, NeoForge 21.1.249, Java 21. Java policy tests are NOT NeoForge build or Xaero runtime tests. See `docs/releases/v0.7.1.md` for acceptance and security tests.
