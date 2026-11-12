# Rogler Control Center – UI- und Admin-Handbuch

**v0.9.0** verwendet Protokoll 5. Die Seite **Kartenabgleich** zeigt den serverbestätigten XMMP-Status und öffnet das vorhandene XMMP-Menü. Unter **Mein Team → Teamverwaltung** stehen Teamleitung, Rollen, Mitglieder und Einladungen. OP2 findet auch Offline-Teams unter **Admin: Teams/Cores → Mitglieder**. OP4 kann unter **Admin: Teams** die Core-Teamlimits einschalten. [Teamverwaltung](releases/v0.8.9.md).

**O / `/roglerui` / `/roglerconfig`** oeffnen das Control Center. Bestehende `teamwars`-Befehle bleiben verfuegbar; das native Core-Inventar ist weiterhin separat am platzierten Core erreichbar.

## Navigation

| Kategorie | Funktion |
|---|---|
| Uebersicht | Spieler online, eigene FTB-Teamzuordnung und Links |
| HUD & Navigation | Schalter, Marker, HUD-Bereiche; ab v0.8.5 frei positionierbar per Drag-and-drop |
| Wegpunkte | Privater Sync, Teamfreigabe, sicherer Link-Abgleich und Auto Name/Farbe |
| Waypoint-Links | UUID-/Token-geschuetzte Apply/Acknowledge/Delete/Move/Unlink-Aktionen |
| Mein Team | Teamverwaltung, Kriegspunkte, Wegpunkte und Kartenabgleich |
| Kartenabgleich | XMMP-Verbindung, wirksame Freigabe, Client-/Serverversion und Core-Erkundungszähler |
| Claims & Chunkloading | Eigener serverautorisierter Snapshot: FTB-Claims, Quoten, Markierungen und residente Corezone; vorhandene eigene Kartendaten erneut senden |
| Mein Base-Core | Core-Menue, Status, Schutz, Energie, Transport, Recovery |
| Admin: Teams/Basis/Schutz/Energie | Live-Werte der vier bisherigen Rogler-Teamwars-Konfigdateien |
| Admin: Kompat | Rogler-eigene Warium-Recovery, Sable-Metriken und Lifecycle-Guards (OP4) |
| Admin: Teams/Cores | Paginierte Team-/Base-Core-Übersicht mit UUID, Online-/Mitgliederzahl, Grace, Raidstatus, Punkten, Koordinaten; Auswahl oeffnet Teamdetails |
| Admin: Raidsteuerung | Team-Auswahl aus der Liste, gesicherte Raidfenster ab OP4 |
| Admin: Status | Server-Zustand (Spieler, Cores, aktive Raids, Claims, Heap) ohne Chat |
| Admin: Audit-Log | Paginierte Chronik erfolgreicher bzw. abgelehnter UI-Adminaktionen und Einstellungsänderungen |
| Admin: Aktionen | Durchsuchbare autorisierte Core-/Team-Administration, Raids und Grace nach Eingabe eines Ziels; Rueckmeldung im UI |
| Admin: Diagnose | Durchsuchbare Diagnose-/Recovery-/Warium-/Sable-Aktionen; ein Kurzstatus kommt ins UI |

Ab v0.8.5: Die Detailseite eines aus der OP-Liste ausgewaehlten Teams bietet Claim-Audit, Team-Grace, Raidstart/-stopp und Team-Core-Widerruf, jeweils nur mit den bestehenden serverseitigen Rechten und Bestaetigungen. Die angezeigten Daten sind ein **Snapshot**; spaetere Aktionen werden auf dem Server erneut autorisiert. Die Anzeige ist keine neue FTB-Mitgliedschaftsverwaltung.

Admin-Aktionen mit Ziel verwenden im ersten Feld **Spielernamen (z. B. Alex)** oder **FTB-Team-UUID**, die Zahl daneben fuer Minuten. Fuer `adoptcore` benoetigt man zusaetzlich den neuen Spieler im zweiten Feld. Kritische Aktionen haben einen weiteren Bestaetigungsdialog. Seit v0.8.4 werden Adminaktionen mit festen IDs und streng geprueften Parametern serverseitig ausgefuehrt; die GUI zeigt Rueckgabestatus/Fehler. v0.8.8 zeigt die echte begrenzte Textantwort; sehr lange Ausgaben bleiben vollständig über die CLI erreichbar. Es werden keine beliebigen Client-Befehle oder Java-Ausdruecke angenommen.

## Einstellungsgrenzen und Besonderheiten

- `max_team_members` und `max_parties` greifen in FTB, wenn OP4 die Core-Teamlimits einschaltet. FTB prüft zusätzlich sein eigenes `max_party_size`.
- Core-Radien muessen `0 <= min_radius < max_radius` erfuellen; Transportzeit 60 bis 86400 Sekunden.
- FE-Leistung folgt `0 <= base <= tier1 <= tier2 <= 256`. Beim Erhoehen zuerst die hoechste Stufe anpassen, beim Senken umgekehrt.
- `claim_radius_chunks`, `auto_ftb_claims`, `base_guard_enabled` und `raids_enabled` setzen OP4 voraus und duerfen erst nach Backup/Datenschutzpruefung geaendert werden. Bereits vorhandene Welt-/Core-Daten werden durch Werte-Aenderung nicht migriert.
- Sable/Warium-**Rogler-eigene** Spezialwerte koennen mit OP4 bearbeitet werden. Warium-Recovery wirkt nicht rueckwirkend auf bereits geladene Entities. Sable-Originalconfig und FTB-eigene Settings bleiben unangetastet; die Admin-Seite kann die eigenen Diagnosen/Reloads ausloesen.
- Seit v0.8.5 bleiben ungespeicherte Zahleneingaben auch nach einer Serveraktualisierung als lokale Entwuerfe im offenen Menu bestehen. Erst `Speichern` sendet den einzelnen Wert; bei Konflikt weiterhin die serverseitige CAS-Sperre beachten. Ein Entwurf aendert noch keine Konfigurationsdatei.
- Das UI verwendet seit v0.8.4 einen Erwartungswert beim Speichern. Wenn ein anderer Admin den Wert inzwischen geaendert hat, erst `Aktualisieren` verwenden (kein stilles Ueberschreiben). Einstellungen werden in den vorhandenen `serverconfig/`-Properties gespeichert, das HUD weiterhin nur im eigenen Client. Private Wegpunkte bleiben lokal, sofern sie nicht ausdruecklich einem FTB-Team freigegeben werden.
- Wenn die Welt oder eine GUI noch nicht bereit ist, zuerst mit `/roglerui` neu oeffnen; **nicht** blind auf Dateiebene Daten loeschen.

