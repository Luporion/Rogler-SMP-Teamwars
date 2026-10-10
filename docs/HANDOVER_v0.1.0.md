# GPT-6 / Codex Handover — v0.1.0

- **Branch:** `feat/v0.1.0-ftb-teams` (source package based on original P0 `7041003`).
- **Version:** `0.1.0` in NeoForge Gradle metadata and `neoforge.mods.toml`.
- **Scope:** read-only FTB Teams 2101.1.11 reflection adapter, scoreboard teammate-only name tags, online sync, config, commands, Java 21 tests, GitHub Actions build workflow.
- **Verified:** `bash scripts/test-core.sh`: P0 41, new TeamAssignment 15 team checks + 8 settings checks, no regressions.
- **Not verified:** NeoForge compile/JAR/runtime, reflection signatures, 2-client gameplay, full modpack, FTB max-party count and voice routing.
- **Next MUST:** Push branch; inspect GitHub Actions NeoForge Compile run; fix compilation in same branch; run `/teamwars status` on empty NeoForge staging, then two clients. Log evidence to `docs/PROJECT_STATE.md`.
- **DO NOT:** Claim stable v0.1 JAR, tag a stable release, migrate production world or deploy experimental features before build + runtime acceptance.
- **Relevant docs:** `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/releases/v0.1.0.md`, `docs/plans/2026-10-10-to-12-MVP.md`.
