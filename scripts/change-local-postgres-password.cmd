@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0change-local-postgres-password.ps1" %*
