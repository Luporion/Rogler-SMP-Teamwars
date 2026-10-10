> **Current milestone v0.2.0 SOURCE CANDIDATE:** first authoritative physical Base Core with issued team UUID token, overworld 300–2000 radius, 1-hour online carrier timer, safe vanilla placement attempt, fallback operator escrow, per-world SavedData, and player-only team diagnostics. NOT raid-ready (FTB claims/Grace/chunkload absent); real NeoForge v0.2 runtime is not yet tested. Dependencies: NeoForge 21.1.249, FTB Teams 2101.1.11 and FTB Library 2101.1.35; **no new third-party mods**. See [requirements](docs/MOD_REQUIREMENTS.md) and [test plan](docs/plans/2026-10-10-v0.2.0-base-core.md).

# Rogler SMP — TeamWars v0.1.0 (source candidate)

> **Not yet a verified Minecraft build.** The Java 21 rules are tested, but this container cannot download Gradle/NeoForge dependencies and a real NeoForge JAR/runtime pass **has not happened**. Do not install `core` JAR as a mod.

**Target:** Minecraft 1.21.1 / NeoForge 21.1.249 / FTB Teams 2101.1.11 / 2–4 teams / 8–12 players. Planned server opening: 2026-10-12.

## What is implemented in v0.1.0

- FTB Teams stays the **only membership authority**. Players create/join/leave teams with the existing FTB Teams GUI or `/ftbteams party ...`.
- Server-side **read-only FTB API adapter** (reflection to avoid bundling/copying third-party binaries).
- Vanilla scoreboard nametag privacy: current FTB-party teammates share one scoreboard team (`hideForOtherTeams`); players without a valid party have unique solo teams. Names can still leak through other mods (e.g. HUD/Jade/voice); test separately.
- `max_team_members` (default 4) safety guard in the mirror. **The mirror does not itself prevent oversized FTB parties**; configure FTB Teams' native `max_party_size = 4` too.
- `/teamwars`, `/teamwars status`, `/teamwars team`, `/teamwars team info`, `/teamwars team help`, `/teamwars admin resync`, `/teamwars admin reload`, `/teamwars admin diagnostics`.
- A per-world configuration file `serverconfig/roglerteamwars.properties` created on first start. Changes can be read with admin reload (without restart).
- Automated reconciliation on login and every 20 server ticks (configurable). Server-side-only scoreboard management; stable identifiers derived from FTB party UUIDs.
- Existing deterministic P0 rules for exploration provenance, base zones, timers, quotas are retained but **NOT connected to Minecraft**. 64 Java checks pass locally.

## NOT in v0.1.0

**No** Base Core block, base claims, transport timer gameplay, raids, score points, Voice Chat channel automation, shared Xaero terrain, map revocation, event engine, trades, Sophisticated upgrades, Chunky Friends trigger or Sable self-destruct. These are planned, not functioning. FTB max party creation count (4) and paid renames are not enforced yet. Do not allow uncontrolled party creation/renaming if competitive fairness depends on those rules.

## Build (first real compilation needed)

On a development machine with **JDK 21**, **Gradle 8.10.2** and internet:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-core.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\build-mod.ps1
```

Or push the `feat/v0.1.0-ftb-teams` branch to GitHub to trigger `.github/workflows/neoforge-build.yml` and download its JAR artifact if the build passes. Do not assume the workflow will pass until its actual run is checked. The official [NeoGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-NeoGradle) is the build reference.

When the build succeeds, test in a **disposable** dedicated server and two NeoForge clients with identical mod sets (including FTB Teams). Verify `/teamwars status`, `/teamwars team`, `admin resync`, nametag visibility from both parties, join/leave, reconnect, restart. **Do not use a production world for the first test.** Full acceptance checklist: `docs/releases/v0.1.0.md`.

## One-time configuration

- FTB Teams `/ftbteams serverconfig` -> set `max_party_size = 4` (built into FTB Teams 2101.1.11).
- Prevent players creating a fifth party via moderation until a native FTB party-creation validator is implemented.
- TeamWars world config: `serverconfig/roglerteamwars.properties`, default 4 members / 4 parties / 20 ticks / teammate-only nametags / friendly fire disabled by default.
- `/teamwars admin reload` after updating the world config.

## GitHub Actions Monitor (Windows)

After applying your patch, testing locally, committing and pushing your branch,
track the two required GitHub Actions and download **only** the matching build artifact:

```powershell
.\scripts\watch-actions.ps1
```

One-time requirements: Git and [GitHub CLI](https://cli.github.com/) (`winget install --id GitHub.cli -e`, then `gh auth login`). The monitor preserves the **full terminal scrollback**: status changes remain as timestamped lines, while a small spinner redraws **only its own temporary line** once per second. GitHub-style status colors: waiting **gray**, running **yellow**, success **green**, failure **red**. Running workflows show elapsed time from GitHub `startedAt` (or `createdAt` if unavailable). The GitHub API is still polled every 5 seconds; `-NoAnimation` disables the temporary line for redirected output or log capture. It verifies that both workflows passed for the exact HEAD SHA and branch, then downloads the JAR(s) to `artifacts/run-<run-id>-<sha8>/` (ignored by Git) and records SHA-256 hashes. A CI success is **not** a Minecraft runtime test.

Setup, parameters, troubleshooting, rollback and test cases: [build monitor plan](docs/plans/2026-10-10-actions-build-monitor.md).

## Multi-chat workflow

1. Read [`AGENTS.md`](AGENTS.md) and [`docs/PROJECT_STATE.md`](docs/PROJECT_STATE.md).
2. Use a feature branch and keep one bounded task per chat.
3. Real Minecraft compilation, testing and logs are required before a JAR can be called playable.
4. Update state and add `docs/HANDOVER_v0.1.0.md`-style evidence with branch, tests, risks, next action.

The repository is the source of truth. Development branches are not production releases.
