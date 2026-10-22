# v0.5.3 LLM handover

Base: `feat/v0.5.2-update` commit `367ccdc2` (hotfixed, CI successful, brief user runtime verified).
Read: `AGENTS.md`, `docs/PROJECT_STATE.md`, `docs/releases/v0.5.3.md`, `docs/CORE_DASHBOARD_ARCHITECTURE.md`, `docs/archive/test-plans/TEST_PLAN_v0.5.3.md`.

**Change:** real, server-synced Base Core dashboard; still has original persistent 9 slots, FE, Create. No AE2 ME-Drive node, no Sophisticated handler and no new engine output added. FE adapter remains adjacent FE block capability, Create engine real from v0.5.2.

**Security:** actions via vanilla `clickMenuButton` only. All authorization performed on server with `CoreInventory.stillValid` plus pure `CoreDashboardPolicy` plus preexisting `CoreUpgradeService` cost/raid checks. Never migrate team inventory into block entity or duplicate a core token. Do not let client provide price, team, coordinates or item counts.

**Test evidence:** local `bash scripts/test-core.sh`: 252 assertions pass (229 v0.5.2 + 23 dashboard). NeoForge compile remains **UNVERIFIED** until GitHub Actions on resulting patch. All new GUI classes must be compiled by Gradle in actual target version, do not infer compatibility from core policy checks.

**Delivery:** Git-format patch against SHA above + full source ZIP including LLM docs. `PATCH.cmd` auto builds exact full Git source ZIP from user's local repo. README and hotfix changelog preserved.
