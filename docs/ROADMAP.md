# Rogler Core – Roadmap und Versionsplan

Stand: 12. November 2026 · Buildbasis: **0.8.8** · Aktuelles Release: **0.8.9**

Diese Roadmap ersetzt die inhaltlich überholte Planung vom 9. November. Sie beschreibt den aktuellen Projektstand und die geplanten nächsten Releases. Die ersten drei Releases sind konkret zugeschnitten; die späteren Versionen sind Zielzuordnungen, die bei neuen Befunden angepasst werden können. Es werden keine Kalendertermine zugesagt.

## 1. Aktueller Stand

| Bereich | Aktueller Stand | Konsequenz für die Planung |
|---|---|---|
| Build und Installation | v0.8.8: Beide GitHub-Workflows für `dc4053c0` erfolgreich. v0.8.9: lokaler Build und Testserver erfolgreich. | Die Prüfung mit verbundenen Clients und dem vollständigen Modpack steht für v0.8.9 noch aus. |
| Claim- und Chunkloading-Anzeige | Nutzer bestätigt am 10.11.: geclaimte und gechunkloadete Chunks auf beiden Karten sichtbar | Das bisherige Anzeigeproblem ist gelöst. Kein erneuter Karten-Hotfix ohne neuen Fehler. |
| FTB-Claims | Im ausgewerteten Lauf zwei Teams mit jeweils neun bestehenden Claims, keine abgewiesenen Claims | Bestehende Claims erhalten; keine Neuanlage oder Datenbereinigung als Standardmaßnahme. |
| Weitere Funktionstests | Nutzer bestätigt: „Die anderen Sachen haben auch soweit funktioniert.“ | Grundfunktionen als im Alltag grundsätzlich erfolgreich behandeln. Diese Rückmeldung ersetzt kein detailliertes Protokoll jedes Sonderfalls. |
| Core-Kartenlogik | v0.8.7 enthält inkrementelle persönliche Erkundungsrevisionen, begrenzte Änderungsjournale, vollständigen Abgleich bei Teamwechsel und Speicher-Retry | Bestehende Optimierung weiterverwenden. Keine Neuimplementierung derselben Funktion. |
| Gemeinsame Xaero-Erkundung | Eigenständiger XMMP-FTBTeams-Fork ist bereits gebaut; `xmmp-0.4.2+1.21.1-neoforge.jar` im ausgewerteten Serverlauf installiert | Die alte Aussage „Fork erst später beginnen“ ist überholt. Das Zusammenspiel von XMMP und Core wird konsolidiert. |
| UI und Administration | Control Center, Team-/Core-Ansichten, HUD-Positionierung, validierte Konfigänderungen und Adminaktionen sind im aktuellen Quellstand vorhanden | Gezielt vervollständigen; kein neues Menüsystem entwickeln. |
| Core-Technik | FE-Ausgabe, Starter-Kinetik für Create, Team-Inventar und Upgrades sind im Quellstand vorhanden | Vorhandene Basis verbessern. Ein FE-Anschluss an AE2 ist noch kein eingebautes ME-Terminal oder ME-Drive. |
| Core-Chunkloading | Reines Java-Quotenmodell vorhanden; im Quellarchiv nur im Modell und dessen Tests referenziert | Sichtbares FTB-Chunkloading ist vorhanden. Ein eigener Core-Adapter mit getrennten Quoten ist noch ein geplanter Ausbau. |
| Leistung | Im ausgewerteten Lauf ohne Spieler später etwa 1,8 ms mittlere JFR-Tickzeit; zwei Überlastmeldungen kurz nach dem Start | Gute Leerlaufbasis. Daraus lässt sich keine konkrete Ersparnis beim Erkunden mit Spielern berechnen. |
| Speichern und Shutdown | Alle Dimensionen gespeichert; anschließend zwei verbliebene Threads gemeldet, darunter Tan’s Huge Trees | Separates Modpack-Thema weiter eingrenzen; bislang kein Nachweis eines Rogler-Core-Shutdownfehlers. |

