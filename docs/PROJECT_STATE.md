# Rogler Core – aktueller Projektstatus

**31. Oktober 2026: v0.8.3 SOURCE CANDIDATE** auf dem nachweislich erfolgreich in CI gebauten v0.8.2-Branch (`feat/v0.8.2-update`, Commit `b7b0874f`; P0 `40389304266`, NeoForge `40389304252`). v0.8.2 wurde in Github Actions erfolgreich gebaut und `roglercore-0.8.2.jar` heruntergeladen. In der Server-Sitzung vom 30.10.2026 wurde ein kritischer Watchdog-Hang im Base-Core/Create/C2ME-Speicherpfad beobachtet.

**v0.8.3 korrigiert den konkreten Rogler-Core-Aufrufpfad:** keine World-/Chunk-/SavedData-Abfrage mehr aus `CoreKineticBlockEntity.write()` oder dem Create-Speed-Getter; autoritativer Cache-Abgleich nur auf dem Server-Tick/bei expliziter Aktualisierung; physische Team-Owner-Prüfung per geladenem Chunk (`getChunkNow()`, fail-closed) statt potenziell synchronem Chunk-Laden; zusätzliche Admin-Suche. Die JAR-interne Modversion wird nun gegen Gradle geprüft. **Lokale Regel-/Stub-/Statische Tests grün; NeoForge-CI und reale Minecraft-Tests v0.8.3 noch ausstehend.** [Release v0.8.3](releases/v0.8.3.md).

**Crash-Evidenz:** `crash-reports/crash-2026-10-30_15.23.37-server.txt` aus der Session-Analyse enthält `CoreKineticBlockEntity.write → enabled → CoreInfrastructureService.owner → Level.getBlockState → ServerChunkCache.getChunk → managedBlock`. C2ME speicherte/serialisierte den Chunk. Der Fix beseitigt diese Zugriffsfolge, beweist aber nicht, dass andere Server-Stalls verschwinden.

**Offen:** CI-Build, 2-Team-/OP-Datenschutz, Core-Chunk-Unload-/Reload-/Ticket-C2ME-Dauertest, GUI-Persistenz und Waypoint-Reconcile-Regression; danach v0.8.3 Freigabeentscheidung. Kein SavedData-Schema-/Protokollwechsel, keine neuen Pflichtmods. Shared Xaero Terrain bleibt bewusst später.

---

**v0.8.1 ist erfolgreich gebaut**: CI Core `40360115893` und NeoForge `40360115915` am 26.10.2026 gruen, JAR `roglercore-0.8.1.jar` heruntergeladen, vom Nutzer auf den Server geladen; Live-Gesamttest noch laufend, kein vollstaendiger Ergebnisnachweis. v0.8.0 CI ebenfalls erfolgreich (Run `39938411669`). Die aelteren v0.7.3 Join-/Architectury-Probleme sind seit v0.7.4 vom Nutzer als behoben gemeldet; Regressiontests bleiben noetig.

**Wichtige Grenzen:** V0.8.2 fuehrt keine neue Teamfunk-, Shared-Xaero-Terrain-, AE2-ME-Drive-, Sophisticated-Upgrade-, Chunkloading-, Markt- oder Event-Engine ein. Admin-UI kann alle bisherigen Rogler-Core-Welt-Properties aendern und fuehrt vorhandene geschuetzte Admin-Commands aus; deren Ergebnis steht derzeit noch im Minecraft-Chat. FTB-eigene Party- und Claim-Grenzen bleiben weiterhin Sache der FTB-Mod. Rogler-eigene Warium/Sable-Spezialkonfig kann mit OP4 und expliziter Bestaetigung veraendert werden; dies ist keine Bearbeitung der fremden Mods oder Rueckwirkung auf bereits geladene Entities. Schutzkritische Parameter OP4 und Bestaetigung. Die neuen Settings aendern bestehende Server-Dateien, keine automatische Rueckmigration. Waypoint/Terrain-Privatsphaere und zwei gegnerische Teams muessen live geprueft werden.

**Naechste konkrete Schritte:** (1) GitHub NeoForge-Build v0.8.3 und JAR-Metadaten-Check, (2) DEV-Weltbackup, C2ME/FTB-Chunk-Unload-/Logout-Test mit platziertem Create-Core, (3) zwei Clients (auch OP2, OP4, Nicht-OP), (4) UI-Felder inkl. Suche, Reload/Persistenz, unerlaubte Aktionen und GUI-Keybind testen, (5) Core-Dashboard und Wegpunkte/Team-Sichtbarkeit regressionspruefen. [Roadmap](ROADMAP.md) · [Testmatrix](TEST_MATRIX.md).

Technikziel weiterhin Minecraft 1.21.1, NeoForge 21.1.249, Create 6.0.10, FTB Teams 2101.1.11, FTB Chunks 2101.1.22, FTB Library 2101.1.37, Xaero Minimap 26.4.2 und World Map 1.45.0. `long_range_tracker_interval: 0` und private Team-Visibility beibehalten, bis die gegnerische Karten-Datenschutzpruefung abgeschlossen ist.

Historische Stande bleiben im [Archiv](archive/). Exakte Implementierungsschritte der v0.8.1-Wegpunktsynchronisierung stehen unter [v0.8.1](releases/v0.8.1.md).
