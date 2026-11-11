# Testmatrix – aktueller Stand v0.8.8

Stand: 11. November 2026. Java-/Adaptertests, vollständiger NeoForge-Build und Minecraft-Laufzeit bleiben getrennte Nachweise.

## Bereits bestätigte Basis

| Bereich | Evidenz | Status |
|---|---|---|
| v0.8.7 Build/Installation/Start | Terminal und Serverdiagnose für Commit e22eecb9 | Erfolgreich |
| Claims und Chunkloading-Markierungen auf beiden Karten | Nutzerbestätigung 10.11. | Erledigt |
| Weitere vorhandene Funktionen | Nutzer: weitere Tests grundsätzlich erfolgreich | Alltagsfunktion bestätigt; kein Einzelprotokoll jedes Randfalls |
| Leerlauf nach Startphase | JFR, ungefähr 1,8 ms mittlere Tickzeit | Baseline; kein Vergleich unter Spielerlast |

## v0.8.8-Prüfungen

| Prüfung | Automatisierte Abdeckung | Minecraft-Abnahme |
|---|---|---|
| Own-Team-Scope / manipuliertes Ziel | DiagnosticPolicyTests; echte Service-Tests mit Teamwechsel und Widerruf zwischen Scope-Lesen und Versand | Nicht-OP: eigene Seite; Teamwechsel; manipulierte claims/action-Anfrage verweigert |
| OP2/OP4-Diagnose | Bestehende Admin-Policy, neue Report-Grenzen | OP2 sieht ausgewähltes Team; verliert nach Entzug der Rechte den Berichtszugriff |
| Read-only FTB-Adapter | Echter Reader gegen FTB-Vertragsdouble mit Mutationsfallen; fremde Details, fehlende API, harte Scanlimits | Welt ohne neue Claims/Tickets/Dateireparaturen durch reine Abfrage |
| Markiert vs. FTB-Regel vs. geladen | Adaptertest mit markiert=true, API=false, geladen=true | Vergleich mit FTB-Markierung; entladene Corezone wird durch Status nicht geladen |
| Teamgesamtzahlen/Quoten | Kleines Dateninventar, negative/unbekannte Limits, >512 Claims ohne Iteration | Mit aktuellen FTB-Teamlimits vergleichen; bei großer Claimmenge eindeutig unbekannt |
| Berichtsseiten | Grenzen/Seitenmodell; bounded UTF-Felder | Kleine und große GUI-Skala, vor/zurück, Tooltip, Resize, erneutes Aktualisieren |
| Eigener Resync | Frischer Team-Scope, vorhandene Daten, Cooldown; bestehender Admin-Sync bleibt | Normales Teammitglied erhält nur seine vorhandenen Claims; keine neue Claim-Reparatur |
| Admin-Befehlsdetails | Bestehende Allowlist; echter Service/Codec gegen Stubs; lange Ausgabe, Paging und Logout getestet | diagnostics/coreaudit/claimaudit mit tatsächlichen Textdetails statt nur Code |
| Protokoll/Metadaten | Protokoll-4-Guard; Gradle/Modkonstante/TOML-Abgleich | Server und Clients gemeinsam 0.8.8; Start/Join ohne Payloadfehler |
| Bestehende Regressionen | Gesamte Core-Serie, Timer, inkrementelle Exploration, Create-Save und HUD-Stubs | Keine gezielte Wiederholung ohne geänderten Pfad oder neuen Befund |

Beobachtete Ausführungsergebnisse stehen in [Release v0.8.8](releases/v0.8.8.md) und dem mitgelieferten Testprotokoll. Minecraft-UI, echte modifizierte Clients und Server-Langzeitbetrieb können nicht durch Quellcodechecks ersetzt werden.

## Gezielter erster Lauf mit v0.8.8

1. Server und Testclients auf dieselbe neue JAR aktualisieren.
2. Als Nicht-OP `O` → **Claims & Chunkloading**: Team, vorhandene neun Claims und FTB-Quoten vergleichen; mehrere Seiten ansehen.
3. Ohne neue Claims anzulegen den eigenen Karten-Resync verwenden.
4. Als OP2 **Admin: Teams/Cores** → Team auswählen → **Claims & Chunkloading für dieses Team**. Anderes Team prüfen.
5. **Admin: Diagnose** → FTB-Adapter bzw. Core-Audit ausführen; tatsächliche Rückmeldungen im Bericht prüfen.
6. Kleine GUI-Skala, Logout/Rejoin und Serverneustart prüfen. Bei Fehlern Client-/Serverlog und CraftProbe-Export sichern.

FTB-Kartenanzeige und Grundfunktionen sind auf v0.8.7 bereits bestätigt und werden nicht erneut als ungelöst geführt.
