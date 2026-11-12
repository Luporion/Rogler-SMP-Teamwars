# Rogler Core – aktueller Projektstatus

Stand: **12. November 2026**. **v0.8.9** basiert auf dem erfolgreichen v0.8.8-Commit `dc4053c0`. Der Terminalexport zeigt erfolgreiche P0- und NeoForge-Workflows für v0.8.8.

## v0.8.9

- FTB-Teamverwaltung im Control Center und über `/teamwars team manage`. Teamleitung, Offiziere und Mitglieder erhalten ihre jeweiligen Aktionen. OP2 kann Mitglieder und Rollen anderer Teams einsehen, auch wenn kein Mitglied online ist.
- OP4 kann die Core-Teamlimits aktivieren. FTB prüft die Grenzen bei Erstellung, Einladung und Beitritt. Eine Limitsenkung lässt bestehende Partys und Mitglieder bestehen.
- Die FTB-Mitgliedschaftsänderung löst beim nächsten Server-Tick den Abgleich von Scoreboard, Core-Markern, Teamwegpunkten und Core-Erkundungsberechtigungen aus.
- Client und Server verwenden **Protokoll 5**. Der lokale Dedicated-Server-Test mit FTB Teams 2101.1.11 bestätigt Start, Limits und Persistenz. Tests mit verbundenen Clients und dem vollständigen Rogler-Alpha-Pack stehen aus.

[Release v0.8.9](releases/v0.8.9.md) · [Testmatrix](TEST_MATRIX.md)

## Bestätigte Ausgangsbasis v0.8.7

- Core- und NeoForge-Workflows erfolgreich, Commit `e22eecb9`; Terminalexport nennt P0 `42712522413` und NeoForge `42712522272`.
- Serverdiagnose bestätigt `roglercore-0.8.7.jar`, erfolgreichen Start und zwei Teams mit je neun bestehenden FTB-Claims; unveränderte erfolgreiche Claim-Abgleiche nur einmal pro Team geloggt.
- Nutzer bestätigt am 10.11.: geclaimte und gechunkloadete Chunks auf beiden Karten sichtbar. Die weiteren Tests haben grundsätzlich funktioniert. Das Anzeigeproblem ist damit erledigt; die Rückmeldung ist kein Einzelprotokoll jedes Sonderfalls.
- Im ausgewerteten Leerlauf später etwa 1,8 ms mittlere JFR-Tickzeit. Kein belastbarer prozentualer Vorher-/Nachher-Vergleich unter Spielerlast.
- XMMP-FTBTeams v0.4.2 ist im ausgewerteten Lauf installiert. Der Fork ist vorhanden, nicht erst ein künftiger Projektbeginn.
- Welt-/Chunkspeicherung erfolgreich. Nach Serverstopp wurden noch Tan’s Huge Trees und ein Pool-Thread gemeldet; das ist ein offenes Modpack-Thema ohne belegte Rogler-Core-Ursache.

## v0.8.8 – implementierter Umfang

- Neue Seite **Claims & Chunkloading** für das eigene frisch von FTB bestätigte Party-Team. OP2+ kann dieselbe Diagnose aus den vorhandenen Teamdetails für ein ausgewähltes Team laden.
- Serverseitige, seitenweise Reports: gespeicherter Core-Zustand, Core-Regeln, gespeicherte Core-Claim-Belege, FTB-Teamclaims, markierte Forceloads, lesbare Limits/Restquoten, Offline-Regel und Corezone bis maximal 7×7 Chunks.
- Die einzelnen Zustände werden getrennt angezeigt: Besitz, Forceload-Markierung, FTB-Regelfreigabe und bereits im Speicher vorhandener Chunk. Keine Behauptung eines aktiven Tickets oder laufender Maschinen allein anhand eines FTB-Getters.
- Status verwendet ausschließlich bestehende FTB-Daten; kein `getOrCreateData`, kein Claim-/Ticket-Update und keine Chunk-Ladeanforderung. Teamgesamtzahlen nur bei höchstens 512 globalen Claims; sonst ausdrücklich unbekannt. Vorhandene Teamdaten werden höchstens aus 128 Einträgen gesucht; Quoten bleiben bei unbekannter API unbekannt.
- Eigener Karten-Resync sendet nur vorhandene Daten des bestätigten Teams an diesen Spieler. Keine Claim-Reparatur, keine neuen Tickets.
- Adminaktionen liefern ihre tatsächlichen, begrenzten Textantworten im UI statt nur Rückgabewerten. Allowlist, Bestätigungen und Berechtigungen bleiben erhalten.
- Protokoll **4** wegen neuem Report-Payload: **Client und Server gemeinsam auf 0.8.8 aktualisieren**. Keine neue Drittmod-Pflicht und kein SavedData-Schemawechsel gegenüber 0.8.7.

Build, automatisierte Prüfungen und Minecraft-Abnahme werden in [Release v0.8.8](releases/v0.8.8.md) und [Testmatrix](TEST_MATRIX.md) getrennt ausgewiesen. Neue v0.8.8-UI-/Netzwerkfunktionen benötigen ihren eigenen Laufzeittest; bestätigte v0.8.7-Grundfunktionen werden dadurch nicht zurückgestuft.

## Bewusste Grenzen und nächste Schritte

Ein eigener Core-Chunkloading-Adapter mit unabhängigen Quoten, echtes integriertes AE2-ME-Terminal/Drive und Sophisticated-Modulslots sind noch nicht implementiert. FE/Create-Basis und das Control Center sind vorhanden. FTB bleibt Claim-/Ticket-Autorität; FTB Teams bleibt Mitgliedschaftsquelle.

Nächste Version: **0.9.0 Kartenintegration mit vorhandenem XMMP konsolidieren**; danach Funk, Core-Ticketquoten und Technikmodule gemäß [Roadmap](ROADMAP.md).

Historischer Dokumentstand: [Archiv vor 0.8.8](archive/legacy/PROJECT_STATE_before_v088_2026-11-11.md). Er enthält damalige offene Build-/Kartenpunkte und ist keine aktuelle Aufgabenliste.
