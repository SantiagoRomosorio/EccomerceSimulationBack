@echo off
setlocal
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-postgres.ps1" %*
exit /b %ERRORLEVEL%
