# Rogler TeamWars v0.4.0 — Core-only raids & private automatic claims

**Status:** SOURCE CANDIDATE, tested offline with Java 21. A real NeoForge CI compile and Minecraft multiplayer verification are REQUIRED before deploying to a server. Latest base: v0.3.1, commit `a84e92a6c680dbda7215777db964c5ef834c46f4`.

- **Automatic FTB Claims** (`auto_ftb_claims=true` for newly created world configs): claims a 3×3 square on Core placement and reconciles after restart, reload and every minute; only its own claims are released after moving. Before a claim is added, TeamWars sets and verifies the team's FTB `Claim Visibility=private`; failures are fail-closed and reported, CoreGuard remains active. Existing world config values are preserved; set to true if your existing world still says false. Private claims are NOT invisible terrain; client-side verification against enemies is still mandatory.
- **Core-only raids:** Default `raids_enabled=false`. After manually setting `true` and OP `/teamwars admin reload`, an OP can `/teamwars admin raidstart <defenderPlayer> <minutes>` (1..30 minutes). One online defender required, grace must have expired. Surviving attackers must hit the real Core 10 times at least 750 ms apart with left click. First attacking FTB team is locked for the raid. On 10th hit, the block goes away; core state becomes recoverable ESCROW, attacking team receives exactly one persistent war point, defeated team gets fresh grace. Every other block stays guarded. Active raids prevent core pack; they end on timeout or when no defender remains online.
- Commands: `/teamwars war status`, `/teamwars war scores`, `/teamwars admin raidstart <player> <minutes>`, `/teamwars admin raidstop <player>`, existing `/teamwars base recover` and `/teamwars base protection`.
- **This is NOT a full siege system:** no standard TNT/Create/Sable raid damage, no automated raid scheduling, no general block editing during raids, no Xaero team map, no force-load quota. For dedicated server use, require two separate clients and an FTB claim privacy audit first.
- Docs: `docs/releases/v0.4.0.md`, `docs/plans/2026-10-12-v0.4.0-core-raid.md`, `docs/archive/handovers/LLM_HANDOVER_v0.4.0.md`, `AGENTS.md`, `docs/PROJECT_STATE.md`. Source ZIP includes all tracked project files for LLM handover.


See earlier historical state in the original `README.md` file.