## Abnahme / Fehlerdiagnose

Die v0.8.7-Basis und Kartenanzeige sind bestätigt. Die Teamverwaltung und den Protokoll-5-Join nach Testmatrix prüfen; bei GUI-Paketfehlern `latest.log` und CraftProbe-Sitzung exportieren. Bei unklarer Konfigaenderung `serverconfig/roglerteamwars*.properties` kontrollieren und Backup benutzen. Sicherheit und Autoritaet liegen stets auf dem Dedicated Server.

## v0.8.3 Admin-Suche

Admin-OP2/OP4: Im Control Center die Seite `Admin: Suche` öffnen, Schlagwort (z. B. `grace`, `fe`, `sable`) eintippen und `Suchen` anklicken. Gefilterte Rogler-Settings werden mit den bisherigen serverseitigen Freigaben und Validierungen bearbeitet. Die Suche umfasst nur die 24 bestehenden Rogler-Konfigurationsfelder, nicht Fremdmod-Dateien.

## v0.8.4 - Teams, Cores, Raid und Audit

- `Admin: Teams/Cores`: bis zu fuenf Teamzeilen je Seite; bei GUI-Scale mit wenig Platz weniger sichtbare Zeilen. `Auswaehlen` oeffnet ab v0.8.5 die Teamdetails mit serverseitiger Team-UUID; von dort koennen die passenden Aktionen direkt gestartet werden. Auf Offline-Teams ohne aktiven FTB-Spieler ist Name und Mitgliederzahl eventuell nicht ermittelbar.
- `Admin: Raidsteuerung`: Team auswaehlen, Minuten (1-30) eingeben und `Start fuer Ziel` oder `Stopp fuer Ziel`; erforderlich OP4 sowie ausdrueckliche Bestaetigung. Aktionen werden von der bestehenden Raid-Regellogik erneut validiert.
- `Admin: Status`: direkte, nur fuer OP2+ sichtbare Anzeigen. Das sind **keine TPS/MSPT** oder Live-Profiling-Daten.
- `Admin: Audit-Log`: letzte Ereignisse zuerst, bis zu fuenf Zeilen je Seite. Der Server legt `world/serverconfig/roglercore-ui-audit.log` an; Rotation als `roglercore-ui-audit.previous.log`.
- `Admin: Aktionen` und `Admin: Diagnose` pruefen IDs und Argumenttypen client- und serverseitig. Der Server erhaelt keine frei zusammengesetzten Minecraft-Befehle; er fuehrt nur explizit erlaubte Aktionen aus. Ab v0.8.8 werden echte begrenzte Befehlsrueckmeldungen im Diagnosebericht dargestellt. Keine Ausgabe bleibt ausdrücklich als fehlende Detailantwort erkennbar.

## v0.8.5 – HUD-Drag-and-drop und Admin-UX

- Im Control Center `HUD & Navigation` -> `HUD frei positionieren (Drag & Drop)`: Vorschaukasten mit der linken Maustaste ziehen. Das Menue zeigt eine repräsentative HUD-Vorschau, nicht die exakten Teamdaten. Mit den vier Eckbuttons kann der Anker gewechselt werden, **ohne die momentane Position zu veraendern**. `Speichern` aktualisiert ausschliesslich `config/roglercore-hud.properties` auf dem eigenen Client; `Abbrechen` und ESC verwerfen die Aenderung.
- `Admin: Teams/Cores` -> `Auswaehlen`: OP-geschuetzte Teamdetails mit genauer Server-Snapshot-UUID, Core-Position, Grace, Punkten und Raidstatus. Aktionen verwenden die gespeicherte UUID aus der Serverantwort und dieselbe Command-Allowlist wie v0.8.4. OP2 kann nicht heimlich OP4-Aktionen ausloesen.
- `Admin: Aktionen` und `Admin: Diagnose`: Textsuche nach Aktionsname, ID und Parametertyp. Das ist nur ein Clientfilter, keine Autorisierungsentscheidung.
- Konfigurationsfelder behalten ungespeicherte Eingaben waehrend eines Server-Refreshs. Bei erfolgreicher Bestaetigung wird der Entwurf verworfen; bei abgewiesenen Aenderungen bleibt er pruefbar. Die CAS-Pruefung laeuft bei jeder Speicherung gegen den aktuellen Serverwert.
- Bestehende Grenzen: Keine native FTB-Team-Mitgliedschaftsmutation, keine vollstaendige strukturierte Drittmod-Diagnose, noch keine vollstaendig auf kleine GUI-Skalen gepruefte Scrollarchitektur.
