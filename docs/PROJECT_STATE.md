# Rogler Core – aktueller Projektstatus

**Stand 24. Oktober 2026 – v0.8.1 SOURCE CANDIDATE:** Auf v0.8.0 Build-Commit `f0b7a91` (erfolgreiche Core- und NeoForge-Workflows `39938411693` / `39938411669`) aufbauend. v0.8.0-JAR erstellt; Minecraft-Laufzeit nicht durch Logs nachgewiesen. v0.8.1 enthält Import-Auto-Verknüpfung eindeutig importierter privater Punkte und explizite, token-geschützte Bestätigungen für das Entfernen einer verwaisten Gegenstelle und das Verschieben eines ausgewählten Wegpunkts; optionaler clientseitiger automatischer **Name/Farbe-only**-Abgleich (standardmäßig AUS, nur aktuelle Sitzung). **v0.8.1 NeoForge Build und reale Laufzeit nicht verifiziert.** [Release v0.8.1](releases/v0.8.1.md).

Sicherheitsgrenze: Ohne native gemeinsame ID wird Löschen/Verschieben *nicht automatisch erraten*. Fehlende/mehrdeutige Wegpunkte werden blockiert. Nach bestätigtem Löschen bleibt der lokale Link als Tombstone erhalten; private FTB-/Xaero-Daten werden nicht zum Server gesendet. Der FTB-Move benötigt eine neue Instanz (FTB API ohne setPos) und ist nicht transaktional garantiert: Backup und realer DEV-Test erforderlich.

---


## Verifizierte Evidenz (Tests streng unterscheiden)

| Stand | Java-/Regeltests | CI-Kompilierung | Minecraft-/Multiplayer-Laufzeit |
|---|---|---|---|
| v0.7.3 | Bestandene Regeltests | Erfolgreich | **Join-Fehler**: Architectury-FTB-Payload falsch über NeoForge versendet |
| v0.7.4 | Bestanden | Erfolgreich | **Serverbeitritt wieder möglich**, vom Nutzer bestätigt; Datenschutz/Waypoint-Anzeige noch nicht umfassend abgenommen |
| v0.7.5 | Bestanden | **Erfolgreich** (GitHub Run `39613065419`) | Nicht nachgewiesen |
| v0.7.6 | Bestanden, inklusive 14 Wegpunkt-Regelchecks | Historische Builds 39618333770 scheiterten an Registrate Maven 521; Bootstrapping inzwischen eingebaut | Nicht nachgewiesen |
| v0.7.7 | Bestanden | **Erfolgreich** (Runs `39875853278`, `39875853255`) | Nicht nachgewiesen |
| v0.8.0 | 29 Link-/10 Journal-Tests bestanden | **Erfolgreich** (Run `39938411669` nach Hotfix) | **Offen** |
| v0.8.1 | Neue Move/Delete-/Auto-Metadata-Regeln getestet | **Offen** | **Offen** |

Die ursprünglichen 0.7.6-Fehler betrafen `com.tterrag.registrate:Registrate:MC1.21-1.3.0+67` (HTTP 521). Der versionierte, SHA-256-geprüfte Create-JarJar-Bootstrap war im erfolgreichen v0.7.7-GitHub-Workflow vorhanden. v0.8.0 wurde nach dem Hotfix erfolgreich gebaut; v0.8.1 benötigt einen eigenen neuen CI-Lauf.

## Aktueller Quellcode: enthalten, aber nicht automatisch freigegeben

