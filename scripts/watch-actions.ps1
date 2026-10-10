# Requires Windows PowerShell 5.1 or PowerShell 7 and GitHub CLI (gh).
# Append-only terminal output: no Clear-Host, redraw, progress bar or gh run watch.
[CmdletBinding()]
param(
    [ValidatePattern('^[0-9a-fA-F]{40}$')]
    [string]$Commit = '',

    [string]$Branch = '',

    [ValidatePattern('^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$')]
    [string]$Repo = 'Luporion/Rogler-SMP-Teamwars',

    [ValidateRange(2, 60)]
    [int]$PollSeconds = 5,

    [ValidateRange(1, 240)]
    [int]$TimeoutMinutes = 75,

    [ValidateRange(1, 30)]
    [int]$DiscoveryMinutes = 3,

    [string]$OutputDir = 'artifacts'
)

$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

function Write-Status {
    param([string]$Message, [string]$Color = 'Gray')
    Write-Host ('[{0}] {1}' -f (Get-Date -Format 'HH:mm:ss'), $Message) -ForegroundColor $Color
}

function Invoke-GhJson {
    param([string[]]$Arguments)
    $output = & gh @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw ('GitHub CLI fehlgeschlagen (Exit {0}): gh {1}' -f $LASTEXITCODE, ($Arguments -join ' '))
    }
    $text = ($output -join "`n").Trim()
    if (-not $text) { throw 'GitHub CLI hat keine JSON-Antwort geliefert.' }
    try { return (ConvertFrom-Json -InputObject $text) }
    catch { throw ('Ungueltige JSON-Antwort von gh: {0}' -f $_.Exception.Message) }
}

function Get-RunList {
    $data = Invoke-GhJson -Arguments @(
        'run', 'list', '-R', $Repo, '--commit', $Commit,
        '--event', 'push', '--limit', '100',
        '--json', 'databaseId,name,workflowName,status,conclusion,headSha,headBranch,event,attempt,url'
    )
    return @($data | Where-Object { $null -ne $_ })
}

function Select-LatestRun {
    param([object[]]$Runs, [string]$Kind)
    $eligible = @($Runs | Where-Object {
        $_.headSha -eq $Commit -and
        $_.headBranch -eq $Branch -and
        $_.event -eq 'push'
    })
    if ($Kind -eq 'core') {
        $eligible = @($eligible | Where-Object {
            $_.workflowName -eq 'P0 Core' -or $_.name -eq 'P0 Core'
        })
    } else {
        $eligible = @($eligible | Where-Object {
            $_.workflowName -match '^NeoForge\b.*\bCompile$' -or
            $_.name -match '^NeoForge\b.*\bCompile$'
        })
    }
    return ($eligible | Sort-Object -Property databaseId -Descending | Select-Object -First 1)
}

function Get-RunState {
    param([object]$Run)
    if ($null -eq $Run) { return 'waiting for workflow' }
    if ($Run.status -eq 'completed') {
        if ($Run.conclusion -eq 'success') { return 'success' }
        return ('completed / {0}' -f $Run.conclusion)
    }
    return [string]$Run.status
}

function Show-RunFailure {
    param([object]$Run, [string]$Label)
    Write-Status "$Label fehlgeschlagen: Run $($Run.databaseId) ($($Run.conclusion))." 'Red'
    if ($Run.url) { Write-Status "Details: $($Run.url)" 'DarkGray' }
    Write-Status 'Fehlgeschlagene Steps/Logs:' 'Yellow'
    & gh run view ([string]$Run.databaseId) -R $Repo --log-failed
    if ($LASTEXITCODE -ne 0) {
        Write-Status 'Logs konnten nicht geladen werden; zeige Run-Zusammenfassung.' 'Yellow'
        & gh run view ([string]$Run.databaseId) -R $Repo
    }
}

function Get-Artifact {
    param([long]$BuildRunId)
    $deadline = (Get-Date).AddMinutes(2)
    $announced = $false
    do {
        $response = Invoke-GhJson -Arguments @(
            'api', ('repos/{0}/actions/runs/{1}/artifacts?per_page=100' -f $Repo, $BuildRunId)
        )
        $foundArtifacts = @($response.artifacts | Where-Object {
            $_.expired -eq $false -and
            $_.name -match '^roglerteamwars-[A-Za-z0-9_.+\-]+-unverified-runtime$'
        })
        if ($foundArtifacts.Count -gt 1) {
            throw 'Mehrere passende Rogler-Artefakte vorhanden. Kein automatischer Download.'
        }
        if ($foundArtifacts.Count -eq 1) { return $foundArtifacts[0] }
        if (-not $announced) {
            Write-Status 'Warte auf das Artifact des erfolgreichen Build-Runs ...' 'Yellow'
            $announced = $true
        }
        Start-Sleep -Seconds $PollSeconds
    } while ((Get-Date) -lt $deadline)
    throw 'Kein passendes, nicht abgelaufenes Rogler-JAR-Artifact gefunden.'
}

