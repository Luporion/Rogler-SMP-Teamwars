# Rogler Core v0.7.2 handover

Base: exact uploaded v0.7.1 full source, whose branch passed P0 and NeoForge CI 2026-10-19 (`e2fddd57`). Includes v0.6.6 team-size fix. TeamIntel design makes position leaks fail-closed and adds a client HUD but **does not yet hook Xaero's map renderer**. No terrain tile import into Xaero. The no-other-teams requirement is still primarily protected via Xaero's fair-play radar disabled and strict outgoing position filtering. Audit third-party radar mods and independent XaeroPlus tracking.

Build status: Java pure-rule test suite may pass offline; NeoForge compile and real-world tests must be performed by GitHub Actions and DEV two-party server. Do not infer working networking from Java-only tests. User's PATCH.cmd uses `git am` and expects a Git-format patch.

NEXT: version-pin Xaero's World Map/Minimap/XaeroPlus & their developer artifacts; build a client-side render integration (not a second map), isolate team textures in a TEMP namespace that is erased on roster change, test cross-team and logout revocation. Do not use a globally additive map-sync addon or re-use personal Xaero cache. For team waypoints test TeamPins in staging; no automatic local FTB/Xaero waypoint migration promised.