**Wichtig:** Claim, Forceload-Markierung, tatsächlich geladener Chunk und laufende Maschinensimulation sind unterschiedliche Zustände. Die Anzeige ist jetzt bestätigt. Das ist keine Behauptung, dass weiterhin ein Chunkloading-Fehler besteht; die Unterscheidung soll künftige Diagnosen verständlicher machen.

## 2. v0.8.8 bis v0.9.0

### v0.8.8 – Claim-/Chunkloading-Status und verständliche Diagnose

**Implementiert im aktuellen Quellstand.** Autorisierte eigene/OP-Berichte, begrenzte Corezone, Teamquoten soweit lesbar, eigener Karten-Resync und echte Befehlsrückmeldungen. Protokoll 4 erfordert Client-/Server-Update zusammen. Build-/Testergebnis und Grenzen siehe [Release](releases/v0.8.8.md).

**Ziel:** Im bestehenden Control Center erkennen, was an der eigenen Basis aktiv ist und warum eine Funktion gegebenenfalls nicht arbeitet.

Geplanter Umfang:

- Eigene Team-/Core-Detailseite um Claimzahl, FTB-Forceload-Status, verfügbare Quoten und tatsächlich geladenen Zustand ergänzen, soweit die installierte FTB-API diese Informationen zuverlässig bereitstellt.
- Unterscheiden, ob ein Status aus einer Core-Regel, einer FTB-Einstellung oder einer momentan nicht verfügbaren Integration stammt. Fehlende Informationen als „unbekannt“ anzeigen.
- Bestehenden berechtigten Claim-Resync direkt erreichbar machen und Erfolg bzw. Fehler im Menü verständlich zurückmelden.
- Ausgewählte bestehende Diagnosebefehle als strukturierte Ergebnisse darstellen, statt nur ihren Rückgabecode oder einen Chatverweis zu zeigen.
- Aktuellen Build-, Installations- und Nutzer-Teststand in `PROJECT_STATE`, `ROADMAP`, `TEST_MATRIX` und den relevanten Anforderungs-/UI-Dokumenten berichtigen. Historische Versionsnotizen bleiben Historie.
- Nur konkret reproduzierte Core-Fehler als begleitende Korrektur aufnehmen.

**Abnahme:** Eigener Claim-/Forceload-Status stimmt mit FTB und den Karten überein. Nicht-OP sieht keine fremden Basisdaten; bestehende OP-Rechte bleiben erhalten. Statusabfragen laden keine zusätzlichen Chunks und verändern weder Claims noch Tickets. Große Listen werden begrenzt und seitenweise geladen.

**Abgrenzung:** Dieses Release ergänzt die Diagnose. Es ersetzt weder FTB Chunks noch die inzwischen funktionierende Kartenanzeige und führt noch keine zusätzliche Core-Ticketverwaltung ein.

### v0.8.9 – Teamverwaltung und wirksame Teamregeln

Umgesetzt sind Rollen, Einladungen, Mitgliederaktionen und lesende Admin-Ansichten für alle FTB-Party-Teams. Die optionalen Core-Grenzen werden bei Teamerstellung, Einladung und Beitritt in FTB durchgesetzt. Standardmäßig sind sie aus. Ein lokaler NeoForge-Serverlauf hat die Grenzen und den Fortbestand bestehender Mitglieder nach einer Limitsenkung bestätigt. Der Client-Test steht noch aus. [Release v0.8.9](releases/v0.8.9.md).

### v0.9.0 – Kartenintegration konsolidieren

**Ziel:** Die bereits vorhandenen Kartenfunktionen aus Rogler Core und XMMP als zusammenhängendes System bedienen und warten können.

Geplanter Umfang:

- Vorhandenen XMMP-FTBTeams-Fork weiterführen. XMMP-Versionen werden separat nummeriert; eine Core-Version bedeutet nicht automatisch eine gleichnamige Fork-Version.
- Zuständigkeiten eindeutig festlegen: FTB Teams für Mitgliedschaft, FTB Chunks für Claims/Tickets, Core für eigene Team-/Core-Regeln, XMMP für den tatsächlichen Geländeabgleich in Xaero.
- Im Control Center den autoritativen Freigabemodus, Verbindungs-/Abgleichstatus und relevante Versionskonflikte anzeigen. Keine zweite unabhängige Einstellung für denselben Freigabemodus einführen.
- Doppelte Team-/Basis-/Wegpunktmarker und widersprüchliche Synchronisierungspfade gezielt beseitigen, falls sie reproduzierbar auftreten.
- Globale Geländeweitergabe wegen des PvP-Ziels deutlich kenntlich machen und als bewusste Adminentscheidung behandeln; Teamfreigabe bleibt der vorgesehene Standard.
- Startabgleich und Nachlieferung vorhandener Daten mit Mengen- und Zeitbudgets begrenzen. Vorher-/Nachher-Messung mit vergleichbarer Spielerlast aufnehmen.

**Abnahme:** Zwei Teams, überlappende und getrennte Erkundung, Austritt, Wechsel, Rejoin, Dimensionswechsel und Neustart. Eigene sowie später selbst erkundete Bereiche bleiben erhalten; ausschließlich fremde Bereiche werden gemäß Freigaberegel entzogen. Beide Xaero-Oberflächen bleiben konsistent. Vorhandene persönliche Kartendaten werden nicht blind überschrieben.

Bereits funktionierende Grundabläufe gelten als Ausgangspunkt. Der Schwerpunkt liegt auf klaren Zuständigkeiten, Statusanzeigen und verbleibenden Randfällen, nicht auf einem Neustart des Kartenprojekts.

## 3. Weitere Versionen

Die folgenden Nummern sind die geplante Reihenfolge, keine bereits begonnenen Implementierungen. Jede Version soll eine klar abgegrenzte Funktion liefern.

| Version | Schwerpunkt | Geplanter Nutzen | Wichtigste Abnahme |
|---|---|---|---|
| **0.9.1** | Teamfunk | Bestehendes Simple Voice Chat um teamgebundenen Funk ohne Distanzlimit ergänzen; Proximity für Begegnungen mit Gegnern erhalten; Bedienung im Control Center | Zwei Teams, Kanalwechsel, Teamwechsel, Logout/Rejoin; keine fremde Funkteilnahme. Verhalten bei gleichzeitigem Funk/Proximity ausdrücklich festlegen. |
| **0.9.2** | Core-Chunkloading mit eigenen Quoten | Vorhandenes Quotenmodell an eine geprüfte FTB-Integration anschließen; Core-Budget und manuelle FTB-Quote getrennt verwalten; Ursache aktiver Core-Tickets anzeigen | Basisumzug, Neustart, Coreverlust und Offlinebetrieb; nur selbst verwaltete Tickets entfernen, keine doppelten oder verwaisten Tickets; Create Power Loader und Compact Machines berücksichtigen. |
| **0.9.3** | Core-Technik und Bedienung | Vorhandene FE-/Create-Funktionen verbessern: konfigurierbare Ausgabe, nachvollziehbare Leistungsgrenzen und kleine, bezahlte Ausbaustufen | FE-Gesamtbudget pro Tick, Create-Netzänderungen, Chunk-Unload und C2ME-Speicherung; UI zeigt reale Fähigkeiten. Höhere SU-/FE-Werte erst nach Balancing. |
| **0.9.4** | AE2-Modul | Optionales Core-Modul mit klar definiertem ME-Zugang; Terminal-/Speicherknoten nur über geprüfte AE2-APIs und reale Energie-/Kanalregeln | Teamrechte, Netzwerkunterbrechung, Core-Umzug, Save/Reload und Inventarkonsistenz. Keine Behauptung eines eingebauten ME-Drives allein wegen vorhandener FE-Ausgabe. |
| **0.9.5** | Sophisticated-Modul | Ausgewählte Upgrade-Slots und begrenzte Lagerfunktionen am Core über eine geprüfte Integration | Erlaubte Upgrades, Automation, Teamwechsel, Umzug und Wiederherstellung; keine Itemduplikation und kein fremder Inventarzugriff. |
| **0.10.0** | Kontrollierter Teamwettkampf | Bereits vorhandene Raid-/Punkte-/Grace-Logik zu einem klaren spielbaren Regelwerk abschließen: Ankündigung, Raidfenster, Offline-Regeln, genau ein Punkt je gültiger Core-Zerstörung und Wiederaufbau | Zwei echte Teams; relevante Sprengstoffe/Automationswege; wiederholte Schadensereignisse und Neustart dürfen keine zusätzlichen Punkte oder Cores erzeugen. |
| **1.0.0** | Konsolidierte Spielversion | Stabiler, dokumentierter Stand für Teams, Karten, Core, Funk und den freigegebenen Wettbewerbsmodus; konsistente UI und einfache Administration | Gemeinsamer Mehrspieler-Dauerlauf, Update-/Backup-/Restore-Probe und verständliche Einstellungen. Optionale Technikmodule nur Teil der Freigabe, wenn sie ihre eigene Abnahme bestehen. |

