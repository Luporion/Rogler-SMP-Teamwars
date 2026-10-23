# Rogler Core – aktueller Projektstatus

**v0.8.0 CI-Korrektur (23. Oktober 2026):** GitHub Actions `39936204517` konnte nach erfolgreich geladenen Registrate-/NeoForge-Abhängigkeiten nicht kompilieren: `LocalWaypointBridge.syncPrivate()` rief `WaypointLinkClient.reservedLocations()` (deklariert `throws Exception`) ohne Abfangen auf, obwohl die aufrufende Methode nur `ReflectiveOperationException` weiterreicht. Separater Buildfix-Patch fängt den Journalzugriff ab und bricht den Privatimport bei Fehlern vollständig ab (kein sicherheitsgefährdendes Wiederherstellen gelöschter Marker). **Lokale Regel-/Strukturtests grün; neuer CI-Build und Multiplayer-Test noch offen.**

**Stand 23. Oktober 2026: v0.8.0 SOURCE CANDIDATE**, basierend auf `feat/v0.7.7-update`. Neu: lokal persistente, explizite FTB/Xaero-Wegpunktpaare mit eigener UUID, Vorschau und Token-gesicherter Übernahme von Name/Farbe, Blockierung versehentlicher Wiederherstellung zuvor verknüpfter gelöschter Punkte. Keine automatische Delete-/Move-Synchronisierung. Teamfreigaben bleiben unverändert explizit und serverautorisiert; Protokoll weiterhin `3`. Details: [v0.8.0 Release](releases/v0.8.0.md).

**Verifiziert:** v0.7.7-Commit `9bdc08f3` – P0 Core Run `39875853278` und NeoForge Run `39875853255` erfolgreich, `roglercore-0.7.7.jar` heruntergeladen. **Nicht verifiziert:** Minecraft-Laufzeit v0.7.7 und NeoForge-Kompilierung/Spieltest v0.8.0. Die bisherigen Registrate-HTTP-521-Probleme sind für diesen grünen CI-Lauf überwunden, aber künftige Läufe bleiben von externen Abhängigkeiten abhängig.

## Verifizierte Evidenz (Tests streng unterscheiden)

| Stand | Java-/Regeltests | CI-Kompilierung | Minecraft-/Multiplayer-Laufzeit |
|---|---|---|---|
| v0.7.3 | Bestandene Regeltests | Erfolgreich | **Join-Fehler**: Architectury-FTB-Payload falsch über NeoForge versendet |
| v0.7.4 | Bestanden | Erfolgreich | **Serverbeitritt wieder möglich**, vom Nutzer bestätigt; Datenschutz/Waypoint-Anzeige noch nicht umfassend abgenommen |
| v0.7.5 | Bestanden | **Erfolgreich** (GitHub Run `39613065419`) | Nicht nachgewiesen |
| v0.7.6 | Bestanden, inklusive 14 Wegpunkt-Regelchecks | Historische Builds 39618333770 scheiterten an Registrate Maven 521; Bootstrapping inzwischen eingebaut | Nicht nachgewiesen |
| v0.7.7 | Bestanden | **Erfolgreich** (Runs `39875853278`, `39875853255`) | Nicht nachgewiesen |
| v0.8.0 | Neue Link-/Journal-Regeltests erstellt | **Noch offen** | **Noch offen** |

Die ursprünglichen 0.7.6-Fehler betrafen `com.tterrag.registrate:Registrate:MC1.21-1.3.0+67` (HTTP 521). Der versionierte, SHA-256-geprüfte Create-JarJar-Bootstrap war im erfolgreichen v0.7.7-GitHub-Workflow vorhanden. Für v0.8.0 ist dennoch ein neuer CI-Lauf erforderlich.

## Aktueller Quellcode: enthalten, aber nicht automatisch freigegeben

