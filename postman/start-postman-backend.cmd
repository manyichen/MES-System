@echo off
rem Windows 答辩辅助入口：在独立 18084 端口启动后端，避免占用日常 8080。
rem 此窗口必须保持打开；Postman 环境 baseUrl 应指向 http://127.0.0.1:18084。
setlocal
title MES Postman Test Backend - Port 18084
cd /d "%~dp0..\backend"

rem 自动化测试不需要启动浏览器，减少无关窗口和焦点切换。
set "MES_OPEN_BROWSER=false"

echo ============================================================
echo MES Postman test backend
echo URL: http://127.0.0.1:18084
echo Keep this window open while Postman is running.
echo Press Ctrl+C to stop the backend after testing.
echo ============================================================
echo.

rem 使用仓库固定 Maven，编译后执行 MesBackendApplication；Ctrl+C 会停止内嵌 Tomcat。
call "..\.tools\apache-maven-3.9.11\bin\mvn.cmd" -DskipTests "-Dmes.port=18084" "-Dmes.host=127.0.0.1" compile exec:java

echo.
echo The backend has stopped or failed to start.
echo Review the message above, then press any key to close this window.
pause >nul