Wenn eine Drittmod-API einen erheblichen Umbau verlangt, wird das betroffene Modul verschoben oder in ein kleineres Folgerelease geteilt. Andere abgeschlossene Funktionen werden dadurch nicht erneut als „ungeprüft“ zurückgestuft.

## 4. Aufgehobene Vorhaben bleiben im Backlog

„Später“ bedeutet weiter geplant, nicht gestrichen:

| Vorhaben | Geplante Einordnung |
|---|---|
| Item-gegen-Item-Markt und Team-Postfach | Nach stabiler Basis, voraussichtlich 1.1.x; Transaktions-IDs, serverseitige Verwahrung, Neustartkonsistenz und Recovery zuerst. |
| Gebühren für Teamumbenennung, Verteilung an andere Teams | Mit der Wirtschaftslösung; keine zweite Bezahl- oder Währungslogik vorab. |
| Events | Danach, voraussichtlich 1.2.x; zunächst ein klar begrenzter Modus, etwa Sektorkontrolle oder Supply Drops. CTF, Konvois und Bossziele folgen einzeln. |
| Radar und Pflanzenboni | Optionale Core-Module nach Last- und Datenschutzprüfung; begrenzte Reichweite und Arbeitsbudgets, keine allgemeine Tickbeschleunigung. |
| Interaktives Claiming/Forceloaden direkt in Xaero | Eigener Ausbau nach Statusanzeige und Ticketintegration; vorhandene Overlay-Anzeige bleibt nutzbar. |
| Weitergehender Wegpunktkomfort | Bestehenden sicheren Sync ausbauen; automatische Verschiebung/Löschung nur mit belastbarer Konflikt- und Wiederherstellungslogik. |
| Spawn-Hub, Zonen und optionale Webkarte | Als eigene Spielwelt-/Admin-Erweiterung; private Basen und Kartendaten nicht unbeabsichtigt veröffentlichen. |
| Mobile Sable-/Create-Basen | Eigenständiger experimenteller Zweig; dynamische Core-Position, Docking, Restart, Claims und Zonenzeit zuerst zuverlässig lösen. |
| Saisonfinale und erweiterte Wettbewerbsevents | Nach einem im Alltag funktionierenden Raid-/Punktesystem. |

## 5. Parallel: Modpack-Betrieb

Diese Punkte bleiben relevant, sind aber keine automatisch erforderlichen Core-Features:

