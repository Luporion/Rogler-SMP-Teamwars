# Current roadmap update — v0.7.6 (2026-10-21)

- v0.7.6 source: explicit private create-only FTB↔Xaero imports, team-scoped selected shares with author-only revoke and temporary markers. **Not yet compiled/tested in game.**
- Next gates: NeoForge build, 2-team/OP privacy tests, 2-client markers, Xaero persistence and network packet sanity.
- Next work after gates: conflict-safe edits/deletes across both private waypoint stores; version-pinned Xaero integration; then real revocable shared exploration tiles; team voice & independent core chunkloading afterwards.

---

# v0.7.5 source update (2026-10-20)

Implemented in source: per-client opt-in position HUD (worldspawn, own respawn, own placed base core, current teammates) configurable via `/roglerhud`; ephemeral team/base Xaero waypoints using a namespaced map; reduce duplicated FTB-to-Xaero marker noise by disabling mirror by default. Runtime/CI still pending. Next: pin exact Xaero World Map API and prove both displays and HUD menus in-game, then implement opt-in two-way waypoint sharing, and only then explore revocable Xaero terrain overlays.

---

# v0.7.2 update — 2026-10-19

Implemented in source: secure teammate-only network presence snapshots and client HUD, preserving 0.7.1 server map sessions. Next: (1) NeoForge CI + two-team privacy tests; (2) native Xaero Minimap/World Map render adapters version-pinned to modpack trio; (3) revocable, nonpersistent team-only terrain tiles in both Xaero maps; (4) team-only waypoints via tested TeamPins or separate integration. This revision is NOT completion of the Xaero terrain/map sync milestone.

# v0.5.3 UI architecture milestone

Native owner-only 5-tab Core dashboard implemented in source. Keeps FE generator, 16 RPM / 256 SU Create starter shaft, 9 team slots. 3 purchase actions via menu. Tech guide uses registered manuals plus Create Ponder / AE2 built-in Guide hints. Side FE/AE2 diagnostics added; an AE2 ME-network node/terminal/Drive is **NOT** implemented. Future pages reserve sophisticated upgrade sockets, map, voice, radar, plant and chunkloading features. CI/runtime acceptance still required. See CORE_DASHBOARD_ARCHITECTURE.md.
---

# v0.5.2 source candidate (2026-10-13)

Implemented industrial Base Core assets and real Create 6.0.10 kinetic generator (16 RPM / 256 SU, purchased), plus in-core tutorial and conservative book discovery. Changed sneak interaction so held connector items no longer pack core. Requires Create on both sides; Java policy tests pass, NeoForge CI and real in-game testing still pending. See `docs/releases/v0.5.2.md`, `docs/TEST_PLAN_v0.5.2.md`, and `docs/research/CORE_ONBOARDING.md`. AE2 ME-node/Drive and Sophisticated upgrade handlers remain deferred.

---

# Update 2026-10-12: v0.3.0 source candidate

**Implemented in source, NOT yet tested in Minecraft:** FTB opt-in auto-claims 3×3 (privacy default OFF), native CoreGuard, persistent 24h first-base Grace, Creative unique Core item fix, recovery without resetting transport hour. Still pending CI compile and two-client PvP/explosion/FTB acceptance. Do not enable raids.

**Next:** v0.3.0 multiplayer stability gate → v0.4 raid windows, war scoring and core damage after protection audits → v0.5 core upgrades/AE2, v0.6 Xaero revocable team map, v0.7 team voice/rename, v0.8 market, v0.9 events, future Sable mobile base. See release docs for exact contracts.

---

# Rogler TeamWars – Produkt-Roadmap (Stand 2026-10-11)

> Die Stufen sind **Planung**, nicht automatisch implementierte Funktionen. Für Minecraft-Laufzeit gilt nur, was im Testprofil nachgewiesen wurde. Zielserverstart: Montag, 12.10.2026, mit bewusst reduziertem Funktionsumfang.

