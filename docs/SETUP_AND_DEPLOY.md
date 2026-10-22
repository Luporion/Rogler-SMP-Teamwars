# Setup, Build, Deployment und Rollback

## 1. Repository / Git-Arbeitsstand

```powershell
cd E:\Entwicklung\rogler-smp-core
git branch --show-current
git status --short
git log -1 --oneline
```

Eine `git archive`-ZIP enthält den **committeten Quellstand, aber nicht `.git` oder uncommittete Änderungen**. Für eine Quelle ohne Git-Historie: `git archive --format=zip --output="M:\Downloads\Rogler-Core-Repository.zip" HEAD`.

## 2. Einmaliger Update-Patch vs. Zusatzkorrektur

- **Neuer Versionspatch:** Nur aus dem vom Patch genannten Ausgangsbranch bei sauberem Arbeitsverzeichnis `PATCH.cmd "M:\Downloads\...patch"` ausführen. Script erstellt Zielbranch, prüft, pusht und beobachtet Actions.
- **Patch für vorhandenen Branch** wie den v0.7.6-CI-Fix: Zielbranch auschecken, `git status --short`, dann `git am "M:\Downloads\...patch"`, `VERIFY.cmd`, `git push origin <branch>`.
- **Nie** denselben Patch mehrfach anwenden, sobald sein Zielbranch existiert. Fehler zunächst lesen, keine Git-Reparatur mit pauschalen `reset --hard`/`clean -fdx`.

## 3. Tests und CI

```powershell
.\VERIFY.cmd
python scripts/test-registrate-bootstrap.py
# Optional mit lokaler voller Create-JAR:
python scripts/bootstrap-registrate.py --create-jar "M:\Downloads\create-1.21.1-6.0.10.jar"
# Neuester Commit nach Push in GitHub Actions:
.\scripts\watch-actions.ps1
```

Für lokale NeoForge-Builds JDK 21 und eine kompatible Gradle-Version verwenden (`gradle -p mod-neoforge build --no-daemon`). CI erzeugt eine JAR nur bei vollständigem erfolgreichen Workflow, die danach **noch unverified-runtime** ist. [Abhängigkeits-/Checksummschutz](guides/REGISTRATE_CI_BOOTSTRAP_v0.7.6.md) nicht umgehen.

## 4. DEV vor Produktion

- Server stoppen und vollständiges Backup: Welt, FTB-Team-/Claim-Daten, Rogler SavedData sowie bei Waypoint-Tests private Xaero-Verzeichnisse aller beteiligten Clients.
- Nur eine Rogler-Core-JAR pro Mods-Ordner; Client und Server gleiche Version / Protokoll; alle Referenz-Modversionen kontrollieren.
- Zwei Teams und mindestens zwei Clients (inkl. OP und Nicht-OP) testen: Join, HUD, FTB/Xaero-Spielersichtbarkeit, Share/Revoke, private Waypoints, Teamwechsel, Neustart, Claim und Kernschutz. [Testmatrix](TEST_MATRIX.md).
- Erst nach bestandenem Laufzeittest in produktive Welt übernehmen. **Nie** alte Core-/FTB-SavedData pauschal löschen.

## 5. Rollback

Bei Laufzeitfehlern Service anhalten, Diagnosen und Backup sichern, kompatible letzte getestete JAR auf **Server und allen Clients** einsetzen. Neues SavedData separat sichern; exakte Migration/Abwärtskompatibilität pro Release beachten. Für v0.7.6 enthält `world/data/roglerteamwars_shared_waypoints.dat` ausschließlich explizit geteilte Marker und sollte bei Rollback erhalten/gesichert bleiben. Die Modifikation der Dokumentation benötigt keine Weltmigration.

[Weitere Guides](INDEX.md) · [letzter Projektstatus](PROJECT_STATE.md)
