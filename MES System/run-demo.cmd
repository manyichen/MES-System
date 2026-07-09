@echo off
setlocal
cd /d "%~dp0"

set MAVEN=..\.tools\apache-maven-3.9.11\bin\mvn.cmd
if not exist "%MAVEN%" (
  echo Maven not found: %MAVEN%
  exit /b 1
)

call "%MAVEN%" "-Dmaven.repo.local=..\.m2\repository" test
if errorlevel 1 exit /b 1

echo.
echo Starting MES demo server at http://127.0.0.1:8080/
java -cp target\classes com.example.messystem.demo.DemoServer 8080
