# Rogler Core / Rogler TeamWars

Minecraft **1.21.1 · NeoForge 21.1.249** — eigener TeamWars-Mod für ein teamorientiertes Rogler-SMP.

> **Entwicklungsstand v0.8.6 (Quellcodekandidat):** UI-Komfortrelease auf dem erfolgreich in GitHub Actions gebauten v0.8.4 (NeoForge Run `42483147984`, Commit `a085d62a`). Admin-Team-Detailseite mit serverseitig validierten Teamaktionen, durchsuchbare Aktionslisten, nicht verlorene Konfig-Entwürfe und ein clientlokaler HUD-Drag-and-drop-Editor. Der C2ME/Create-Watchdogfix aus v0.8.3 und die Schutzregeln aus v0.8.4 bleiben erhalten. **v0.8.5 benötigt einen eigenen CI- und Minecraft-Laufzeittest.** Keine neuen Pflichtmods oder SavedData-Migrationen. [Release](docs/releases/v0.8.5.md) · [Handbuch](docs/UI_CONTROL_CENTER.md) · [Status](docs/PROJECT_STATE.md).

## Hier beginnen

| Dokument | Zweck |
|---|---|
| [Dokumentationsindex](docs/INDEX.md) | Navigation zu Handbüchern, Versionen und Archiven |
| [Aktueller Projektstatus](docs/PROJECT_STATE.md) | Tatsächliche Implementierung, nachgewiesene Tests, Risiken und nächster Schritt |
| [Roadmap](docs/ROADMAP.md) | Ziele, Prioritäten und noch offene Funktionen |
| [Testmatrix](docs/TEST_MATRIX.md) | Verbindliche Release- und Datenschutzprüfungen |
| [Abhängigkeiten](docs/MOD_REQUIREMENTS.md) | Welche Modversionen erforderlich bzw. für Integrationen vorgesehen sind |
| [Setup / Deployment](docs/SETUP_AND_DEPLOY.md) | Entwicklung, GitHub Actions, Installation und Rollback |
| [Entwicklungsregeln](AGENTS.md) | Stabile Projektkonventionen und Sicherheitsgrundsätze |

## Kernstruktur

- `core/` – pure Java-Regeln und Tests, ohne Minecraft-/Mod-Abhängigkeiten.
- `mod-neoforge/` – NeoForge-Integration, Netcode, Minecraft-UI und Mod-Adapter.
- `scripts/` – Tests, PATCH/VERIFY, CI-Dependency-Bootstrap und Build-Monitor.
- `deployment/` – Installationsprofile und Xaero-Vorlagen.
- `docs/releases/` – historische Versionsnotizen; `docs/archive/` – frühere Handovers/Statusstände.

## Validierung

```powershell
.\VERIFY.cmd
# NeoForge-Abhängigkeiten und JAR in GitHub Actions bauen:
.\scripts\watch-actions.ps1
```

`PATCH.cmd` nur **einmal** pro neuem Git-Update-Patch und **nur auf dem angegebenen Ausgangsbranch** ausführen. Ein bereits vorhandener Feature-Branch ist kein Grund, denselben Patch erneut anzuwenden. Ein grüner CI-Build ersetzt keinen Test mit einem Dedicated Server und mehreren Clients.

Die ältere Versionsdokumentation und die ursprüngliche ausführliche README sind unverändert unter [Archiv](docs/archive/legacy/README_before_cleanup_2026-10-22.md) zugänglich. Diese Version ändert kein Server-SavedData-Schema und keine FTB-Team-/Core-Eigentumsdaten; die neuen privaten Wegpunktaktionen sind vor produktiver Nutzung im DEV-Profil zu testen.