| Funktion | Status/Grenzen |
|---|---|
| FTB-Teams-Zuordnung; Scoreboard-Nametags | Quellcode vorhanden, ältere Gameplay-Tests partiell; Privatsphäre gegenüber anderen HUD-/Kartenmods separat prüfen |
| Basis-Core, 1h-Transport, Rettung, Schutz, FTB-Claims | Im Projekt implementiert; einzelne Interaktionen im Spiel getestet, umfassende Mehrspieler-/Raid-Abnahme ausstehend |
| Core-Inventar, FE, Create-Stress, Upgrades/Manuale | Vorhandene Teilfunktionen; **keine** vollwertige AE2-ME-Drive-/Sophisticated-Upgradekompatibilität nachgewiesen |
| Raids, Punkte, Explosions-/Sable-Guards | Teilweise implementiert; Schutz über alle Mods/Waffen, Performance und Wiederherstellung nicht vollständig abgenommen |
| Teampositions-HUD | v0.7.5 optional und standardmäßig aus; clientseitige GUI/Positionierung noch praktisch prüfen |
| Eigene Team-/Basismarker in Xaero | v0.7.5 im Code; Minimap- und World-Map-Anzeige und Entzug real prüfen |
| v0.8.0 private Wegpunkt-Paare | Opt-in pro Serverprofil gespeicherte Verknüpfungs-IDs, Token-gesicherte Metadaten-Übernahme, Lösch-/Move-Konflikte nicht automatisch ändern; Kompilation/Laufzeit offen |
| v0.7.6 Waypoints | Explizites Teilen/Widerrufen; serverseitig FTB-Team-gefilterte temporäre Marker; **manueller create-only** Privatimport FTB↔Xaero. Kein automatisches Umbenennen/Löschen |
| Gemeinsame Xaero-Geländekarte | Serverseitige Explorationsregeln/Sessiongrundlage; **native Geländekachel-Synchronisierung und zuverlässiger Cache-Widerruf fehlen** |
| Teamfunk, Core-Chunkloader-Sonderquoten, Markt, Events, mobile Sable-Basen | Planungs-/Teilintegration abhängig von Teilbereich; keines als vollständig getestet markieren |

## Unmittelbare Blocker und nächste Aktion

1. **v0.8.0 CI validieren:** `feat/v0.7.7-update` als Basis, P0 Core und NeoForge Actions für v0.8.0 ausführen, Java-/API-Kompilierfehler beheben; Registrate-Hashprüfung beibehalten.
2. **DEV-Abnahme** erst nach grünem v0.8.0-Build: Server + alle Clients exakt dieselbe Rogler-Core-JAR; zwei unterschiedliche FTB-Teams einschließlich OP testen, keine Gegnerpunkte/-koordinaten; keine `EncoderException` auf Join.
3. **Waypoint-Probe:** `/roglerhud`, `/roglerwaypoints list|sync|share|unshare|link|changes|apply|acknowledge|unlink` auf Backup-Client; Xaero/FTB-Anzeige, Duplikate, Relog/Serverrestart, privater Speicher, Dimensionen und Teamwechsel prüfen.
4. **Daten schützen:** `long_range_tracker_interval: 0`, Location/Claim Visibility der bereits existierenden FTB-Teams privat und Xaero-Radar aus, bis Security-Test bestanden. Kein World-, Waypoint- oder Claim-Reset.
5. Nach diesen Gates: synchronisierte Änderungen/Löschungen konfliktfrei machen, anschließend widerrufbares Xaero-Terrain-Sharing. Prioritäten: [ROADMAP.md](ROADMAP.md).

## Kontext und Hinweise

- Bekannte Zielversionen: Minecraft 1.21.1, NeoForge 21.1.249, Create 6.0.10, FTB Teams 2101.1.11, FTB Chunks 2101.1.22, FTB Library 2101.1.37, Xaero Minimap 26.4.2 und World Map 1.45.0. Installation auf beiden Seiten prüfen; keine impliziten Updates.
- Historische Statusschnappschüsse sind unter [archivierten Projektständen](archive/project-states/) und der [unveränderten bisherigen PROJECT_STATE.md](archive/legacy/PROJECT_STATE_before_cleanup_2026-10-22.md) verfügbar. Frühere Aussagen wie „noch kein Core“ gelten **für ihre damalige Version**, nicht für heute.
- Neuester vorliegender CI-Nachweis: **v0.7.7 erfolgreich kompiliert** am 23. Oktober 2026. v0.8.0 muss erst gebaut und in Minecraft getestet werden.
