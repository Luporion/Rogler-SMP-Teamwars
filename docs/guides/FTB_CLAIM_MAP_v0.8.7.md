# FTB Chunks, FTB Teams und Xaero: Claim-Kartenanzeige (NeoForge 1.21.1)

## Gesicherte Fakten aus Serverdiagnose 9.11.2026

- Serverdiagnose enthielt Rogler Core v0.8.5, FTB Chunks 2101.1.23, FTB Teams 2101.1.11.
- Serverseitig wurden je zwei Team-UUIDs 9 vorhandene FTB-Claims erkannt (`existing=9`, `rejected=0`).
- Die FTB-Karte/Claim-Ansicht zeigt laut Nutzer diese Claims nicht, die Xaero-Karte dagegen schon. Kein Client-Log/Claim-SavedData dieser Sitzung für eine definitive Root Cause vorhanden.
- Rogler `FtbClaimBridge` verlangt `CLAIM_VISIBILITY = PRIVATE` für Auto-Claims. Diese Datenschutzvorgabe NICHT auf PUBLIC ändern.

## Server – Konfiguration, Reihenfolge

Serverprofil: `.../crafty-4/servers/<server-uuid>/`, `level-name=world`.

1. Bei ausgeschaltetem Server überprüfen, ob die datei `world/serverconfig/ftbchunks-world.snbt` existiert. Ist sie vorhanden, ist sie gegenüber `config/ftbchunks-world.snbt` für diese Welt vorrangig. Bei fehlender Weltdatei die Vorlage `config/ftbchunks-world.snbt` bearbeiten; vorzugsweise eine Kopie unter `world/serverconfig/ftbchunks-world.snbt` als persistente Welt-Override anlegen.
2. Einstellungen im SNBT beibehalten/ändern:
   - `long_range_tracker_interval: 0` (war `20` in der hochgeladenen `config/ftbchunks-world.snbt`; PvP-Gegnerverfolgung deaktivieren)
   - `team_prop_defaults: { def_claim_visibility: "private" }` (war `"public"`; nur für **neue** Teams)
   - `ally_mode: "default"` beibehalten (`"forced_all"` wäre ein Privacy-Problem)
   - `disable_protection: false` beibehalten
   - `claiming.max_claimed_chunks: 500` und `force_loading.max_force_loaded_chunks: 25` vorläufig beibehalten
   - `force_loading.force_load_mode: "default"`: bestimmt erlaubten Offlinebetrieb nach FTB-Ranks-Regel, NICHT die Claim-Anzeige; je nach gewünschtem Verhalten später explizit ändern
3. Analog `world/serverconfig/ftbteams-server.snbt` bzw. `config/ftbteams-server.snbt`: `max_party_size: 4` statt Vorlage `0` (nur wenn maximal vier Spieler gewünscht). Das ist die tatsächliche FTB Teams-Grenze; ein Rogler-Config-Wert allein erzwingt sie nicht. Bestehende größere Teams werden nicht automatisch aufgelöst.
4. Nach Backup/Serverneustart die wirksame Weltconfig kontrollieren. Die Diagnose enthielt **nur die Vorlagen** und belegt die aktiven Weltwerte nicht.

## Client – entscheidende Anzeigeeinstellungen

- In **derselben Client-Instanz**, mit der FTB Chunks geöffnet wird, `config/ftbchunks-client.snbt` beziehungsweise die bevorzugte `local/ftbchunks-client.snbt` prüfen, insbesondere im Block `appearance`:

```snbt
appearance: {
  claimed_chunks_on_map: true
  own_claimed_chunks_on_map: true
}
```

- Diese Werte sind Optionen innerhalb einer **existierenden** SNBT-Datei/Section, keine vollständige neue Konfiguration. Alternativ im Spiel: FTB-Karte öffnen, Einstellungszahnrad → `Show claimed chunks on map` und `Show your own claimed chunks on map` aktivieren.
- In FTB-Claim-Ansicht korrekte Dimension und Region am eigenen Core prüfen. Nach Änderung neu verbinden; kein Löschen von Claim-Daten/Kartencaches als erste Maßnahme.
- Server- und Client-FTB-Chunks-Version vergleichen. Bei privaten Team-Claims gab es einen FTB-Admin-Claim-GUI-Bug (Issue #2185); 2101.1.23 listet dazu einen Fix. Die richtige Darstellung für den Eigentümer ist weiterhin getrennt zu prüfen.

## Rogler Core v0.8.7 – eine gezielte Sync-Aktion

Der OP-Command `/teamwars admin syncclaims <spieler>` verifiziert FTB-Party-UUID + Besitzer der Claims und fordert anschließend mit FTB `syncChunksToPlayer` das **nur an diesen autorisierten Spieler gerichtete** Client-Refresh an. Das ist **kein neues Claim-System**, ändert kein Claim-Eigentum und lädt keine Chunks. Er setzt nur vorhandene Claims ggf. wieder, wenn sie wirklich fehlen, wie der bisherige Befehl. Das ersetzt nicht die korrekten clientseitigen Anzeigeoptionen und behebt nicht automatisch FTB-Rendering-Bugs.

**Nicht verwenden:** blindes `/ftbchunks unclaim_all`, Löschen der FTB-Datensätze, alle Claims auf PUBLIC setzen oder zweites paralleles Claim-Framework. Die Ja/Nein-Frage, ob ein Claim forcet geladen ist, lässt sich nicht allein aus `existing=9` ableiten.

## Quellen

- FTB Chunks v1.21.1 Doku: https://docs.feed-the-beast.com/mod-docs/1.21.1/mods/suite/Chunks/config/
- FTB Chunks Issue #2185: https://github.com/FTBTeam/FTB-Mods-Issues/issues/2185
- FTB Chunks 2101.1.23 Changelog: https://www.curseforge.com/minecraft/mc-mods/ftb-chunks-forge/files/9374151/additional-files
