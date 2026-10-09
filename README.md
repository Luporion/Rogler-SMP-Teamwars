# Rogler TeamWars — P0 Foundation

**Status: P0 offline core tested; NeoForge scaffold not yet built or tested inside Minecraft. NOT A SERVER-READY MOD.**

Target: Minecraft **1.21.1**, **NeoForge 21.1.249**, ~8–12 players, 2–4 teams.
Target playable server start: **Monday, 2026-10-12 (Europe/Berlin)**.

## What is here

- `core/`: dependency-free Java 21 rules/data prototype, **41 passing checks** on 2026-10-09.
  - Stable UUID-based team identities and membership.
  - Per-player + per-dimension exploration provenance, union and revocation delta.
  - Versioned CRC-protected atomic exploration snapshots.
  - Safe X/Z radius math, carrying timer 3600s, mobile limit 600s.
  - Separate core/manual chunkloading quotas and overlap accounting.
- `mod-neoforge/`: **UNVERIFIED** NeoForge Gradle scaffold and `/teamwars status` skeleton. Not a completed adapter.
- `docs/`: implementation contracts, architecture, independent research, game design, release gates, sprint plan.
- `AGENTS.md`: binding directions for any follow-up GPT/LLM session.

## Test locally (Windows PowerShell, Java 21 installed)

```powershell
cd path\to\rogler-teamwars-P0
powershell -ExecutionPolicy Bypass -File .\scripts\test-core.ps1
```

Linux/macOS: `bash scripts/test-core.sh`. A Java demo jar can be created by `bash scripts/package-core.sh`; it is **not** a Minecraft mod and must **not** be copied into `mods/`.

## Build the NeoForge scaffold (requires an internet-connected development PC)

There is intentionally **no fake Gradle wrapper** checked in; this sandbox has no network to download Gradle and NeoForge dependencies. Starting point: official [NeoForge 1.21.1 NeoGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-NeoGradle).

1. Install JDK 21 and a compatible Gradle distribution, or obtain the official MDK's genuine `gradlew`, `gradlew.bat`, and `gradle/wrapper/` files (review before copying).
2. `cd mod-neoforge` and `gradle build` (or `./gradlew build` when the genuine wrapper is added).
3. If build succeeds, try the produced JAR **only in a disposable NeoForge 21.1.249 test profile**.
4. Verify `/teamwars status` reports `P0 scaffold loaded` (it intentionally performs no gameplay actions).
5. Record the real result and logs in `docs/PROJECT_STATE.md`.

**IMPORTANT:** The Java rules are tested; the NeoForge adapter is only an initial build skeleton. No Xaero, FTB, Sable or Sophisticated API integration is asserted to work.

## First things to read, in order

1. `AGENTS.md` — rules for other assistants and branches.
2. `docs/PROJECT_STATE.md` — what is actually done, blocked and next.
3. `docs/plans/2026-10-10-to-12-MVP.md` — critical path to Monday.
4. `docs/ARCHITECTURE.md` and `docs/contracts/EXPLORATION.md` — data and adapter boundaries.
5. `docs/TEST_MATRIX.md` — server-start gates.

## GitHub setup

This ZIP contains a ready-to-continue local Git repository with an initial commit (`main`). No remote has been configured, and no GitHub repo has been created by this artifact.

```powershell
cd path\to\rogler-teamwars-P0
git status
git remote add origin https://github.com/YOUR-ACCOUNT/rogler-teamwars.git
git push -u origin main
```

Create an **empty private repository** first (do not generate a README there). Or use an existing private repo as remote and merge appropriately.

**No third-party mod JARs or decompiled proprietary code are included.** Only filenames and versions from the submitted diagnostic snapshot are recorded.
