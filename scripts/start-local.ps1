[CmdletBinding()]
param(
    [ValidateRange(1, 65535)]
    [int]$Port = 8080,
    [string]$EnvFile
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if (-not $EnvFile) {
    $EnvFile = Join-Path $repoRoot ".env.local"
}
$EnvFile = [IO.Path]::GetFullPath($EnvFile)
if (-not (Test-Path -LiteralPath $EnvFile -PathType Leaf)) {
    throw "Local config not found: $EnvFile. Run .\scripts\setup-local-db.ps1 first."
}

foreach ($line in Get-Content -LiteralPath $EnvFile -Encoding UTF8) {
    $trimmed = $line.Trim()
    if (-not $trimmed -or $trimmed.StartsWith("#") -or -not $trimmed.Contains("=")) {
        continue
    }
    $parts = $trimmed.Split("=", 2)
    $name = $parts[0].Trim()
    $value = $parts[1].Trim()
    if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'")))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($name, $value, "Process")
}

# The local database file intentionally contains no cloud secrets. Reuse only
# the existing DashScope settings from the original .env; never import its
# database variables into the local process.
$baseEnvFile = Join-Path $repoRoot ".env"
$dashScopeNames = @("DASHSCOPE_API_KEY", "DASHSCOPE_BASE_URL", "DASHSCOPE_MODEL")
if (Test-Path -LiteralPath $baseEnvFile -PathType Leaf) {
    foreach ($line in Get-Content -LiteralPath $baseEnvFile -Encoding UTF8) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#") -or -not $trimmed.Contains("=")) {
            continue
        }
        $parts = $trimmed.Split("=", 2)
        $name = $parts[0].Trim()
        if ($name -notin $dashScopeNames -or -not [string]::IsNullOrWhiteSpace(
                [Environment]::GetEnvironmentVariable($name, "Process"))) {
            continue
        }
        $value = $parts[1].Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $maven) {
    $mavenPath = Get-ChildItem (Join-Path $repoRoot ".tools") -Directory -Filter "apache-maven-*" -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        ForEach-Object { Join-Path $_.FullName "bin\mvn.cmd" } |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1
    if (-not $mavenPath) {
        throw "Maven was not found. Install Maven or add mvn.cmd to PATH."
    }
    $mavenExecutable = $mavenPath
}
else {
    $mavenExecutable = $maven.Source
}

Write-Host "Starting MES backend: http://127.0.0.1:$Port"
Write-Host "Database: $env:MES_DB_USER@$env:MES_DB_HOST`:$env:MES_DB_PORT/$env:MES_DB_NAME"
Push-Location $repoRoot
try {
    & $mavenExecutable -pl backend -DskipTests "-Dmes.port=$Port" compile exec:java
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
