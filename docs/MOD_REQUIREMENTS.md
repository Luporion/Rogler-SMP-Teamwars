# Modanforderungen – Rogler Core v0.8.9

**Minecraft 1.21.1, NeoForge 21.1.249, Rogler Core 0.8.9 auf Client und Server.** Teamberichte verwenden Netzwerkprotokoll **5**. Die Team-Hooks wurden mit der FTB Teams NeoForge JAR 2101.1.11 getestet.

Keine neuen Drittanbieter-Pflichtmods und keine Änderung bestehender Abhängigkeitsversionen durch dieses Release.

| Komponente | Bisherige Ziel-/Diagnoseversion | Verwendung |
|---|---|---|
| Create | 6.0.10-280 | Vorhandene Core-Kinetik; unveränderte Abhängigkeit |
| FTB Teams | 2101.1.11 | Autoritative Party-Mitgliedschaft |
| FTB Chunks | 2101.1.23 im Nutzerexport; Metadaten weiterhin mindestens 2101.1.22 | Claims, Markierungen und Quoten; fehlende API-Werte werden unbekannt |
| FTB Library / Architectury | Kompatible bestehende Versionen laut Modpack | FTB-Abhängigkeiten und bestehender Paketpfad |
| Xaero Minimap / World Map | Bisheriger Referenzstand 26.4.2 / 1.45.0 | Bestehende Kartenintegration; nicht durch 0.8.8 aktualisiert |
| XMMP-FTBTeams | 0.4.2 im Nutzerexport | Separater vorhandener Gelände-Sharing-Fork; keine neue Pflicht durch Core-Diagnose |

Die Reflection-Verträge der Diagnose wurden an der offiziellen FTB-Chunks-Quelle `1.21.1/main`, Commit `1093f3392e42a28b69184061dfa0b5b3d22fb4b9`, mit `mod_version=2101.1.23` geprüft. Quellen und Semantik: [Diagnoseguide](guides/CLAIM_DIAGNOSTICS_v0.8.8.md). Das ist kein Ersatz für den Laufzeittest mit den tatsächlichen Modpack-JARs.

## Build

Java 21 und Gradle 8.14 wie bisherige CI. Registrate `MC1.21-1.3.0+67` weiterhin ausschließlich über den bestehenden SHA-geprüften Create-JarJar-Bootstrap oder die reguläre Auflösung. Keine Prüfsummen umgehen und keine Drittmod-JARs in Git aufnehmen.

## Datenschutz

FTB-Langstreckentracking und Claim-Privatsphäre nach bestehendem Serverprofil beibehalten. OP2+ darf die vorhandene Admin-Teamübersicht und zugehörige Diagnosen sehen; normale Spieler nur ihr eigenes frisch bestätigtes FTB-Team. Der Report ist ein Snapshot, keine neue Freigaberegel für Karten anderer Mods.