function Get-ValidJars {
    param([string]$Directory)
    return @(Get-ChildItem -LiteralPath $Directory -File -Recurse -Filter '*.jar' |
        Where-Object { $_.Name -notmatch '(?i)-(sources|javadoc|dev)\.jar$' })
}

function Test-CachedDownload {
    param([string]$Target, [object]$Artifact, [long]$BuildRunId)
    $manifestPath = Join-Path $Target 'build-monitor.json'
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) { return $false }
    try {
        $saved = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
        if ([long]$saved.RunId -ne $BuildRunId -or
            [long]$saved.ArtifactId -ne [long]$Artifact.id -or
            $saved.Commit -ne $Commit -or
            $saved.Branch -ne $Branch) { return $false }
        $jars = @(Get-ValidJars -Directory $Target)
        if ($jars.Count -eq 0 -or @($saved.Jars).Count -ne $jars.Count) { return $false }
        foreach ($item in @($saved.Jars)) {
            $file = Join-Path $Target ([string]$item.File)
            if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { return $false }
            if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne $item.SHA256) {
                return $false
            }
        }
        return $true
    } catch { return $false }
}

try {
    foreach ($tool in @('git', 'gh')) {
        if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
            throw "'$tool' fehlt. Bitte Git und GitHub CLI (gh) installieren."
        }
    }
    if (-not $Commit) {
        $Commit = ((& git -C $repoRoot rev-parse --verify HEAD) -join "`n").Trim()
        if ($LASTEXITCODE -ne 0 -or $Commit -notmatch '^[0-9a-fA-F]{40}$') {
            throw 'Lokaler Git-Commit konnte nicht ermittelt werden.'
        }
    }
    if (-not $Branch) {
        $Branch = ((& git -C $repoRoot branch --show-current) -join "`n").Trim()
        if ($LASTEXITCODE -ne 0 -or -not $Branch) {
            throw 'Kein aktiver Branch (detached HEAD?). Bitte -Branch angeben.'
        }
    }
    if ([IO.Path]::IsPathRooted($OutputDir)) {
        $outputRoot = [IO.Path]::GetFullPath($OutputDir)
    } else {
        $outputRoot = [IO.Path]::GetFullPath((Join-Path $repoRoot $OutputDir))
    }

    $null = & gh api ('repos/{0}' -f $Repo) --jq '.full_name'
    if ($LASTEXITCODE -ne 0) {
        throw "Repository nicht erreichbar: $Repo. 'gh auth login' und Repo-Zugriff pruefen."
    }
    $shortSha = $Commit.Substring(0, 8)
    Write-Status "Monitor fuer $Repo | Branch $Branch | Commit $shortSha" 'Cyan'
    Write-Status 'GitHub Actions werden alle paar Sekunden geprueft; kein Loeschen des Terminal-Verlaufs.' 'DarkGray'

    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    $discoveryDeadline = (Get-Date).AddMinutes($DiscoveryMinutes)
    $states = @{}
    $core = $null
    $build = $null
    $consecutiveErrors = 0

    while ((Get-Date) -lt $deadline) {
        try {
            $runs = @(Get-RunList)
            $consecutiveErrors = 0
        } catch {
            $consecutiveErrors++
            Write-Status "GitHub-Abfrage fehlgeschlagen ($consecutiveErrors/3): $($_.Exception.Message)" 'Yellow'
            if ($consecutiveErrors -ge 3) { throw }
            Start-Sleep -Seconds $PollSeconds
            continue
        }
        $core = Select-LatestRun -Runs $runs -Kind 'core'
        $build = Select-LatestRun -Runs $runs -Kind 'build'

        foreach ($row in @(
            @{ Key = 'core'; Label = 'P0 Core'; Run = $core },
            @{ Key = 'build'; Label = 'NeoForge Build'; Run = $build }
        )) {
            $run = $row.Run
            $state = Get-RunState -Run $run
            $id = if ($null -ne $run) { [string]$run.databaseId } else { '-' }
            $attempt = if ($null -ne $run) { [string]$run.attempt } else { '-' }
            $fingerprint = "$id/$attempt/$state"
            if ($states[$row.Key] -ne $fingerprint) {
                $color = 'Cyan'
                if ($state -eq 'success') { $color = 'Green' }
                elseif ($state -like 'completed / *') { $color = 'Red' }
                elseif ($state -eq 'waiting for workflow') { $color = 'Yellow' }
                Write-Status ("{0} [Run {1}]: {2}" -f $row.Label, $id, $state) $color
                $states[$row.Key] = $fingerprint
            }
        }

        foreach ($candidate in @(
            @{ Label = 'P0 Core'; Run = $core },
            @{ Label = 'NeoForge Build'; Run = $build }
        )) {
            if ($null -ne $candidate.Run -and $candidate.Run.status -eq 'completed' -and
                $candidate.Run.conclusion -ne 'success') {
                Show-RunFailure -Run $candidate.Run -Label $candidate.Label
                throw 'Ein erforderlicher GitHub-Workflow war nicht erfolgreich.'
            }
        }
        if ($null -ne $core -and $null -ne $build -and
            $core.status -eq 'completed' -and $build.status -eq 'completed' -and
            $core.conclusion -eq 'success' -and $build.conclusion -eq 'success') { break }
        if (($null -eq $core -or $null -eq $build) -and (Get-Date) -ge $discoveryDeadline) {
            throw "Nicht beide Push-Workflows gefunden. Wurde Commit $shortSha auf '$Branch' gepusht?"
        }
        Start-Sleep -Seconds $PollSeconds
    }
    if ($null -eq $core -or $null -eq $build -or
        $core.conclusion -ne 'success' -or $build.conclusion -ne 'success') {
        throw "Timeout: GitHub Actions nicht nach $TimeoutMinutes Minuten erfolgreich abgeschlossen."
    }

    Write-Status 'Beide Workflows fuer denselben Commit erfolgreich.' 'Green'
    $buildId = [long]$build.databaseId
    $artifact = Get-Artifact -BuildRunId $buildId
    $destination = Join-Path $outputRoot ("run-{0}-{1}" -f $buildId, $shortSha)

    if (Test-Path -LiteralPath $destination) {
        if (Test-CachedDownload -Target $destination -Artifact $artifact -BuildRunId $buildId) {
            Write-Status "Artifact bereits vollstaendig vorhanden: $destination" 'Green'
            exit 0
        }
        throw "Zielordner existiert, aber Inhalt ist nicht verifiziert: $destination (nicht ueberschrieben)."
    }

    New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null
    $staging = Join-Path $outputRoot ('.download-{0}' -f [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $staging | Out-Null
    try {
        Write-Status "Lade '$($artifact.name)' aus Build-Run $buildId ..." 'Cyan'
        & gh run download ([string]$buildId) -R $Repo --name ([string]$artifact.name) --dir $staging
        if ($LASTEXITCODE -ne 0) { throw 'Artifact-Download fehlgeschlagen.' }
        $jars = @(Get-ValidJars -Directory $staging)
        if ($jars.Count -eq 0) {
            throw 'Artifact enthaelt keine installierbare Haupt-JAR (nur Quellen oder keine JAR).'
        }
        $manifest = [ordered]@{
            Repository = $Repo
            Branch = $Branch
            Commit = $Commit
            RunId = $buildId
            ArtifactId = [long]$artifact.id
            Artifact = [string]$artifact.name
            DownloadedUtc = (Get-Date).ToUniversalTime().ToString('o')
            RuntimeVerified = $false
            Jars = @($jars | ForEach-Object {
                [ordered]@{
                    File = $_.FullName.Substring($staging.Length).TrimStart([char[]]@('\', '/'))
                    SHA256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
                }
            })
        }
        $manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $staging 'build-monitor.json') -Encoding UTF8
        if (Test-Path -LiteralPath $destination) { throw 'Zielordner wurde parallel angelegt; Abbruch.' }
        Move-Item -LiteralPath $staging -Destination $destination
        Write-Status "Download abgeschlossen: $destination" 'Green'
        foreach ($jar in $jars) { Write-Status "JAR: $($jar.Name)" 'Green' }
        Write-Status 'CI-erfolgreich, Minecraft-Laufzeittest weiterhin offen (unverified-runtime).' 'Yellow'
    } finally {
        if (Test-Path -LiteralPath $staging) {
            Remove-Item -LiteralPath $staging -Recurse -Force
        }
    }
} catch {
    Write-Status "ABBRUCH: $($_.Exception.Message)" 'Red'
    exit 1
}
