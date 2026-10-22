# Rogler Core – aktueller Projektstatus

**Stand der übergebenen Repository-ZIP: 22. Oktober 2026 · Entwicklungsbranch `feat/v0.7.6-update` · Modversion v0.7.6.** Dies ist der **Entwicklungsstatus**, keine neue Release-Freigabe. Die ZIP enthält Quellcode und den Registrate-CI-Bootstrap, aber keinen bestätigten erfolgreichen v0.7.6-NeoForge-Build. [Release v0.7.6](releases/v0.7.6.md) · [Buildfix](guides/REGISTRATE_CI_BOOTSTRAP_v0.7.6.md).

## Verifizierte Evidenz (Tests streng unterscheiden)

| Stand | Java-/Regeltests | CI-Kompilierung | Minecraft-/Multiplayer-Laufzeit |
|---|---|---|---|
| v0.7.3 | Bestandene Regeltests | Erfolgreich | **Join-Fehler**: Architectury-FTB-Payload falsch über NeoForge versendet |
| v0.7.4 | Bestanden | Erfolgreich | **Serverbeitritt wieder möglich**, vom Nutzer bestätigt; Datenschutz/Waypoint-Anzeige noch nicht umfassend abgenommen |
| v0.7.5 | Bestanden | **Erfolgreich** (GitHub Run `39613065419`) | Nicht nachgewiesen |
| v0.7.6 | **Bestanden**, inklusive 14 Wegpunkt-Regelchecks | **Nicht nachgewiesen**: Build Run `39618333770` + Wiederholung scheiterten an Registrate Maven HTTP 521. CI-Bootstrap ist im ZIP vorhanden, erneuter Erfolg nicht belegt | Nicht nachgewiesen |

Der 0.7.6-Fehler trat beim Auflösen von `com.tterrag.registrate:Registrate:MC1.21-1.3.0+67` auf und beweist **keinen** Java-Compilerfehler; er schließt einen späteren Compilerfehler ebenso wenig aus. Der neue SHA-256-geprüfte Create-JarJar-Bootstrap muss erst durch Actions laufen.

## Aktueller Quellcode: enthalten, aber nicht automatisch freigegeben

| Funktion | Status/Grenzen |
|---|---|
| FTB-Teams-Zuordnung; Scoreboard-Nametags | Quellcode vorhanden, ältere Gameplay-Tests partiell; Privatsphäre gegenüber anderen HUD-/Kartenmods separat prüfen |
| Basis-Core, 1h-Transport, Rettung, Schutz, FTB-Claims | Im Projekt implementiert; einzelne Interaktionen im Spiel getestet, umfassende Mehrspieler-/Raid-Abnahme ausstehend |
| Core-Inventar, FE, Create-Stress, Upgrades/Manuale | Vorhandene Teilfunktionen; **keine** vollwertige AE2-ME-Drive-/Sophisticated-Upgradekompatibilität nachgewiesen |
| Raids, Punkte, Explosions-/Sable-Guards | Teilweise implementiert; Schutz über alle Mods/Waffen, Performance und Wiederherstellung nicht vollständig abgenommen |
| Teampositions-HUD | v0.7.5 optional und standardmäßig aus; clientseitige GUI/Positionierung noch praktisch prüfen |
| Eigene Team-/Basismarker in Xaero | v0.7.5 im Code; Minimap- und World-Map-Anzeige und Entzug real prüfen |
| v0.7.6 Waypoints | Explizites Teilen/Widerrufen; serverseitig FTB-Team-gefilterte temporäre Marker; **manueller create-only** Privatimport FTB↔Xaero. Kein automatisches Umbenennen/Löschen |
| Gemeinsame Xaero-Geländekarte | Serverseitige Explorationsregeln/Sessiongrundlage; **native Geländekachel-Synchronisierung und zuverlässiger Cache-Widerruf fehlen** |
| Teamfunk, Core-Chunkloader-Sonderquoten, Markt, Events, mobile Sable-Basen | Planungs-/Teilintegration abhängig von Teilbereich; keines als vollständig getestet markieren |

## Unmittelbare Blocker und nächste Aktion

1. **CI-Fix validieren:** `feat/v0.7.6-update` auf dem zu dieser ZIP passenden Commit prüfen und den GitHub Actions-Build mit dem Registrate-Bootstrap ausführen. SHA-256-Abweichungen strikt behandeln, nicht ignorieren. Danach Java-Kompilierfehler falls vorhanden beheben.
2. **DEV-Abnahme** erst nach grünem Build: Server + alle Clients exakt dieselbe Rogler-Core-JAR; zwei unterschiedliche FTB-Teams einschließlich OP testen, keine Gegnerpunkte/-koordinaten; keine `EncoderException` auf Join.
3. **Waypoint-Probe:** `/roglerhud`, `/roglerwaypoints list|sync|share|unshare` auf Backup-Client; Xaero/FTB-Anzeige, Duplikate, Relog/Serverrestart, privater Speicher, Dimensionen und Teamwechsel prüfen.
4. **Daten schützen:** `long_range_tracker_interval: 0`, Location/Claim Visibility der bereits existierenden FTB-Teams privat und Xaero-Radar aus, bis Security-Test bestanden. Kein World-, Waypoint- oder Claim-Reset.
5. Nach diesen Gates: synchronisierte Änderungen/Löschungen konfliktfrei machen, anschließend widerrufbares Xaero-Terrain-Sharing. Prioritäten: [ROADMAP.md](ROADMAP.md).

## Kontext und Hinweise

- Bekannte Zielversionen: Minecraft 1.21.1, NeoForge 21.1.249, Create 6.0.10, FTB Teams 2101.1.11, FTB Chunks 2101.1.22, FTB Library 2101.1.37, Xaero Minimap 26.4.2 und World Map 1.45.0. Installation auf beiden Seiten prüfen; keine impliziten Updates.
- Historische Statusschnappschüsse sind unter [archivierten Projektständen](archive/project-states/) und der [unveränderten bisherigen PROJECT_STATE.md](archive/legacy/PROJECT_STATE_before_cleanup_2026-10-22.md) verfügbar. Frühere Aussagen wie „noch kein Core“ gelten **für ihre damalige Version**, nicht für heute.
- Letztes bekanntes Log belegt **keinen** erfolgreichen v0.7.6 Actions-Lauf nach dem CI-Bootstrap; dessen Ausgang ist aktuell offen.
