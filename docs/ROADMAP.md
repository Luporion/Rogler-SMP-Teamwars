# Rogler Core – Produktroadmap

**Stand: 31. Oktober 2026.** Die Roadmap beschreibt Ziele und Prioritäten, **nicht** automatisch freigegebene Spielmechaniken. Technischer Iststand mit Belegen: [PROJECT_STATE.md](PROJECT_STATE.md). Ursprüngliche vollständige Roadmap und ältere Milestones sind [unverändert archiviert](archive/legacy/ROADMAP_before_cleanup_2026-10-22.md).

## Priorität 0 – v0.8.3 Stabilitaet/C2ME/Control Center abnehmen

- [x] v0.8.2 P0/NeoForge-Build erfolgreich (Runs `40389304266` / `40389304252`); Runtime weiterhin separat zu pruefen.
- [x] v0.8.3 Quellcode: blockierungsfreie Create-NBT-Serialisierung, geladene Chunk-Pruefung, Admin-Suche, JAR-Version-CI-Guard.
- [ ] v0.8.3 NeoForge/JAR-Metadaten-Build gruen; anschliessend Backup-DEV-Server mit C2ME, Core und FTB-Chunk-Unload/Logout testen.
- [ ] Zwei-Team-OP-/Nicht-OP-Admin-/Wegpunkt-/Core-Regression nachweisen.


- [x] v0.7.4: FTB-Architectury-Join-Fehler korrigiert; Beitritt im Spiel bestätigt.
- [x] v0.7.5: HUD standardmäßig aus, Position und Entfernungen konfigurierbar; NeoForge CI grün.
- [x] v0.7.6: Explizite teamgebundene Wegpunktfreigabe, Urheber-Widerruf, private fehlende FTB↔Xaero-Wegpunkte importieren – **als Quellcode**.
- [x] v0.7.7 (Quellcode): Konfliktvorschau, sicherer Namens-/Farbabgleich mit Token, Dimensions- und Palettenkorrektur. Keine automatische Löschung/Verschiebung.
- [x] v0.7.7: NeoForge-JAR erfolgreich gebaut (CI `39875853255`); Laufzeit noch offen.
- [x] v0.8.0 (Quellcode): private Wegpunktverknüpfungen mit persistenter ID, Konfliktanzeige und Schutz vor ungewollter Wiederherstellung.
- [x] v0.8.0: NeoForge-Build erfolgreich (Run `39938411669`), aber In-Game-Tests weiter offen.
- [x] v0.8.1 Quellcode: explizite, Token-geschuetzte Verschiebungen/Loeschungen und optionale Name/Farbe-Automatik implementiert.
- [x] v0.8.1: NeoForge CI erfolgreich (Runs `40360115893` / `40360115915`), Test auf Liveserver begonnen.
- [ ] v0.8.1: Client-Persistenz, Gegenseiten-Aenderungen und Multiplayer-Privacy im Spiel nachweisen.
- [x] v0.8.2 (Quellcode): einheitliches HUD-/Waypoint-/Team-/Core-/Admin-Control-Center, 24 validierte Weltsettings, serverseitige Befehls-/Permissions-Grenzen.
- [x] v0.8.2: NeoForge-Build erfolgreich.
- [ ] v0.8.2/0.8.3: GUI-/Admin-DEV-Abnahme, OP2/OP4 und Nicht-OP-Datenschutz testen.
- [ ] v0.7.6: 2-Team-/OP-Datenschutz, Xaero- und FTB-Rendering, Freigabe/Widerruf, Rejoin, Dimension, Restart, Backup und Duplikate praktisch testen.
- [ ] Zeitkritische Integrationsregressionen zu Modupdates/Packet-Encodern/Kompatibilität systematisch absichern.

## Priorität 1 – Karten, Wegpunkte und Teamkoordination

