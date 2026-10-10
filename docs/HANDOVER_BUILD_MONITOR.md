# Handover — append-only GitHub Actions Build-Monitor (2026-10-10)

**Branch base inspected:** `feat/v0.2.0-base-core` at `27adcf7b4795b2af00c73cbeb57bec67b8f7c372`. This is development tooling only; no additional Minecraft mods or new Java dependencies.

**Files:** `scripts/watch-actions.ps1` and `docs/plans/2026-10-10-actions-build-monitor.md`; README, PROJECT_STATE and .gitignore updated with usage and ignored downloads.

**Implemented:** Determine HEAD SHA and branch from local repo, match `push` workflows by both values, append-only status changes, fail-fast with failed-step logs, successful-run artifact selection, isolated artifact download, SHA-256 manifest, idempotent reuse, no overwrite, and CLI-only operation.

**Not verified yet:** Execution in Windows PowerShell 5.1/PowerShell 7 with `gh` authenticated, workflow status transitions, real artifact download, and actual Minecraft runtime. No changes to CI or the Minecraft mod sources. See test matrix in the plan.

**User smoke test:** Apply patch -> commit and push -> `.\scripts\watch-actions.ps1` -> expect both workflows success -> `artifacts/run-<id>-<sha8>/` with JAR and manifest. Repeat and verify it skips the download. A separate test with a failing commit should display logs without any download. Preserve existing terminal scrollback throughout.

**Rollback:** `git revert` only the commit that adds the monitor. Ignored `artifacts/` can be cleaned locally when no longer needed. No game data migration.
