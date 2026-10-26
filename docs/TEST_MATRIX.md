# Testmatrix – verbindliche Release-Gates

**Stand v0.8.2; Spalten niemals vermischen:** `Regeltest` (Java), `Build` (NeoForge CI), `Laufzeit` (Minecraft DEV mit realen Mods) und `Produktiv` (nach Freigabe). Stand der vorhandenen Evidenz: [PROJECT_STATE.md](PROJECT_STATE.md). Historischer Stand im [Archiv](archive/legacy/TEST_MATRIX_before_cleanup_2026-10-22.md).

| Bereich / Akzeptanzfall | Automatisierte Tests | Minecraft-DEV-Nachweis | Freigabegate |
|---|---|---|---|
| CI-Abhängigkeiten/Registrate SHA-256 | Bootstrap-Test vorhanden | N/A | Bootstrap und `compileJava` grün |
| v0.8.2 UI/Keybind `/roglerui` | ControlCenterPolicyTests 107 Checks + statische Privacy-Guards | Noch offen | OP2/OP4/Nicht-OP, O-Taste, unterschiedliche GUI-Skalierungen |
| Live-Settings/Persistenz | Server-Schema + Property-Validation, NeoForge-Integration offen | Noch offen | 24 Werte, ungültige Kreuzkombinationen, Restart, Audit, FTB Limit-Hinweis |
| Admin-UI Aktionen | Whitelist-Pfade + Servercommand-Permissions | Noch offen | Grace, Raid, Core-Recovery, Repair-Confirm/OP4, Chat-Feedback |
| Minecraft Start / Join | Compiler/Packet-Guards alleine unzureichend | v0.7.4 Join bestätigt; **v0.8.1 offen** | 2 Spieler, Serverlog ohne Encoder/Mixin-Ausnahmen |
| FTB Party-Beitritt/Austritt/OP | Regeltests vorhanden | Unvollständig | Auch OP keine fremden Marker/Koordinaten |
| FTB Long-Range / Claim-Marker | Konfiguration prüfbar | Offen | Gegner mit aktueller FTB-Sichtbarkeit und anderen HUDs unsichtbar |
| HUD aus per Default; Respawn-/Worldspawn-/Basisdistanz | v0.7.5 12 Policychecks | Offen | Clientmenü, Relog, Dimension, keine Chatüberlagerung |
| Xaero Team-/Coremarker | v0.7.5 Quellcode vorhanden | Offen | Minimap und World Map, Keine Fremdteams |
| Waypoint Privatimport FTB→Xaero, Xaero→FTB | v0.7.6 14 Policychecks inkl. Sharing | Offen | Erstellen nur fehlender Punkte, keine Duplikate, Backup-/Reboot-Persistenz |
| v0.7.7 Privat-Abgleich/Konflikte | 27 neue Policychecks und statische API-Guards | Offen | Vorschau read-only, korrektes Dimension-Gating, Token bei Änderung ungültig, keine unautorisierten Löschungen |
| v0.8.0 persistente private Wegpunkt-Links | 29 Link- und 10 Journalprüfungen plus Privacy-Guards | Offen | Verknüpfen, Client-Neustart, eindeutige einseitige Änderung mit Token, keine Wiederherstellung gelöschter Links, defektes Journal blockiert Import |
| v0.8.1 Move/Delete/Auto-Metadaten | Neue 14 Policychecks, statische Schutzchecks | Offen | FTB→Xaero / Xaero→FTB, stale Token, Ambiguität, defektes Journal, Persistenz nach Reconnect; Auto nur Name/Farbe, niemals Delete/Move |
| Individuelles Teilen/Widerrufen, Teamaustritt | Team-Scope-/Ownership-Tests | Offen | Anzeige A, keine B, nach Widerruf/Wechsel sofort weg |
| Geteilte Gelände-Erkundung und Cache-Widerruf | Union/Revocation-P0-Tests | **Funktion nicht fertig** | Nicht als umgesetzt markieren |
| Core platzieren/transportieren/retten | Java-/Ledger-Tests | Teilweise | 1h, Außenrand, Lava, Escrow, Reload, Inventar voll |
| Claims/CoreGuard, Sprengstoffe/Sable | Regel-/Guard-Tests | Lücken | Raid-/Explosionstest inkl. anderen Mods und Weltbackup |
| Raids und Kriegspunkte | Policytests | Nicht umfassend | Einmalige Punkte, Grace, Offline-Schutz, Ersatzcore |
| Create FE/Kinetik/AE2/Sophisticated | Teilfunktionstests | Integration ungeprüft | Kein unerlaubter Energie- oder Inventarzugriff |
| Chunkloading/Performance | Quoten-/Guard-Regeln | Lasttests offen | 8–12 Spieler, Flug/Chunkgeneration TPS/MSPT/Spark messen |

## Nachweisvorlage für jedes Release

```text
Version / Branch / Commit:
Modpack- und Loader-Versionen:
Core-Regeltests (Befehl + Ergebnis):
NeoForge Actions (Run-ID + Ergebnis):
Dedicated-Server Join (Log-Zeitstempel):
Client A (Team, OP): / Client B (Team):
Gameplaytestfälle mit Screenshots/Logs:
Fehler/Regressionsrisiken:
Backup-/Rollback-Stand:
Freigabe (DEV / Produktion / gesperrt):
```

Bei relevanten neuen Mod-JARs API-Signaturen an den tatsächlich installierten Versionen erneut prüfen. Keine produktive Freigabe durch einen reinen `git am`-/Java-Test.