| Ziel | Bestehender Stand | Fehlender Nachweis / Entwicklungsarbeit |
|---|---|---|
| Persönliche Exploration pro UUID/Dimension | Kernlogik/Session-Verwaltung vorhanden | Reale Client-Karte und vollständige Revoke-Tests |
| Freigegebene Team-Wegpunkte | v0.7.6 Source: explizit teilen/zurücknehmen; ephemeral Marker | CI, In-Game-Nachweis, OP- und Ex-Mitglied-Tests |
| Private FTB↔Xaero-Synchronisierung | v0.8.1 Source: Auto-Verknüpfung neuer Imports; Name/Farbe opt-in automatisch, Moves/Deletes mit explizitem Token | Minecraft-DEV-Abnahme; native ID/Event-Forschung für künftig automatische Moves/Deletes ohne Datenverlust |
| Verbündete und eigene Basis auf Xaero | v0.7.5 Source mit Namenraum-Markern | Minimap + World Map verifizieren, tote Marker verhindern |
| Widerrufbare gemeinsame Geländekarte | Serverseitig personenbezogene Exploration | Echte Terrain-Tiles in Xaero übernehmen, persönliche Daten unverändert lassen, Widerruf bei Austritt/Rejoin/Cache-Clear |
| HUD & Distanznavigation | v0.7.5 optionale, Client-Einstellungen | Komforttests, Überlagerungen anderer HUDs, ggf. Drag & Drop ohne zusätzliche Pflichtmod |
| Gegner nur bei Proximity sichtbar | FTB/OP-Kartenfilter im Code, FTB Long-Range deaktivierbar | Umfassender Audit inklusive anderer Mods und Marker |

## Priorität 2 – Teamfunk, Core & Wettbewerb

- **Teams:** 2–4 Teams und typischerweise 3–4 Spieler je Team; FTB Teams als Mitgliedschaftsquelle; sichere Team-Erstellungslimits, Rollen, Umbenennungsgebühren zugunsten gegnerischer Teams; gegnerische Nametags und ungewollte Positionslecks vermeiden.
- **Voice:** Simple Voice Chat für unbegrenzten internen Teamfunk und regulären Proximity-Kanal gegenüber Gegnern, mit Rejoin-/Teamwechsel-Regeln und echten Clients.
- **Core & Basiszone:** Eigener Core in Overworld, derzeit 0–5000 Blöcke vom Spawn; Trägerwarnungen, sichere 1h-Rettung und kontrollierbarer Umzug; Grace, Claims und Rechte zuverlässig absichern.
- **Chunkloading:** Getrennte Quoten für Core-geladene und frei von Spielern geladene FTB-Chunks; Kompatibilität zu Create Power Loader und anderen Loaders. Ticketauflösung unter Serverlast testen.
- **Core-Technik:** FE-Ausgabe und Create-Stress erweitern; echtes AE2-Terminal/ME-Drive und kontrollierte Sophisticated-Upgrades statt ungesicherten Integrationsbehauptungen; begrenzte Radar-/Pflanzenboni.
- **PvP/Raids:** Angekündigte Raidfenster, Schutz vor Offline-Raids, genau ein Kriegspunkt je legitimer gegnerischer Core-Zerstörung, Ersatzcore und neue Grace; Missbrauch via Explosionen/Automationsmods verhindern.

## Priorität 3 – Wirtschaft, Events und Sable

- Globaler Item-für-Item-Markt mit serverseitigem Escrow, Team-Postfach, Transaktions-IDs und sicherem Rollback.
- Events: Sektorkontrolle, Capture the Flag, Konvois, Supply Drops, Boss-/Festungsziele und saisonales Finale.
- Starter-/Spawn-Hub, strukturierte Zonen auf Xaero und optional Admin-Webkarte ohne versteckte Gegnerdaten.
- Mobile Basen mit Sable/Create: Core-Position auf Schiffen, Dock/Undock/Restart, kumulativ 10 Minuten außerhalb der Zone, kontrollierte Warnung und sichere Selbstzerstörung – **nicht produktiv ohne umfangreiche Sicherheits- und Performance-Tests**.

## Reihenfolge nach v0.8.3

1. Kritischen C2ME/Create-Core-Serialisierungshotfix mit NeoForge-CI und eigenem Chunk-Save-/Unload-Test nachweisen; danach Control Center im Multiplayer (OP2/OP4/Nicht-OP), Konfigspeicherung/Restart und bestaetigte Core-Aktionen abnehmen.
2. Bestehende Admin-Command-Ergebnisberichte schrittweise durch strukturierten UI-Status ersetzen; FTB Teams- und Sable-/Warium-eigene Konfig nur nach API-/Restart-Pruefung integrieren. Keine Pseudo-Regler.
3. Geplantes Shared-Xaero-Terrain-Sharing spaeter als eigenstaendigen Fork von Xaero's Maps: Multiplayer+ pruefen (MIT-Quelle; FTB Team-Scopes und private Explorationsprovenienz).
4. Teamfunk, Core-Chunkloading sowie AE2-/Sophisticated-Integrationen entwickeln und danach ebenfalls ueber das Control Center steuerbar machen; Wirtschaft und Events folgen.

Bei jedem Release [Testmatrix](TEST_MATRIX.md) mit getrenntem Java-Test, NeoForge-CI und Minecraft-Gameplay-Nachweis ausfuellen.
