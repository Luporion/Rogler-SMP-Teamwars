# Rogler Core v0.6.5 — server startup hotfix

Fixes the confirmed `IllegalClassLoadError` in v0.6.4 by moving the normal
`SableRemovedSublevelGuard` class out of the reserved Mixin package and
updating its callers. Adds Mixin-package boundary checks for CI and Windows
`VERIFY.cmd`.

- Keeps `sub_level_splitting=true`; **Sable: Destructive is optional**, not required.
- Does not change world data, registry IDs, FTB claims, inventory, raid state, FE or Create.
- Java policy tests pass; NeoForge compile and real Sable/Shtreimel QA are still needed.
- See `docs/releases/v0.6.5.md` before replacing the deployed JAR.
