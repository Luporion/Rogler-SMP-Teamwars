# Rogler Control Center – UI- und Admin-Handbuch

**O / `/roglerui` / `/roglerconfig`** oeffnen das Control Center. Bestehende `teamwars`-Befehle bleiben verfuegbar; das native Core-Inventar ist weiterhin separat am platzierten Core erreichbar.

## Navigation

| Kategorie | Funktion |
|---|---|
| Uebersicht | Spieler online, eigene FTB-Teamzuordnung und Links |
| HUD & Navigation | Schalter, Marker, HUD-Bereiche, Position |
| Wegpunkte | Privater Sync, Teamfreigabe, sicherer Link-Abgleich und Auto Name/Farbe |
| Waypoint-Links | UUID-/Token-geschuetzte Apply/Acknowledge/Delete/Move/Unlink-Aktionen |
| Mein Team | Team-/War-/Explorationsstatus ueber bestehende Serverbefehle |
| Mein Base-Core | Core-Menue, Status, Schutz, Energie, Transport, Recovery |
| Admin: Teams/Basis/Schutz/Energie | Live-Werte der vier bisherigen Rogler-Teamwars-Konfigdateien |
| Admin: Kompat | Rogler-eigene Warium-Recovery, Sable-Metriken und Lifecycle-Guards (OP4) |
| Admin: Teams/Cores | Paginierte Team-/Base-Core-Übersicht mit UUID, Online-/Mitgliederzahl, Grace, Raidstatus, Punkten, Koordinaten und Auswahl |
| Admin: Raidsteuerung | Team-Auswahl aus der Liste, gesicherte Raidfenster ab OP4 |
| Admin: Status | Server-Zustand (Spieler, Cores, aktive Raids, Claims, Heap) ohne Chat |
| Admin: Audit-Log | Paginierte Chronik erfolgreicher bzw. abgelehnter UI-Adminaktionen und Einstellungsänderungen |
| Admin: Aktionen | Autorisierte Core-/Team-Administration, Raids und Grace nach Eingabe eines Ziels; Rueckmeldung im UI |
| Admin: Diagnose | Vorhandene Diagnose-/Recovery-/Warium-/Sable-Aktionen; ein Kurzstatus kommt ins UI |

Admin-Aktionen mit Ziel verwenden im ersten Feld **Spielernamen (z. B. Alex)** oder **FTB-Team-UUID**, die Zahl daneben fuer Minuten. Fuer `adoptcore` benoetigt man zusaetzlich den neuen Spieler im zweiten Feld. Kritische Aktionen haben einen weiteren Bestaetigungsdialog. Seit v0.8.4 werden Adminaktionen mit festen IDs und streng geprueften Parametern serverseitig ausgefuehrt; die GUI zeigt Rueckgabestatus/Fehler. Die vollstaendigen Details mancher alten Diagnosebefehle sind weiterhin ueber deren CLI erreichbar. Es werden keine beliebigen Client-Befehle oder Java-Ausdruecke angenommen.

## Einstellungsgrenzen und Besonderheiten

- `max_team_members` und `max_parties` sind nicht die tatsaechlich von FTB Teams erzwungenen Limits. `max_party_size` und FTB-Party-Grants muessen weiterhin in FTB verwaltet werden.
- Core-Radien muessen `0 <= min_radius < max_radius` erfuellen; Transportzeit 60 bis 86400 Sekunden.
- FE-Leistung folgt `0 <= base <= tier1 <= tier2 <= 256`. Beim Erhoehen zuerst die hoechste Stufe anpassen, beim Senken umgekehrt.
- `claim_radius_chunks`, `auto_ftb_claims`, `base_guard_enabled` und `raids_enabled` setzen OP4 voraus und duerfen erst nach Backup/Datenschutzpruefung geaendert werden. Bereits vorhandene Welt-/Core-Daten werden durch Werte-Aenderung nicht migriert.
- Sable/Warium-**Rogler-eigene** Spezialwerte koennen mit OP4 bearbeitet werden. Warium-Recovery wirkt nicht rueckwirkend auf bereits geladene Entities. Sable-Originalconfig und FTB-eigene Settings bleiben unangetastet; die Admin-Seite kann die eigenen Diagnosen/Reloads ausloesen.
- Das UI verwendet seit v0.8.4 einen Erwartungswert beim Speichern. Wenn ein anderer Admin den Wert inzwischen geaendert hat, erst `Aktualisieren` verwenden (kein stilles Ueberschreiben). Einstellungen werden in den vorhandenen `serverconfig/`-Properties gespeichert, das HUD weiterhin nur im eigenen Client. Private Wegpunkte bleiben lokal, sofern sie nicht ausdruecklich einem FTB-Team freigegeben werden.
- Wenn die Welt oder eine GUI noch nicht bereit ist, zuerst mit `/roglerui` neu oeffnen; **nicht** blind auf Dateiebene Daten loeschen.

## Abnahme / Fehlerdiagnose

Vor Produktivbetrieb: v0.8.4 CI-Lauf sowie Start/Join mit zwei gegnerischen Teams und den Create-C2ME-Chunk-Save-Pfad testen; bei GUI-Paketfehlern `latest.log` und CraftProbe-Sitzung exportieren. Bei unklarer Konfigaenderung `serverconfig/roglerteamwars*.properties` kontrollieren und Backup benutzen. Sicherheit und Autoritaet liegen stets auf dem Dedicated Server.

## v0.8.3 Admin-Suche

Admin-OP2/OP4: Im Control Center die Seite `Admin: Suche` öffnen, Schlagwort (z. B. `grace`, `fe`, `sable`) eintippen und `Suchen` anklicken. Gefilterte Rogler-Settings werden mit den bisherigen serverseitigen Freigaben und Validierungen bearbeitet. Die Suche umfasst nur die 24 bestehenden Rogler-Konfigurationsfelder, nicht Fremdmod-Dateien.

## v0.8.4 - Teams, Cores, Raid und Audit

- `Admin: Teams/Cores`: bis zu fuenf Teamzeilen je Seite; bei GUI-Scale mit wenig Platz weniger sichtbare Zeilen. `Auswaehlen` oeffnet das vorhandene Admin-Aktionsformular mit der FTB-Team-UUID als Ziel. Auf Offline-Teams ohne aktiven FTB-Spieler ist Name und Mitgliederzahl eventuell nicht ermittelbar.
- `Admin: Raidsteuerung`: Team auswaehlen, Minuten (1-30) eingeben und `Start fuer Ziel` oder `Stopp fuer Ziel`; erforderlich OP4 sowie ausdrueckliche Bestaetigung. Aktionen werden von der bestehenden Raid-Regellogik erneut validiert.
- `Admin: Status`: direkte, nur fuer OP2+ sichtbare Anzeigen. Das sind **keine TPS/MSPT** oder Live-Profiling-Daten.
- `Admin: Audit-Log`: letzte Ereignisse zuerst, bis zu fuenf Zeilen je Seite. Der Server legt `world/serverconfig/roglercore-ui-audit.log` an; Rotation als `roglercore-ui-audit.previous.log`.
- `Admin: Aktionen` und `Admin: Diagnose` pruefen IDs und Argumenttypen client- und serverseitig. Der Server erhaelt keine frei zusammengesetzten Minecraft-Befehle; er fuehrt nur explizit erlaubte Aktionen aus. Der Rueckgabecode ist nicht in jedem Drittmod-Fall eine vollstaendige Textdiagnose.
