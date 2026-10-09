# Test matrix / release gates

| Case | P0 core | NeoForge integration | Staging server | Required by Monday |
|---|---|---|---|---|
| Unique stable team UUID / rename | PASS | TODO | TODO | yes (team at least) |
| Join/leave; personal vs team exploration | PASS | TODO | TODO | membership yes; shared map optional if unproven |
| Overlapping exploration stays when A leaves | PASS | TODO | TODO | conditional on shared map feature |
| Xaero minimap full terrain team union | N/A | TODO | TODO | no if unsafe to implement |
| Xaero world map terrain team union | N/A | TODO | TODO | no if unsafe to implement |
| Team-only Xaero tiles erased on leave+relog | N/A | TODO | TODO | no if unsafe to implement |
| Per-dimension/negative coords | PASS | TODO | TODO | yes for core |
| 300/2000 annulus inclusive | PASS | TODO | TODO | yes |
| One-hour carry auto-place / last in-zone | PASS (intent) | TODO | TODO | yes for custom core launch |
| Illegal/non-solid/foreign claim placement | N/A | TODO | TODO | yes for custom core launch |
| Accumulating mobile over-limit countdown | PASS | TODO | TODO | no; feature flagged off |
| FTB actual claim protection | N/A | TODO | TODO | yes |
| Dual FTB chunk ticket classes | PASS (accounting) | TODO | TODO | no; use vanilla FTB quotas initially |
| Snapshot CRC + atomic write | PASS | TODO | TODO | yes for any production data |
| Base destroyed -> point -> grace -> replacement | N/A | TODO | TODO | yes, possibly manual fallback |
| Explosive weapons vs claim rules | N/A | TODO | TODO | no; default protect until verified |
| World backup and restart | N/A | TODO | TODO | yes |
| Chunk/pregen performance at 8–12 users | N/A | TODO | TODO | yes (stress smoke / limits) |

## P0 test invocation
`bash scripts/test-core.sh` or Windows `powershell -ExecutionPolicy Bypass -File scripts/test-core.ps1` — 41 checks passed on JDK 21 (2026-10-09).

## Acceptance evidence to collect
`latest.log` around commands, screenshots of both Xaero displays, FTB claim screenshots, snapshot after crash and restart, Spark TPS/MSPT comparison, exact mod versions and SHA256. Evidence must not leak server secrets.

## Regressions must be tested when...
Membership changes; Xaero map cache/schema changes; FTB claim settings change; ship is assembled/disassembled; Game rules change during operation; any migration updates persistent UUID mappings.
