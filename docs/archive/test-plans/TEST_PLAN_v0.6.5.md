# Rogler Core v0.6.5 — verification

## Build & static checks
- [ ] Run `VERIFY.cmd` (Windows) or `bash scripts/test-core.sh` (Linux); all 558 policy checks and new Mixin package boundary check pass.
- [ ] GitHub Actions NeoForge compile succeeds on Java 21, NeoForge 21.1.249.
- [ ] Verify `SableRemovedSublevelGuard.class` occurs only under `compat/` rather than `compat/sable/` inside the compiled JAR.

## Isolated test server
- [ ] Back up world, serverconfig and mods/AutoModpack metadata. Never perform a nuke regression on production.
- [ ] Remove v0.6.3/v0.6.4 JAR from `mods`; install **only** verified `roglercore-0.6.5.jar`.
- [ ] Confirm normal startup: no `IllegalClassLoadError`, no missing class in Mixin package.
- [ ] Confirm `/rogler admin compat lifecycle` and `/rogler admin compat report` output.
- [ ] Confirm `sub_level_splitting=true` stays enabled, no required Sable: Destructive dependency.
- [ ] Restart twice with intact team claims and core inventory; re-run controlled ship splitting, record logs.
- [ ] Monitor Sable runtime errors and watchdog; the present fix addresses a startup class-loading issue only.

## Distribution
- [ ] Update AutoModpack only after a successful dedicated-server test.
- [ ] Clients receive exactly the same single Core version and no stale locked JAR.
