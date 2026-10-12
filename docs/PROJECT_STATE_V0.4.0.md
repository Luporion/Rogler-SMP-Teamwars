# LATEST — v0.4.0 CORE-ONLY RAID SOURCE CANDIDATE (2026-10-12)

Based on GitHub v0.3.1 `a84e92a6c680dbda7215777db964c5ef834c46f4`. User confirmed v0.3.1 Creative and Tree Physics tests PASS; a full two-client lifecycle and FTB claim privacy test is not documented as passed. NeoForge v0.4.0 runtime remains UNTESTED.

- Default FTB auto claims TRUE **for new world configs**; existing properties preserved. Always force/verify FTB team `CLAIM_VISIBILITY=PRIVATE` before new claims; fail closed and keep CoreGuard when API fails. Auto claim on core placement and reconcile on startup/reload/60 seconds. No force-loaded chunks.
- Explicit OP core-only raids behind `raids_enabled=true`, online defender requirement, 10 HP, 750 ms server cooldown, attacker party locked at first hit, 1 saved point on victory; survivor core to ESCROW, defender gets new grace. Raid closes on timeout or defender offline. Does NOT grant attacker any ordinary block edit ability or make standard explosions destructive.
- New storage `overworld/data/roglerteamwars_raids.dat`. Existing `roglerteamwars_basecores.dat`, `roglerteamwars_protection.dat` formats preserved.
- Offline Java test suite extended with RaidPolicyTests. NeoForge CI compile and real in-game 2-party tests MUST occur before production.
- Git patch + full tracked-source ZIP including AGENTS/LLM/docs required every future release. `git gc.auto=0` / `maintenance.auto=false` for current Windows clone due object directory locking; root cause remains unconfirmed.

**Next:** compile CI, 2-client private-claim visibility test, manual raid active/expired/offline/persistence tests, then separate v0.4.x beta before enabling raids in production.


See earlier historical state in the original `docs/PROJECT_STATE.md` file.
