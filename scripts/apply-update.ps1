#Requires -Version 7.0
param([string]$PatchPath,[switch]$SkipPush)
$ErrorActionPreference='Stop'
Set-Location (Join-Path $PSScriptRoot '..')
function Check-Git([string[]]$Commands) {
  & git @Commands
  if ($LASTEXITCODE -ne 0) { throw "git $($Commands -join ' ') failed (exit $LASTEXITCODE)" }
}
$repo=(git rev-parse --show-toplevel).Trim()
if (-not $repo) { throw 'Not a Git repository' }
$st=(git status --porcelain)
if ($st) { throw "Repository not clean! Resolve changes before applying the patch:`n$st" }
$inProgress=(git rev-parse --git-path rebase-apply).Trim()
if(Test-Path -LiteralPath $inProgress) { throw "An unfinished git am/rebase exists: $inProgress. Resolve it before updating." }
if(-not $PatchPath) {
  try { $downloads=(New-Object -ComObject Shell.Application).NameSpace('shell:Downloads').Self.Path }
  catch { $downloads=Join-Path $env:USERPROFILE 'Downloads' }
  if(-not (Test-Path -LiteralPath $downloads)) { throw "Downloads missing: $downloads" }
  $matches=@(Get-ChildItem -LiteralPath $downloads -File -Filter 'Rogler-*-v*.patch' |
    Sort-Object LastWriteTime -Descending)
  if($matches.Count -eq 0) { throw "No TeamWars patch found in $downloads" }
  $PatchPath=$matches[0].FullName
}
if(-not(Test-Path -LiteralPath $PatchPath)) { throw "Patch file not found: $PatchPath" }
$branch=(git branch --show-current).Trim()
if (-not $branch) { throw 'Refusing detached HEAD.' }
# Do not re-apply an already committed patch. A major cause of earlier git-am failures.
$subject = Select-String -LiteralPath $PatchPath -Pattern '^Subject: \[PATCH\] (.*)$' | Select-Object -First 1
if($subject) {
  $name = $subject.Matches[0].Groups[1].Value
  $history = @(git log --all -30 --format='%s')
  if($history -contains $name) { throw "Patch already committed: $name" }
}
# Each downloaded release starts on its own branch; never silently mix versions.
if ([IO.Path]::GetFileName($PatchPath) -match 'Rogler-(?:TeamWars|Core)-(v[0-9]+\.[0-9]+\.[0-9]+)-update\.patch') {
  $version = $Matches[1]
  if ($branch -notlike "*$version*") {
    $nextBranch = "feat/$version-update"
    if (& git branch --list $nextBranch) { throw "Branch $nextBranch already exists. Switch to it and inspect before retrying." }
    & git switch -c $nextBranch
    if ($LASTEXITCODE -ne 0) { throw "Could not create $nextBranch" }
    $branch = $nextBranch
  }
}
if ($branch -eq 'main') { throw 'Refusing to apply patch directly to main.' }
Write-Host "Applying patch $PatchPath to $branch"
& git apply --check -- $PatchPath
if($LASTEXITCODE -ne 0) { throw 'Patch is incompatible with this repository; NOTHING was applied. Check source branch and patch version.' }
# Avoid fake-ancestor hash failures and unexpected opportunistic Git GC on Windows.
& git -c gc.auto=0 am --no-3way -- $PatchPath
if($LASTEXITCODE -ne 0) { throw 'git am failed. DO NOT push or retry; inspect git status. The patch was preserved in Downloads.' }
# The patch commit must actually carry the expected Gradle/mod version.
if ([IO.Path]::GetFileName($PatchPath) -match 'Rogler-(?:TeamWars|Core)-(v[0-9]+\.[0-9]+\.[0-9]+)-update\.patch') {
  $expected = $Matches[1].Substring(1)
  $actual = (Get-Content (Join-Path $repo 'mod-neoforge/gradle.properties') | Where-Object { $_ -match '^mod_version=' } | Select-Object -First 1)
  if ($actual -ne "mod_version=$expected") { throw "Patch committed but mod_version is $actual, expected $expected. Do NOT push!" }
}
& cmd /c (Join-Path $repo 'VERIFY.cmd')
if ($LASTEXITCODE -ne 0) { throw 'Core tests failed; not pushing.' }
# Always produce an EXACT full tracked-source ZIP from the real user repository.
# This includes all original GitHub scripts, LLM handovers and historical docs,
# unlike older offline source snapshots with omitted monitor files.
$versionLine=(Get-Content (Join-Path $repo 'mod-neoforge/gradle.properties') |
    Where-Object { $_ -match '^mod_version=' } | Select-Object -First 1)
if(-not $versionLine) { throw 'Missing mod_version in gradle.properties.' }
$releaseVersion=($versionLine -split '=',2)[1].Trim()
$releasePath=(Resolve-Path -LiteralPath $PatchPath).Path
$sourceZip=Join-Path (Split-Path -Parent $releasePath) "Rogler-Core-v$releaseVersion-full-source-exact.zip"
if(Test-Path -LiteralPath $sourceZip) { throw "Refusing to overwrite existing source snapshot: $sourceZip" }
& git archive --format=zip "--output=$sourceZip" HEAD
if ($LASTEXITCODE -ne 0) { throw 'Exact full-source ZIP generation failed; not pushing.' }
Write-Host "Exact full tracked-source ZIP: $sourceZip"
if (-not $SkipPush) {
  & git push -u origin $branch
  if ($LASTEXITCODE -ne 0) { throw 'Push failed; patch was committed locally but not uploaded.' }
  if (Test-Path (Join-Path $PSScriptRoot 'watch-actions.ps1')) {
    & pwsh -NoProfile -File (Join-Path $PSScriptRoot 'watch-actions.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'CI watcher failed; inspect GitHub Actions.' }
  }
}
Write-Host 'Update completed. Original patch remains in Downloads (safe to delete).'
