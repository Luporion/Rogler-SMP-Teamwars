# Rogler Core project state — v0.6.2 candidate

- Date: 2026-10-15. Based on v0.6.1 hotfix `48433f4`.
- Verified: 491 pure Java assertions locally; patch application checked on a clean offline copy with GitHub base-file hashes; actual dedicated server/NeoForge compiled JAR **PENDING**.
- Explosion protection now snapshots a primitive event-local spatial index; no FTB/world requests inside affected-block filtering. One tick = no persistent permissions cache.
- Server tick integrity: five seconds air observation, two auto-restores in rolling two-minute window, ESCROW after third missing; immediate ESCROW on occupied location; no force load. `coreaudit` is read-only, `corerepair apply` explicitly runs pass. Existing tick reconciliation remains automatic.
- Stale FTB receipt clears if missing/foreign owner, does not unclaim foreign party. Uncertain same-team manual reclaims still need provenance-safe redesign.
- v0.6.1 optional Warium rescue is unchanged, **not proof Warium heavy explosions are safe**.
- Next: CI -> throwaway dedicated server -> measured A/B heavy nuke/claims test -> consider v0.6.3 if mismatch. Xaero and Sable mobile base deferred.
