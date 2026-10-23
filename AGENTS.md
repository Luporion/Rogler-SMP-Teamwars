# AGENTS.md – verbindliche Entwicklungsregeln für Rogler Core

**Aktueller Feature-Branch: v0.8.0 SOURCE CANDIDATE.** Client-private Wegpunkt-Verknüpfungen werden lokal und pro Serverprofil gespeichert. Keine automatischen Löschungen, Verschiebungen oder Team-Uploads. `/roglerwaypoints changes` zeigt konservative Konflikte. Siehe [Release v0.8.0](docs/releases/v0.8.0.md).

**Einstieg:** [Dokumentationsindex](docs/INDEX.md) → [Projektstatus](docs/PROJECT_STATE.md) → [Roadmap](docs/ROADMAP.md) → relevante [Release-Notiz](docs/releases/v0.7.7.md) und [Tests](docs/TEST_MATRIX.md). Das historische, ausführliche Regelwerk steht vollständig im [Archiv](docs/archive/legacy/AGENTS_before_cleanup_2026-10-22.md). Historische Versionsaussagen sind keine aktuellen Freigaben.

## Vor jeder Änderung

1. `git status --short`, Branch, letzte Commits und betroffene Dateien prüfen; niemals fremde Änderungen zurücksetzen.
2. Konkreten Umfang und Akzeptanztest festlegen; nach Möglichkeit bestehende Mod-APIs und versionierte Schnittstellen verwenden.
3. `core/` strikt ohne Minecraft, NeoForge, FTB, Xaero oder Create halten; serverseitige Autorität in `mod-neoforge/`.
4. Am Ende **beobachtete** Java-Tests, CI-Kompilierung und Minecraft-Laufzeit separat dokumentieren. Ein getestetes Regelmodell beweist keine Mixin-, Netzwerk-, Karten- oder GUI-Integration.
5. `docs/PROJECT_STATE.md` und, bei Änderung von Prioritäten, `docs/ROADMAP.md` pflegen; Release-Details in `docs/releases/vX.Y.Z.md`. Keine neuen Versionschroniken in README/AGENTS/PROJECT_STATE anhängen.
6. Neue Drittanbieter-Abhängigkeiten oder Versionserhöhungen nur geprüft und ausdrücklich dokumentiert; keine JARs in Git committen.

## Unverhandelbare Gameplay- und Datenschutzregeln

- FTB Teams ist die alleinige Teamzugehörigkeitsquelle (Party-/Spieler-UUIDs; nie Displaynamen als Eigentumsnachweis). 2–4 Teams sind geplant, Konfiguration allein garantiert kein Schöpfungslimit.
- **Spielersichtbarkeit, Claims, Basiskoordinaten und Wegpunkte müssen teamautorisiert bleiben.** Clientdaten, OP-Ausnahmen von Fremdmods und modifizierte Pakete sind kein Vertrauensanker. FTB Long-Range-Tracking nur nach Privacy-Audit aktivieren.
- Persönliche Exploration gehört dauerhaft einer Spieler-UUID und Dimension; sichtbare Team-Exploration = Vereinigung aktueller Mitglieder. Austritt widerruft fremde exklusive Bereiche, eigene und überlappende bleiben. Karten-/Client-Caches niemals unkontrolliert überschreiben; Chunky-Pregeneration ist keine Exploration.
- Primäre Spielerkarten: Xaero Minimap + World Map. FTB Chunks für Claim-/Ticket-System; Integrationen an tatsächlichen JAR-Versionen testen. Keine stillschweigende Verteilung aller privaten Waypoints.
- Basis-Core: eindeutiges serverseitiges Token; Overworld-Zone derzeit standardmäßig 0–5000 Blöcke, transportiert maximal 1h, sichere Platzierung oder Escrow. Keine Platzierung durch unsichere `setBlock`-Abkürzung und kein Löschen fremder/manueller Claims.
- Raids/Kriegspunkte, Sprengstoffe, Sable-Schiffe und Chunkloader nur nach Mehrspieler-/Wiederherstellungs- und Lasttests produktiv freigeben. Kein pauschales Ausschalten von Schutzvorrichtungen.
- Server ist Autorität für Handel, Gebühren, Core-Inventar, Freigaben und SavedData. Änderungen müssen backupfähig, idempotent und nach Neustart konsistent sein.

## API-/Build-Grenzen

- Minecraft 1.21.1 / NeoForge 21.1.249, Create 6.0.10, FTB Teams/Chunks und Xaero-Integrationen laut [Modanforderungen](docs/MOD_REQUIREMENTS.md).
- FTB Chunks `PlayerVisibilityPacket` verwendet **Architectury NetworkManager**, nicht direkten NeoForge-`PacketDistributor` für denselben Drittanbieter-Payload. Regression v0.7.3 führte zu Join-Fehler; v0.7.4 änderte den Versandweg.
- Keine falschen Versprechen: v0.7.7 hat manuelle, konfliktbewusste private, nur fehlende Waypoints importierende FTB↔Xaero-Synchronisierung; **keine automatische Änderungs-/Löschsynchronisierung** oder native widerrufbare Xaero-Geländekachelansicht.
- Aktueller Build-Fix: [Registrate-Bootstrap](docs/guides/REGISTRATE_CI_BOOTSTRAP_v0.7.6.md). SHA-256-Prüfung niemals umgehen, um CI künstlich grün zu bekommen.

## Lieferung und Sicherheit

- Pro Patch: Git-format-Patch für den exakten Ausgangsbranch und vollständiges `git archive`-ZIP; Änderungen, Quellenstand, Checks, Risiken und Rollback nennen.
- `PATCH.cmd` **nicht erneut** ausführen, wenn Zielbranch/Commit bereits erstellt ist; für Korrekturen auf existierendem Branch mit `git am`, `VERIFY.cmd`, Commit/Push arbeiten.
- Keine produktiven Saves, persönliche Xaero-Dateien oder Team-/Claim-Daten ohne Backup überschreiben. Diagnose-ZIPs mit tatsächlichen Ergebnissen dokumentieren; ein grüner GitHub Actions-Workflow allein ist kein In-Game-Test.
- Für ältere, release-spezifische Sonderregeln im unveränderten [AGENTS-Archiv](docs/archive/legacy/AGENTS_before_cleanup_2026-10-22.md) und unter `docs/archive/handovers/` nachschlagen.
