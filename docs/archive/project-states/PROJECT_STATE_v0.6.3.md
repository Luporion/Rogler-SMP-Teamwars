# Rogler Core v0.6.3 snapshot — 2026-10-15

Source candidate for next GitHub CI, baseline v0.6.2 commit `1321a76a`.

- World compatibility: namespace `roglerteamwars`, original SavedData IDs and blocks unchanged.
- v0.6.2 protection: event-scoped Chunk index, bounded Core physical recovery, FTB own-auto-claims reconciliation, metrics.
- v0.6.1: opt-in Warium FusionBlast join cancellation OFF by default.
- v0.6.3: read-only Sable: Destructive readiness and split-config audit, optional event-only explosion observation, new admin commands. No new dependencies or mixing into Sable.
- Splitting stays enabled per user requirement; NEVER set `sub_level_splitting=false` to disguise problem.
- Milestone CI: pending; runtime dedicated-server tests: pending. Offline core policy tests: 536 passed.
- Later roadmap: Mianbao direct plot damage via verified Destructive API, Sable/Mahou AABB root cause, Sable Lifecycle confirmed tests, progressive Warium only if semantics proven, Xaero team exploration/voice after stabilization.
