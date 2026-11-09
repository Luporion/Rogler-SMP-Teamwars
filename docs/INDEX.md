# Dokumentationsindex – Rogler Core

**Hier starten:** [Projektstatus](PROJECT_STATE.md) → [Roadmap](ROADMAP.md) → [Testmatrix](TEST_MATRIX.md). Alle drei Dateien sind **aktuelle** Dokumente; ältere Stände befinden sich bewusst im Archiv.

## Aktive Referenzdokumente

- [Control-Center-UI-Handbuch](UI_CONTROL_CENTER.md) – Spieler, Teams, Admin-Regler, sichere Aktionen und Bedienung.

| Datei | Warum lesen? |
|---|---|
| [PROJECT_STATE](PROJECT_STATE.md) | Nachgewiesener Entwicklungsstand, Build-/Runtime-Status und Blocker |
| [ROADMAP](ROADMAP.md) | Ziele, Priorisierung, noch fehlende Funktionen |
| [TEST_MATRIX](TEST_MATRIX.md) | Abnahmefälle und Nachweisformat |
| [ARCHITECTURE](ARCHITECTURE.md) | Struktur/State-/Adaptergrenzen (historische Aussagen nach Status prüfen) |
| [CORE_DASHBOARD_ARCHITECTURE](CORE_DASHBOARD_ARCHITECTURE.md) | Core-UI-Architektur |
| [DECISIONS](DECISIONS.md) | Architekturentscheidungen |
| [MOD_REQUIREMENTS](MOD_REQUIREMENTS.md) | Laufzeit-/Build-Abhängigkeiten und Modversionen |
| [MODPACK_BASELINE](MODPACK_BASELINE.md) | Ursprüngliche Modpack-Basis, nicht aktuelle Live-Modliste |
| [SETUP_AND_DEPLOY](SETUP_AND_DEPLOY.md) | Update-, Build-, Test- und Rollbackablauf |
| [Registrate-CI-Guide](guides/REGISTRATE_CI_BOOTSTRAP_v0.7.6.md) | SHA-geprüfter Build-Fallback für Create/Registrate |

- [FTB-Claim-Map/Configs v0.8.7](guides/FTB_CLAIM_MAP_v0.8.7.md) – exakte zu prüfende Server-/Client-Pfade, Private-Claims und Sync.

## Release-, Planungs- und Forschungsunterlagen

- [v0.8.7 Release](releases/v0.8.7.md) – Team-Exploration inkrementell, Claim-Audit/Client-Resync, fehlgeschlagene Snapshots.
- [v0.8.6 Release](releases/v0.8.6.md) – serverseitiger Core-Transporttimer unabhängig vom Lagerort.
- [v0.8.5 Release](releases/v0.8.5.md) – HUD per Maus verschieben, Admin-Teamdetails und Aktionssuche.
- [v0.8.4 Release](releases/v0.8.4.md) – Control Center 2.0, Team-/Core-/Raid-Ansichten, Audit und Aktionsschutz.
- [v0.8.3 Release](releases/v0.8.3.md) – Create/C2ME Chunk-Speicher-Hotfix, Admin-Suche.

- [v0.8.1 Release](releases/v0.8.1.md) – bestätigte Move/Delete-Behandlung und optionale lokale Metadaten-Automatik.

- [v0.8.2 Release](releases/v0.8.2.md) · [v0.8.1 Release](releases/v0.8.1.md)
- [v0.8.0 Release](releases/v0.8.0.md) – private Wegpunkt-Identitäten und sichere Bestätigungsbefehle.

- [v0.7.7 Release](releases/v0.7.7.md) · [v0.7.6 Release](releases/v0.7.6.md) · [v0.7.5 Release](releases/v0.7.5.md) · [vollständige Versionshistorie](releases/)
- [Pläne](plans/) – damalige Implementierungspläne, **keine** automatisch aktuelle Roadmap.
- [Forschung](research/) – FTB-/Xaero-/Core-API-Spikes und Feasibility.
- [Verträge](contracts/) – Basis-/Explorationskontrakte.

## Archiv: unverändert nachvollziehbare Historie

- [Frühere Versions-READMEs](archive/readmes/)
- [Handovers / LLM-Übergaben](archive/handovers/)
- [Versionsgebundene Projektstatusberichte](archive/project-states/)
- [Versionsgebundene Testpläne](archive/test-plans/)
- [Historische Mod-Anforderungen](archive/mod-requirements/)
- [Migrationen](archive/migrations/)
- [Vollständige Originalfassungen der bereinigten aktiven Dokumente](archive/legacy/)

**Archiv-Regel:** Historische Datums-/Versionsaussagen bleiben als Beleg für ihren jeweiligen Stand stehen. Sie dürfen **nicht** als heutige Release-Freigabe gelesen werden. Bei Funktions-/Prioritätsänderungen nur die aktiven Dateien aktualisieren und pro Release eine eigene unveränderliche Notiz ablegen.
