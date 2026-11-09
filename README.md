# Rogler Core / Rogler TeamWars

Minecraft **1.21.1 · NeoForge 21.1.249** — eigener TeamWars-Mod für ein teamorientiertes Rogler-SMP.

> **Entwicklungsstand v0.8.7 (Quellcodekandidat):** gezielte Performance- und Stabilitätskorrekturen nach der Serverdiagnose vom 9.11.2026. Team-Erkundung wird für stabile FTB-Mitgliedschaften inkrementell abgeglichen statt vollständig neu aufgebaut; der FTB-Auto-Claim-Audit läuft alle fünf Minuten statt jede Minute und protokolliert unveränderte Ergebnisse nur einmal. Fehlgeschlagene Exploration-Snapshots behalten ausstehende Änderungen. Ein expliziter Admin-Claim-Sync aktualisiert jetzt zusätzlich die FTB-Claim-Ansicht **des berechtigten Spielers** (FTB-API, kein garantierter Client-Bugfix). v0.8.6 ist am 9.11.2026 in CI erfolgreich gebaut worden (P0 `42692408480`, NeoForge `42692408891`, Commit `cc5a5b86`); v0.8.7 benötigt eigenen NeoForge-Build und Minecraft-Laufzeittest. Keine neuen Pflichtmods, kein Save-Schemawechsel. [Release](docs/releases/v0.8.7.md) · [FTB-Claim-Karten-Guide](docs/guides/FTB_CLAIM_MAP_v0.8.7.md) · [Status](docs/PROJECT_STATE.md).

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
