# P0 feasibility verdicts (2026-10-09)

| Requirement | Verdict | Evidence / next experiment |
|---|---|---|
| Independent rules implementation | **YES, tested** | Java 21; 41 local checks; scripts/test-core.sh |
| Per-player ownership and team-map revocation data logic | **YES, tested offline** | Player A leaves; A-exclusive chunks revoked, overlap remains; snapshot CRC |
| Xaero's own minimap + world map render team terrain & revoke cached data | **UNKNOWN (critical)** | Need tested client rendering seam with XaeroPlus + Sable bridges |
| NeoForge compile/run P0 mod | **UNKNOWN** | Offline environment lacks Gradle / NeoForge deps. Scaffold source exists, no usable Minecraft JAR |
| FTB Teams API data/event integration | **PLAUSIBLE, untested** | Official 2101.1.11 creation-validator method known; compile against exact pack |
| FTB Chunks separate force-load quota | **UNKNOWN** | Pure quota accounting tested; real FTB tickets/events remain API investigation |
| Mobile Sable core | **UNKNOWN** | Pure boundary timer implemented; need world transform/physics, logout behavior |
| Sophisticated Core inventory upgrades | **UNKNOWN** | Need concrete API compile and compatibility matrix per upgrade |
| Chunky Friends integration | **UNKNOWN** | Need source/API check, not pregen-as-exploration |
| Simple Voice Chat grouping | **PLAUSIBLE, untested** | API has voice groups; validate OPEN vs proximity and cheating |

## Explicit P0 conclusion
The data model is achievable; the highest-risk dependency is a **single native Xaero map that can revoke team-only terrain without damaging private personal map history**. This is NOT solved by creating a server-side exploration database. It also needs tile transfer/render/client cache isolation.

No modded server start is authorized based on the P0 core tests alone.
