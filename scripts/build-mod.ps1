$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location (Join-Path $root 'mod-neoforge')
try {
    if (Test-Path '.\gradlew.bat') {
        & .\gradlew.bat build --stacktrace --no-daemon
    } elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
        & gradle build --stacktrace --no-daemon
    } else {
        throw 'No Gradle executable. Use the GitHub Actions NeoForge Compile workflow or install Gradle 8.10.2. Do not use the core demo JAR as a Minecraft mod.'
    }
    if ($LASTEXITCODE -ne 0) { throw "NeoForge build failed: $LASTEXITCODE" }
    Write-Host 'Build succeeded. Test the JAR from mod-neoforge/build/libs/ on a disposable 1.21.1 NeoForge 21.1.249 server.'
} finally { Pop-Location }
