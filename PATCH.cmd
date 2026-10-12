@echo off
setlocal
cd /d "%~dp0"
where pwsh >nul 2>nul
if errorlevel 1 (
  echo PowerShell 7 is required. Install pwsh first.
  exit /b 1
)
pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\apply-update.ps1" %*
if errorlevel 1 (
  echo.
  echo Patch process FAILED; see error above. No automatic retries.
  exit /b 1
)
