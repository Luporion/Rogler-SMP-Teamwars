# GitHub Actions Build-Monitor — implementation and tests

## Scope

PowerShell utility at `scripts/watch-actions.ps1`. No gameplay or CI workflow changes. The existing workflows `P0 Core` and `NeoForge v0.2.0 Compile` are the gates; the build workflow uploads one `roglerteamwars-*-unverified-runtime` artifact. The NeoForge workflow name may contain a later version number, provided it retains `NeoForge ... Compile`.

### One-time setup (Windows PowerShell 5.1 or PowerShell 7)

```powershell
winget install --id GitHub.cli -e
gh auth login
gh auth status
```

Git must also be available in PATH. Run the following **from the repository clone** after applying a patch, testing locally, committing and pushing:

```powershell
git push
.\scripts\watch-actions.ps1
```

If PowerShell's execution policy blocks scripts, use a **single-process** override (do not change the system policy):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\watch-actions.ps1
```

Optional parameters:

```powershell
.\scripts\watch-actions.ps1 -PollSeconds 10 -TimeoutMinutes 90
.\scripts\watch-actions.ps1 -Commit <full-40-character-SHA> -Branch feat/v0.2.0-base-core
.\scripts\watch-actions.ps1 -OutputDir 'C:\Minecraft\Artifacts'
```

**Terminal behavior:** No `cls`, `Clear-Host`, ANSI cursor operations, full-screen UI, `Write-Progress` or `gh run watch`. Each changed state appends a timestamped line to the existing history. `Ctrl+C` stops the process. `gh run download` is invoked only for the matching artifact after both green checks.

**Matching:** The exact local `HEAD` SHA and current branch, `push` event only. No reliance on the newest run in a repository. Up to 3 minutes for workflow discovery and 75 minutes total by default. If a workflow fails, the monitor prints `gh run view --log-failed` output and exits nonzero. Errors are not treated as green.

**Download:** `artifacts/run-<run-id>-<sha8>/` beneath the project clone, with the extracted JAR(s) and `build-monitor.json` (SHA-256 per file, SHA, branch, run and artifact IDs). The destination is never silently overwritten. A matching, hash-verified local download is reused. A temporary download folder is cleaned on failure; `artifacts/` is ignored by Git.

**Safety:** Success proves the current revision passed these CI workflows and the matching artifact downloaded; it does **not** prove mod runtime compatibility. Continue testing the JAR with the matching modpack in a disposable world. Never auto-copy it into a productive server's `mods` directory.

## Acceptance matrix

1. Two green push runs for the same SHA/branch => download only the build run's matching named artifact, verify JAR, save SHA-256 manifest, exit 0.
2. Workflow still queued/running => show state transition lines only; no screen clear, no high-frequency repeated messages.
3. Core failed or compile failed => show relevant logs, exit 1, never download.
4. Missing run or mismatched SHA/branch/event => wait and ultimately fail; never take an older commit's success.
5. Multiple or missing matching artifacts => fail safely.
6. Successful repeated invocation => verify manifest and hashes, do not overwrite.
7. Existing destination with wrong manifest, missing JAR or changed hash => reject without overwrite.
8. `Ctrl+C` => stop, preserve terminal scrollback.
9. Offline `gh`/unauthenticated GitHub => fail clearly; transient failures tolerated for two polling attempts.

## Evidence and limitations

Authored against the repository's existing `core-ci.yml` and `neoforge-build.yml`, current branch `feat/v0.2.0-base-core` (2026-10-10). Structural/static and patch-application checks may be run without GitHub credentials. **The PowerShell runtime and real end-to-end GH download have not been executed in the authoring environment; Windows testing remains required.**