- **Shutdown-Threads:** Im nächsten gezielten Lauf Herkunft der verbliebenen Threads erfassen. THT-/Executor-Aufräumen im zuständigen Projekt oder der verursachenden Mod beheben, sobald die Ursache feststeht.
- **LSS/Voxy:** Im ausgewerteten Lauf wurde der abgeleitete LOD-Speicher nach Registry-Änderung neu aufgebaut und mit null Fehlern abgeschlossen. Beim nächsten Start Wiederverwendung kontrollieren. `generation.enabled: false` bleibt die Vorgabe; LOD-Anfragen dürfen keine neuen Chunks erzeugen.
- **Kartendatenschutz:** Prüfen, ob die zuletzt empfohlene LSS-Änderung `far_players.mode: "off"` inzwischen gesetzt ist. Der vorliegende Export hatte noch `"on"`; aus der jetzigen Rückmeldung folgt keine neue Konfigbestätigung. Keine erneute Warnschleife nach erfolgreicher Prüfung.
- **Leistungsvergleich:** Nur bei vergleichbarer Spielerzahl, Bewegung, geladenen Maschinen und LOD-/Pregen-Aktivität vergleichen. Die bisherige Leerlaufmessung bleibt eine Baseline, kein Nachweis einer prozentualen Verbesserung.
- **Sonstige Lade-/Rezeptwarnungen:** In der allgemeinen Modpack-Optimierung behandeln; nur mit belegtem Bezug in ein Core-Release aufnehmen.

## 6. Entwicklungs- und Abnahmeregeln

1. **Aktuelle Basis erhalten:** Vom zuletzt bestätigten Stand aus weiterarbeiten. Bestehende Claims, Karten und Speicherstände erhalten.
2. **FTB Teams bleibt Autorität:** Mitgliedschaft und Rechte werden serverseitig anhand der tatsächlichen IDs geprüft. Neue Menüs sind kein zweites Teamsystem.
3. **Pro Release ein Hauptziel:** Modell-/Regressionstests, NeoForge-Build und relevante Minecraft-Abnahme separat festhalten. Vorhandene Nutzerbestätigungen erhalten und nicht bei jedem Release pauschal wieder öffnen.
4. **Aufwand begrenzen:** Diagnose und Abgleich dürfen keine unnötigen Chunkladungen, großen globalen Scans oder unbeschränkten Netzwerkpakete verursachen.
5. **Datenänderungen nachvollziehbar machen:** Vor Migrationen Backup; tatsächliche Schemaänderungen und Rückkehrmöglichkeiten dokumentieren. Keine fremden Claims, Tickets oder persönlichen Karten überschreiben.
6. **Lieferung wie gewohnt:** Bei einer Implementierung exakter Git-Patch im ZIP, vollständiges Quellarchiv, Befehle, Checks und passende Release-Dokumentation. Diese Roadmap selbst enthält keine neue Mod-JAR und ändert keine Spielwelt.
7. **Abhängigkeiten funktionsbezogen halten:** Vorhandene Mods nutzen; neue Pflichtmods oder Versionssprünge erst nach konkreter API-/Kompatibilitätsprüfung festlegen.

## 7. Quellen und Statusgrenzen

- Exakter bereitgestellter v0.8.7-Quellstand: `Rogler-Core-v0.8.7-full-source-exact.zip`, insbesondere `docs/ROADMAP.md`, `docs/PROJECT_STATE.md`, `docs/UI_CONTROL_CENTER.md`, `docs/CORE_DASHBOARD_ARCHITECTURE.md`, `ChunkLoadingBudget` und die Core-/FTB-Adapter.
- Terminalexport vom 12.11.2026: beide Workflows für Commit `dc4053c0` erfolgreich.
- Terminalexport vom 10.11.2026: erfolgreiche Workflows für Commit `e22eecb9`, heruntergeladenes v0.8.7-Artefakt.
- Ausgewertete CraftProbe-Sitzung: 09.11.2026, 23:44 Uhr bis 10.11.2026, 00:30 Uhr, einschließlich Modliste, Logs und JFR. Dieser Export ist kein Live-Abzug des späteren Serverstands.
- Nutzerbestätigung vom 10.11.2026: Claim-/Chunkloading-Markierungen auf beiden Karten sichtbar; weitere Tests grundsätzlich erfolgreich.

Die zukünftigen Versionen sind Planung. Sie sind weder bereits gebaut noch als vollständig implementiert dargestellt.
