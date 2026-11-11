# Claim-/Chunkloading-Diagnose v0.8.8

## Bedienung

`O` oder `/roglerui` → **Claims & Chunkloading**. Die Seite ist für normale Mitglieder eines bestätigten FTB-Party-Teams zugänglich. Alternativ unter **Mein Base-Core** öffnen. Mit den Pfeilen blättern, mit **Aktualisieren** einen neuen Snapshot anfordern. Tooltip zeigt den vollständigen Text kurzer Zeilen.

OP2+: **Admin: Teams/Cores** → Team auswählen → **Claims & Chunkloading für dieses Team**. Bestehende OP2-Rechte werden nicht eingeschränkt; Nicht-OP erhält kein fremdes Ziel.

## Was die Werte bedeuten

| Wert | Quelle und Bedeutung |
|---|---|
| Core (gespeichert) | Registrierter Ledger-Zustand; keine physische Blockprüfung und kein Chunkladen |
| Core-Regel | Aktuelle Rogler-Regeln zu Auto-Claims und Guard |
| Core-Claim-Belege | Vom Core gespeicherte Belege eigener Claim-Erstellung; keine Gesamtzahl vorhandener FTB-Claims |
| FTB-Teamclaims / markiert | Aktuelle FTB-Daten über alle Dimensionen, sofern die globale Claimmenge höchstens 512 beträgt |
| Limit / frei | Von vorhandenem FTB-TeamData gelesene Maximalwerte; unbekannte oder negative Werte bleiben unbekannt |
| Offline-Forceload | FTB `canDoOfflineForceLoading`; Regel-/Berechtigungszustand |
| FTB-API je Chunk | FTB `isActuallyForceLoaded`; im geprüften Quellstand Markierung UND Offline-Regelfreigabe. Kein Beweis eines real vorhandenen Loader-Tickets. |
| geladen | `getChunkNow` liefert einen bereits residenten LevelChunk; keine Ladeanforderung. Kein Beweis für Entity-Ticking oder laufende Maschinen. |
| Corezone | Radius aus aktueller Core-Schutzkonfiguration um den gespeicherten Overworld-Core, maximal 49 Chunks. Kein zusätzliches Claiming. |

Eine Markierung kann vorhanden sein, während der Chunk noch nicht resident ist. Ein residenter Chunk kann von Spielern oder einem anderen Loader geladen sein. Dieser Report behauptet keine genaue Ticketherkunft, die FTB-API nicht verlässlich liefert.

Teamgesamtzahlen werden auf großen Claim-Inventaren ausdrücklich unbekannt; die Corezone bleibt lokal lesbar. Vorhandene TeamData-Suche ist auf 128 Teams begrenzt. Es werden keine fehlenden Teamdaten erzeugt. Snapshot-Seiten umfassen höchstens acht Zeilen; ein Report höchstens 96 Zeilen. Neue Statusanfragen sind pro Spieler auf eine je zehn Ticks begrenzt, Resyncs auf eine je 40 Ticks.

## Eigene Kartendaten erneut senden

Der Button führt keinen Admin-Claim-Audit aus. Er prüft die reale Mitgliedschaft erneut und sendet vorhandene eigene FTB-Claims nur an diesen Spieler. Keine neuen Claims, Tickets oder Konfigänderungen. Falls bestehende Daten innerhalb des Suchlimits nicht gefunden werden oder die Schnittstelle fehlt, meldet die Aktion das ausdrücklich.

## Tatsächliche Admin-Rückmeldungen

Die bestehenden Adminaktionen behalten Allowlist, Berechtigung und Bestätigungen. Die echte `CommandSource`-Textantwort wird begrenzt in einen Bericht übernommen; Ergebniscode und Details bleiben getrennt. Bei Überlänge wird auf die vollständige Ausgabe des vorhandenen Befehls verwiesen. **Aktualisieren** eines Aktionsberichts führt die Aktion nicht erneut aus.

## Geprüfter FTB-Vertrag

Offizielles Repository: `FTBTeam/FTB-Chunks`, Branch `1.21.1/main`, Commit `1093f3392e42a28b69184061dfa0b5b3d22fb4b9`, `gradle.properties`: Version `2101.1.23`.

- https://github.com/FTBTeam/FTB-Chunks/blob/1093f3392e42a28b69184061dfa0b5b3d22fb4b9/common/src/main/java/dev/ftb/mods/ftbchunks/data/ClaimedChunkManagerImpl.java
- https://github.com/FTBTeam/FTB-Chunks/blob/1093f3392e42a28b69184061dfa0b5b3d22fb4b9/common/src/main/java/dev/ftb/mods/ftbchunks/data/ChunkTeamDataImpl.java
- https://github.com/FTBTeam/FTB-Chunks/blob/1093f3392e42a28b69184061dfa0b5b3d22fb4b9/common/src/main/java/dev/ftb/mods/ftbchunks/data/ClaimedChunkImpl.java

Insbesondere `getOrCreateData`, `getClaimedChunks` und `getForceLoadedChunks` werden von der Statusabfrage nicht verwendet: ersteres kann Daten erzeugen, die beiden letzteren können globale Filter-Caches aufbauen. Stattdessen nur kleine, begrenzte existierende Inventare und direkte Corezonen-Lookups.
