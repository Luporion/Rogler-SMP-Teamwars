$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
New-Item -ItemType Directory -Force build/classes | Out-Null
$files = @(Get-ChildItem -Recurse core/src -Filter '*.java' | ForEach-Object { $_.FullName })
& javac --release 21 -encoding UTF-8 -d build/classes $files
if ($LASTEXITCODE -ne 0) { throw "javac failed: $LASTEXITCODE" }
& java -ea -cp build/classes dev.rogler.teamwars.core.CoreTests
if ($LASTEXITCODE -ne 0) { throw "tests failed: $LASTEXITCODE" }