## Tatsächlicher Implementierungsstand

| Baustein | Status am 11.10.2026 | Noch zu verifizieren |
|---|---|---|
| P0 reiner Java-Regelkern | Implementiert und getestet | Integrationsverhalten ist eigenständig |
| v0.1 FTB Teams → Vanilla Scoreboard, Nametags und Befehle | Im Minecraft-Testprofil gestartet; CI Build erfolgreich | Gegner-Nametags mit 2 echten Clients; Join/Leave / Mods mit eigenen Nametags |
| v0.2.0 Kern ausgeben, Overworld-Platzierung, SavedData, 1h Timer | Implementiert, reproduzierbare Platzierung anfangs mit Radiusfehler | Unterbrechungen/Timeout bei vollen Inventaren |
| v0.2.1 0–5000 Zone, Lava-Schutz, Self-Recovery | In Spiel getestet: Placement funktioniert. CI Build grün | Lava, Despawn, Restart, Escrow und alle Rettungspfade separat testen |
| v0.2.2 kontrolliertes Einpacken/Umzug | **Quellcode-Kandidat**, Java-Regeln getestet | NeoForge-CI und Multiplayer-Smoke-Test noch offen |
| Terminal-Monitor & automatischer GitHub-Artifact-Download | Real im Benutzerterminal erfolgreich geprüft | Kommende Versionen / Netzwerkfehler |

## Sofort (Release-Blocker für Montag)

1. **v0.2.2**: `Shift + Rechtsklick` und `/teamwars base pack` mit genau einem gültigen Kern pro Team. Tokenwechsel, Schutz vor Inventarverlust, Standortprüfung und Recover-Adminroutine. Reale Minecraft-Testfälle inklusive Neustart, Inventar voll, fremdes Team und Lava.
2. **FTB Chunks und Grace-Period**: Claim am Teamkern, definierte Teamrechte, Schutz gegen normales Mining und Fremdinteraktion, Grace beim Spielstart und nach Kernverlust. Keine Freigabe für Raids, bevor Explosions-/Technik-Wege geprüft sind.
3. **Grundlegender Wettkampf**: Nachweisbare autorisierte Kernzerstörung → exakt ein Kriegspunkt für Angreifer, konsistenter Ersatzkern und neue Grace-Period. Team-Scoreboard und Admin-Reset/Restore.
4. **Abnahmetest**: Zwei bis vier unterschiedliche Clients, FTB-Teams, Rechtsklick, Inventar-Limit, Chunk-Unloads, Neustart, Kernverlust, 30-Minuten-Lastlauf, CraftProbe-Logs, vollständiges Weltbackup. Keine neue ungetestete JAR direkt im Produktivbetrieb.

Wenn 2–3 nicht vollständig getestet sind: **MVP-Fallback** mit FTB Teams + FTB Chunks + Xaero Minimap/World Map + Simple Voice Chat, Claims und Kriegspunkte durch Admins verwalten. Kein ungeschütztes automatisches Raiden.

## Nach dem Serverstart: geplante Funktionen

### Teams & Kommunikation
- 2–4 Teams, 8–12 Spieler, 3–4 Spieler pro Team; FTB Teams ist Mitgliedschaftsquelle.
- Namen, Rollen/Leiter, Änderungsgebühren in Items an konkurrierende Teams, Cooldown.
- Nametags nur teamintern; Fehlerquelle sind fremde HUD-/Kartenmods.
- Simple Voice Chat: Teamfunk unbegrenzt und Gegner-Proximity hörbar; kein unbemerktes Isolated-Escape.

