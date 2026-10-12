# CURRENT RELEASE LEDGER: v0.5.0 – read with docs/PROJECT_STATE.md

This supplemental status ledger preserves historical PROJECT_STATE.md unmodified so git-am applies on the actual GitHub baseline. The full truth of this milestone is documented in `docs/releases/v0.5.0.md` and `docs/HANDOVER_v0.5.0.md`.

**Current status:** pure Java 21 tests passed offline. NeoForge compile, FTB runtime API reflection and 2-client multiplayer behavior UNVERIFIED. No production raid release. Git patch based on commit `6d106ebd`.

**Features:** stable persistent TeamWars team UUID register + admin UUID target commands; FTB Chunks forced-all privacy preflight and audit; core armor tiers and materials; existing op-controlled core-only raid windows and one point per defeat retained.

**Known limitations:** FTB parties are still deleted when last member leaves; orphan adoption with FTB claims is blocked for safety. `PRIVATE` does not cryptographically hide claim coordinates from modified clients. Server source of truth is SavedData. No FE, Create, AE2 upgrade inventory or shared Xaero map.
