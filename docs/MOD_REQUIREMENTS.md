# Neue UI-Anforderungen v0.8.2

Keine neuen Drittanbieter-Pflichtmods. Gleicher Minecraft-/NeoForge-/FTB-/Xaero-/Create-Stand wie v0.8.1. **Client und Server muessen gemeinsam auf v0.8.2 aktualisiert werden**, da zwei neue UI-Payloadtypen im unveraenderten Protocol-Registrar `3` hinzukommen. Noch kein GitHub-Actions-Buildnachweis fuer v0.8.2; alle unten stehenden v0.7.7-/v0.7.6-Hinweise als historische Voraussetzung lesen.

# Aktuelle Mod-Anforderungen – v0.7.7 (Quellcode)

- Minecraft **1.21.1**, NeoForge **21.1.249**, Create **6.0.10**, FTB Teams **2101.1.11**, FTB Chunks **2101.1.22** und FTB Library **2101.1.37** sind die bisher geprüften Zielversionen; **keine** neuen Drittanbieter-Mods für v0.7.7.
- Clientseitige optionale Waypoint-/HUD-Integration ist auf Xaero Minimap **26.4.2** und World Map **1.45.0** ausgerichtet; bei abweichender Version API-Kompatibilität erneut prüfen.
- Registrate **MC1.21-1.3.0+67** wird zum Bauen benötigt; CI verwendet den SHA-geprüften Create-JarJar-Bootstrap. Das ist **kein zusätzlicher** Modpack-Download.
- Alle Spieler und der Dedicated Server benötigen dieselbe Rogler-Core-Version; Netzwerkprotokoll bleibt bei **3**. Ein erfolgreicher Java-Test ist keine Minecraft-Freigabe.

## Historische Anforderungen

# Modanforderungen – aktueller Entwicklungsstand v0.7.6

**Zielumgebung aus vorhandener Projektdokumentation und Nutzerdiagnosen** (keine automatische Prüfung der gegenwärtig installierten Server-/Client-JARs):

| Komponente | Zielversion | Zweck / Status |
|---|---|---|
| Minecraft | 1.21.1 | Verbindliche Zielversion |
| NeoForge | 21.1.249 | Verbindliche Zielversion |
| Rogler Core | 0.7.6 | **Noch nicht als vollständig gebaut/getestet bestätigt**; Server und Clients müssen bei Installation dieselbe Protokollversion nutzen |
| FTB Teams | 2101.1.11 | Teammitgliedschaftsquelle |
| FTB Library | 2101.1.37 | Benötigt von FTB-Mods; reale Modliste prüfen |
| FTB Chunks | 2101.1.22 | Claims, Sichtbarkeit, Wegpunkt-Integration; Architectury-Payloadpfad beachten |
| Create | 6.0.10 (Build 280) | Core-Kinetik, Registrate ist Build-Abhängigkeit |
| Xaero Minimap | 26.4.2 | Client-Karten-/Waypoint-Integration; Laufzeittest erforderlich |
| Xaero World Map | 1.45.0 | Client-Kartenanzeige; native Marker/Terrain derzeit nicht voll verifiziert |
| XaeroPlus | 2.36.0 | Im Referenzprofil vorhanden; nicht als Rogler-Core-Pflichtmod behauptet |

**Keine neuen Drittanbieter-Mods durch die Dokumentationsbereinigung.** Für v0.7.6 sind laut [Release-Notiz](releases/v0.7.6.md) keine neuen Drittanbieter-Pflichtdownloads vorgesehen. Die konkreten Pflichtmods sind von der jeweiligen Funktion und deren Runtime-Abhängigkeiten zu unterscheiden. Andere bereits installierte Modpack-Komponenten (Simple Voice Chat, Sophisticated, AE2, Sable) werden nicht allein durch ihre Anwesenheit als vollständig unterstützte Integration betrachtet.

## Minecraft vs. Build

- `Registrate MC1.21-1.3.0+67` wird zur Buildzeit aufgelöst. Die frühere Maven-URL lieferte bei GitHub Actions HTTP 521; der [Bootstrap](guides/REGISTRATE_CI_BOOTSTRAP_v0.7.6.md) extrahiert die exakt gehashte eingebettete JAR aus dem offiziellen Create-Full-JAR. `.ci-maven/` bleibt lokal und wird nicht committed.
- **Nicht** Create aktualisieren, auf ungeprüfte JAR-Downloads wechseln oder SHA-256-Checks umgehen, nur damit der Build grün wird.
- Client-/Server-Abgleich auf **Modversion, Loader, Protokoll und fehlende Doppel-JARs** prüfen. Xaero persönliche Datei-Backups vor `/roglerwaypoints sync` erstellen.

## Server-Privatsphäre (Pflicht vor Teamtests)

- FTB Chunks: `long_range_tracker_interval: 0`; bestehende Teams `Location Visibility` und `Claim Visibility` auf `Private` (Defaultwerte gelten nicht rückwirkend). `location_mode_override: false` allein genügt nicht.
- Xaero: Radar/Spielertracking serverseitig deaktiviert; Cave Mode darf getrennt davon aktiviert bleiben, sofern die Serverprofile real wirksam sind.
- Gegner-/OP-Sichtbarkeit und andere Modded-Karten/Overlays mit zwei Parteien prüfen, nicht aus Konfigurationsdatei allein ableiten.

Frühere versionierte Anforderungen sind unter [Archiv](archive/mod-requirements/) verfügbar; die originalen alten Sammelangaben stehen [hier](archive/legacy/MOD_REQUIREMENTS_before_cleanup_2026-10-22.md).