### Gemeinsame Exploration – großes eigenständiges Integrationsprojekt
- Ausschließlich Xaero's Minimap + World Map als eine zusammengehörige Karte; keine zweite FTB-Karte als Spieleroberfläche.
- Explorationsprovenienz nach UUID und Dimension; sichtbare Teamkarte = Vereinigung aktueller Mitglieder; bei Austritt nur fremde unüberlappte Bereiche entziehen.
- Nicht bloß Chunk-Masken: Terrain-Renderdaten, Client-Cache und Widerruf in beiden Xaero-Oberflächen testen. Kein Kartenwissen durch Chunky-Pregeneration.
- Teammarker, gemeinsamer Basis-Kernmarker, Distanzanzeige und Kartenringe; später 3D-Grenze nur für Kernträger.

### Basis & Schutz
- 0–5000 Overworld-Basisring als derzeitiger Standard, Admin-live-konfigurierbar, optional getrennte Spawn-Schutzzone.
- Teamgebundene FTB-Claims, Grace-Zeiten (Initial/Join/Neugründung/Kernverlust), getrennte Quoten für freie Team-Chunks und Core-force-loaded Chunks.
- Chunky-Friends-Kopplung zur kontrollierten Chunk-Vorabgenerierung; keine Exploration aus Pregeneration.
- Anti-Offline-Raid: Raidfenster, Vorankündigung, Angriffsregeln, kein Umzug während aktiven Raids.
- Core-Upgrades (FE, Create-Stress, spezifische Pflanzen-Wachstumsboni, begrenztes Chunkloading, Radar), mit harten Tick-/Serverlimits.
- Kern-Inventar mit später auswählbaren Sophisticated-Core-Upgrade-Handlern; AE2-Integration für Terminal/Drive/Netzwerk.

### Kampfsystem, Events und Wirtschaft
- Kriegspunkte und Info-Leaks, Balancing gegen Snowball, saisonaler Sieg-/Finalmodus.
- Eventengine: Sektor-Eroberung, CTF, Luftschiffkämpfe, Konvois, Supply Drops, Aufklärung, neutrale Festungen, Boss-Jagd.
- Globaler Item-gegen-Item-Markt mit serverseitigem Escrow und Team-Postfach, optional risikobehaftete Konvois für wertvolle Items.
- Startstruktur beim Worldgen mit Starter Structure, geschützter Spawn-Hub, Admin-Webmap als Start, später Team-Fog-of-War (BlueMap-Integration nur nach Datenschutzprüfung).

### Mobile Basen (experimentell, höchste Integrationsgefahr)
- Sable/Create Aeronautics: dynamische Weltposition des Kerns auf dem Schiff, Persistenz über Dock/Undock und Restarts.
- 10 Minuten kumulative Überschreitungszeit jenseits erlaubtem Radius; Sirene und kontrollierte Nuke-artige Selbstzerstörung ohne Welt-/Spawn-Griefing.
- Keine produktive Freigabe ohne Server-Performance- und Missbrauchstests.

## Technische Regeln für weitere GPT-6-Chats
- `AGENTS.md` + `docs/PROJECT_STATE.md` zuerst; `core/` bleibt unabhängig von Minecraft.
- Features strikt in Docs, Java-Unit-Tests, NeoForge-CI und Multiplayer-Tests getrennt bewerten.
- Pro Patch: gesamte Repository-Version optional als ZIP + `git am`-Patch, Datei nach Anwendung löschen, `scripts/test-core.ps1`, Push, `scripts/watch-actions.ps1`, Diagnose.
- Release immer mit `docs/MOD_REQUIREMENTS.md` und kurzer Pflicht-/Optional-Liste, inklusive **keine neuen Mods**, wenn zutreffend.

## v0.4.0 source implementation (2026-10-12)
Core-only OP-controlled raids and persistent war score added in source, not yet NeoForge/runtime verified. Auto claims default TRUE for new-world config and reconcile with PRIVATE forced via FTB API; existing config values preserved. General siege permissions, cross-mod weapons, offline raid windows, map secrecy and two-player proof remain pending. Next v0.4.x is actual NeoForge runtime and FTB-privacy testing, then wider raid weapon compatibility.
