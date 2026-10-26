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
| Admin: Aktionen | Core-/Team-Administration, Raids und Grace nach Eingabe eines Ziels |
| Admin: Diagnose | Health/Audit/Recovery/Sable/Warium und Reload-Befehle |

Admin-Aktionen mit Ziel verwenden im ersten Feld **Spielernamen (z. B. Alex)** oder **FTB-Team-UUID**, die Zahl daneben fuer Minuten. Fuer `adoptcore` benoetigt man zusaetzlich den neuen Spieler im zweiten Feld. Kritische Aktionen haben einen weiteren Bestaetigungsdialog. Rueckmeldungen der alten Commands stehen aktuell im Chat. Es werden keine beliebigen Client-Befehle oder Java-Ausdruecke angenommen.

## Einstellungsgrenzen und Besonderheiten

- `max_team_members` und `max_parties` sind nicht die tatsaechlich von FTB Teams erzwungenen Limits. `max_party_size` und FTB-Party-Grants muessen weiterhin in FTB verwaltet werden.
- Core-Radien muessen `0 <= min_radius < max_radius` erfuellen; Transportzeit 60 bis 86400 Sekunden.
- FE-Leistung folgt `0 <= base <= tier1 <= tier2 <= 256`. Beim Erhoehen zuerst die hoechste Stufe anpassen, beim Senken umgekehrt.
- `claim_radius_chunks`, `auto_ftb_claims`, `base_guard_enabled` und `raids_enabled` setzen OP4 voraus und duerfen erst nach Backup/Datenschutzpruefung geaendert werden. Bereits vorhandene Welt-/Core-Daten werden durch Werte-Aenderung nicht migriert.
- Sable/Warium-**Rogler-eigene** Spezialwerte koennen mit OP4 bearbeitet werden. Warium-Recovery wirkt nicht rueckwirkend auf bereits geladene Entities. Sable-Originalconfig und FTB-eigene Settings bleiben unangetastet; die Admin-Seite kann die eigenen Diagnosen/Reloads ausloesen.
- Einstellungen werden in den vorhandenen `serverconfig/`-Properties gespeichert, das HUD weiterhin nur im eigenen Client. Private Wegpunkte bleiben lokal, sofern sie nicht ausdruecklich einem FTB-Team freigegeben werden.
- Wenn die Welt oder eine GUI noch nicht bereit ist, zuerst mit `/roglerui` neu oeffnen; **nicht** blind auf Dateiebene Daten loeschen.

## Abnahme / Fehlerdiagnose

Vor Produktivbetrieb: v0.8.2 CI-Lauf sowie Start/Join mit zwei gegnerischen Teams testen; bei GUI-Paketfehlern `latest.log` und CraftProbe-Sitzung exportieren. Bei unklarer Konfigaenderung `serverconfig/roglerteamwars*.properties` kontrollieren und Backup benutzen. Sicherheit und Autoritaet liegen stets auf dem Dedicated Server.
