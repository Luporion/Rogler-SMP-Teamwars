# Rogler Core / Rogler TeamWars

Minecraft **1.21.1 · NeoForge 21.1.249** — eigener TeamWars-Mod für ein teamorientiertes Rogler-SMP.

> **Entwicklungsstand v0.8.3 (Quellcodekandidat):** Sicherheitsfix fuer den am 30.10.2026 beobachteten C2ME/Create-Core-Watchdog-Hang: Kein potentiell blockierender Welt-/Chunkzugriff mehr bei der BlockEntity-NBT-Serialisierung und physische Core-Owner-Pruefung nur aus geladenen Chunks. Das Rogler Control Center erhaelt eine Admin-Suche ueber 24 bestehende Rogler-Einstellungen. Version im JAR-Metadata wird durch CI geprueft. **v0.8.2 NeoForge-CI erfolgreich** (Run `40389304252`), v0.8.3 Build und Minecraft-Test noch offen. Keine neuen Pflichtmods, kein SavedData-Schemawechsel. [Control-Center-Handbuch](docs/UI_CONTROL_CENTER.md) · [Status](docs/PROJECT_STATE.md) · [Release](docs/releases/v0.8.3.md).

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
