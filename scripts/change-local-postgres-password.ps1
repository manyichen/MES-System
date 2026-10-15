[CmdletBinding()]
param(
    [string]$PostgresHost = "127.0.0.1",
    [ValidateRange(1, 65535)]
    [int]$PostgresPort = 5432,
    [string]$Superuser = "postgres"
)

$ErrorActionPreference = "Stop"
$psql = Get-Command psql.exe -ErrorAction SilentlyContinue
if ($psql) {
    $psqlPath = $psql.Source
}
else {
    $psqlPath = Get-ChildItem -LiteralPath "C:\Program Files\PostgreSQL" -Directory -ErrorAction SilentlyContinue |
        Sort-Object { [int](($_.Name -split '\.')[0]) } -Descending |
        ForEach-Object { Join-Path $_.FullName "bin\psql.exe" } |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1
}
if (-not $psqlPath) {
    throw "psql.exe was not found."
}

Write-Host "Enter the CURRENT postgres password first."
Write-Host "Then enter the NEW password twice. Password input is hidden."
& $psqlPath --host=$PostgresHost --port=$PostgresPort --username=$Superuser `
    --dbname=postgres --password --command="\password $Superuser"
if ($LASTEXITCODE -ne 0) {
    throw "Password change failed with exit code $LASTEXITCODE."
}
Write-Host "PostgreSQL superuser password changed."
