# Setup and deployment — Windows / Crafty

## Local source checkout
1. Extract ZIP to a stable development folder; `cd` into the folder containing `.git` and `AGENTS.md`.
2. JDK 21: `java -version`.
3. Verify core: `powershell -ExecutionPolicy Bypass -File scripts/test-core.ps1`.
4. Inspect: `git log --oneline -3`, `git status --short`.
5. Create private empty GitHub remote, then `git remote add origin ...` and `git push -u origin main`.

## NeoForge scaffold build
- Use JDK 21. Obtain real official Gradle wrapper binaries from the official NeoForge MDK or install compatible Gradle; don't fabricate a wrapper jar.
- `cd mod-neoforge` ; `gradle build` ; capture output. A failed build must be documented, not hidden.
- If successful, install the resulting mod JAR *in a copy of the test profile*, never directly in the production world.
- Test `/teamwars status`; it only prints a scaffold message, **no teamwars gameplay implemented**.
- Confirm versions from `docs/MODPACK_BASELINE.md` and Minecraft logs.

## No-panic Monday fallback (no custom JAR)
- Back up entire test profile and world before any config changes.
- FTB Teams max party size 4 (if 3–4 teams of 3–4 players), set private claim visibility and appropriate claim quotas in **world/serverconfig overrides**, not just client config.
- Keep FTB Chunks map out of the player workflow; continue using existing Xaero Minimap + World Map.
- Use Simple Voice Chat + FTB Teams; manual team channels if API integration not ready.
- Admin controls grace/raids/score manually; prevent untested explosions in base claims.
- Never deploy the standalone `rogler-teamwars-core-P0.jar` into Minecraft `mods/`.

## Backups and rollback
- Stop server before copying/modifying live world or replacing core persistence.
- Keep at least two dated backups; test restore on a staging server.
- If custom mod crashes, roll back BOTH JAR + its versioned world data from matched backup; changing only the JAR may corrupt state.
