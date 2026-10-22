# v0.5.4 maintainer / LLM handover

1. Read `AGENTS.md`, `docs/releases/v0.5.4.md`, `docs/archive/project-states/PROJECT_STATE_v0.5.4.md` and `docs/CORE_DASHBOARD_ARCHITECTURE.md`.
2. Base expected: actual GitHub branch `feat/v0.5.3-update`, SHA prefix **731c0bd1**. The patch modifies only blobs whose original SHA were checked against that commit. It does not overwrite historical README / PROJECT_STATE.
3. Migrate inventory dynamically using `CorePowerPolicy.SLOTS=27`. NEVER use hardcoded 9 in Java menu shift-click and saved-data loops. Unchanged SavedData schema and name; no item duplication in packed/ESCROW.
4. Create BE: client cannot query TeamWars party ledger. `TeamWarsDriveEnabled` sync tag is required to align tooltip & Create kinetic rendering with server. `tick()` only server and compares actual `getTheoreticalSpeed` against desired `getGeneratedSpeed` periodically, no work when matched. Do not run `updateGeneratedRotation()` every game tick.
5. Guide exchange: a real ordinary Minecraft BOOK is one-time input for each registered mod handbook. Both GUI and command use same server logic; no free creative bypass. No nonexistent books.
6. Existing CI release contract: exact tracked source ZIP generated using `git archive` by PATCH.cmd, Git-format patch, JAR only if actual NeoForge CI passes; world-side migration rollback warning as above.
7. Source ZIP reconstructed from prior v0.5.3 offline archive has historical limitations (monitor/handover placeholders). User's exact `full-source-exact.zip` after applying patch to real repo is canonical. Do not claim offline release ZIP is byte-identical for unchanged historical files.
8. Capture files are environment diagnostics, NOT included in the source repo and are not distributed in patches.
