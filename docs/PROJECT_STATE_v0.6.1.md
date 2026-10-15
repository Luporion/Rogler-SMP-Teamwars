# Rogler Core state v0.6.1 (source candidate)

## Verified in this release locally

- Exact parent source is v0.6.0, commit `a10c7d4e`. Modified baseline files checked against GitHub blob SHA.
- Java 21 pure-policy tests (including WariumRecoveryPolicy) pass; NeoForge real-dependency compilation awaits GitHub Actions.
- World/save/registry IDs unchanged. Visual name `Rogler Core`; old command retained as a full alias through Brigadier redirect.

## Not verified

- Actual Warium 1.0.3/1.0.4 matching entity class/registry ID and real disk-loaded event, on-server crash-loop recovery.
- Real NeoForge compilation, FTB claim/privacy multiplayer and protected base regressions with this changed JAR.
- Corrupt `r.-1.-3.mca` status remains unknown; no raw-file fix was attempted.

## Invariants

- Never disable Watchdog or manipulate production .mca/FTB files.
- OFF is default, bad config forces OFF.
- Optional mod absent never triggers classes of optional mod.
- Full-source ZIP plus format-patch per version. Exact on-user-machine zip from `PATCH.cmd` takes precedence over any offline reconstructed archive.

## Following release

v0.6.2: protected-chunk spatial index and performance counters, low-cost Warium explosion metrics, core/claim consistency verification after large explosions. Xaero team map and voice remain separate unimplemented milestones.
