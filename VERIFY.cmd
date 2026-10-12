@echo off
setlocal
cd /d "%~dp0"
where pwsh >nul 2>nul
if errorlevel 1 (
  echo PowerShell 7 required. Install pwsh.
  exit /b 1
)
pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\test-core.ps1"
if errorlevel 1 exit /b 1
echo.
echo Core tests passed. NeoForge compile and in-game verification still need GitHub Actions / DEV world.