| Funktion | Status/Grenzen |
|---|---|
| FTB-Teams-Zuordnung; Scoreboard-Nametags | Quellcode vorhanden, ältere Gameplay-Tests partiell; Privatsphäre gegenüber anderen HUD-/Kartenmods separat prüfen |
| Basis-Core, 1h-Transport, Rettung, Schutz, FTB-Claims | Im Projekt implementiert; einzelne Interaktionen im Spiel getestet, umfassende Mehrspieler-/Raid-Abnahme ausstehend |
| Core-Inventar, FE, Create-Stress, Upgrades/Manuale | Vorhandene Teilfunktionen; **keine** vollwertige AE2-ME-Drive-/Sophisticated-Upgradekompatibilität nachgewiesen |
| Raids, Punkte, Explosions-/Sable-Guards | Teilweise implementiert; Schutz über alle Mods/Waffen, Performance und Wiederherstellung nicht vollständig abgenommen |
| Teampositions-HUD | v0.7.5 optional und standardmäßig aus; clientseitige GUI/Positionierung noch praktisch prüfen |
| Eigene Team-/Basismarker in Xaero | v0.7.5 im Code; Minimap- und World-Map-Anzeige und Entzug real prüfen |
| v0.8.0/0.8.1 private Wegpunkt-Paare | Opt-in pro Serverprofil gespeicherte Verknüpfungs-IDs; v0.8.1 neuer sicherer Auto-Import-Link, Name/Farbe-Automatik opt-in; bestätigte Move/Delete-Gegenstellen. v0.8.1 Build/Laufzeit offen |
| v0.7.6 Waypoints | Explizites Teilen/Widerrufen; serverseitig FTB-Team-gefilterte temporäre Marker; **manueller create-only** Privatimport FTB↔Xaero. Kein automatisches Umbenennen/Löschen |
| Gemeinsame Xaero-Geländekarte | Serverseitige Explorationsregeln/Sessiongrundlage; **native Geländekachel-Synchronisierung und zuverlässiger Cache-Widerruf fehlen** |
| Teamfunk, Core-Chunkloader-Sonderquoten, Markt, Events, mobile Sable-Basen | Planungs-/Teilintegration abhängig von Teilbereich; keines als vollständig getestet markieren |

## Unmittelbare Blocker und nächste Aktion

1. **v0.8.1 CI validieren:** `feat/v0.8.0-update` als Basis, P0 Core und NeoForge Actions für v0.8.1 ausführen; Registrate-Hashprüfung beibehalten.
2. **DEV-Abnahme** erst nach grünem v0.8.1-Build: Server + alle Clients exakt dieselbe Rogler-Core-JAR; zwei unterschiedliche FTB-Teams einschließlich OP testen, keine Gegnerpunkte/-koordinaten; keine `EncoderException` auf Join.
3. **Waypoint-Probe:** `/roglerhud`, `/roglerwaypoints list|sync|sync-linked|autosync|share|unshare|link|changes|apply|delete-linked|move-linked|acknowledge|unlink` auf Backup-Client; Xaero/FTB-Anzeige, Duplikate, Relog/Serverrestart, privater Speicher, Dimensionen und Teamwechsel prüfen.
4. **Daten schützen:** `long_range_tracker_interval: 0`, Location/Claim Visibility der bereits existierenden FTB-Teams privat und Xaero-Radar aus, bis Security-Test bestanden. Kein World-, Waypoint- oder Claim-Reset.
5. Nach diesen Gates: native Identitäts-/Event-Strategie für vollständig automatische Move/Delete-Propagation erforschen, anschließend widerrufbares Xaero-Terrain-Sharing. Prioritäten: [ROADMAP.md](ROADMAP.md).

## Kontext und Hinweise

- Bekannte Zielversionen: Minecraft 1.21.1, NeoForge 21.1.249, Create 6.0.10, FTB Teams 2101.1.11, FTB Chunks 2101.1.22, FTB Library 2101.1.37, Xaero Minimap 26.4.2 und World Map 1.45.0. Installation auf beiden Seiten prüfen; keine impliziten Updates.
- Historische Statusschnappschüsse sind unter [archivierten Projektständen](archive/project-states/) und der [unveränderten bisherigen PROJECT_STATE.md](archive/legacy/PROJECT_STATE_before_cleanup_2026-10-22.md) verfügbar. Frühere Aussagen wie „noch kein Core“ gelten **für ihre damalige Version**, nicht für heute.
- Neuester belegter NeoForge-Build: **v0.8.0 erfolgreich kompiliert** am 23. Oktober 2026 (Run `39938411669`). v0.8.1 muss noch gebaut und im Minecraft-Testprofil erprobt werden.
