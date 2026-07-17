@echo off
setlocal
title MES Postman Test Backend - Port 18084
cd /d "%~dp0..\backend"

set "MES_OPEN_BROWSER=false"

echo ============================================================
echo MES Postman test backend
echo URL: http://127.0.0.1:18084
echo Keep this window open while Postman is running.
echo Press Ctrl+C to stop the backend after testing.
echo ============================================================
echo.

call "..\.tools\apache-maven-3.9.11\bin\mvn.cmd" -DskipTests "-Dmes.port=18084" "-Dmes.host=127.0.0.1" compile exec:java

echo.
echo The backend has stopped or failed to start.
echo Review the message above, then press any key to close this window.
pause >nul
