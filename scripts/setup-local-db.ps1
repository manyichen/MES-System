[CmdletBinding()]
param(
    [string]$PostgresHost = "127.0.0.1",
    [ValidateRange(1, 65535)]
    [int]$PostgresPort = 5432,
    [ValidatePattern('^[A-Za-z_][A-Za-z0-9_]*$')]
    [string]$Database = "MES",
    [ValidatePattern('^[A-Za-z_][A-Za-z0-9_]*$')]
    [string]$AppUser = "mes_app",
    [string]$Superuser = "postgres",
    [string]$BackupFile,
    [switch]$SkipRestore
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

function Find-PostgresTool([string]$Name) {
    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $installRoot = "C:\Program Files\PostgreSQL"
    if (Test-Path -LiteralPath $installRoot) {
        $candidate = Get-ChildItem -LiteralPath $installRoot -Directory |
            Sort-Object { [int](($_.Name -split '\.')[0]) } -Descending |
            ForEach-Object { Join-Path $_.FullName "bin\$Name.exe" } |
            Where-Object { Test-Path -LiteralPath $_ } |
            Select-Object -First 1
        if ($candidate) {
            return $candidate
        }
    }

    throw "$Name was not found. Install PostgreSQL Server and add its bin directory to PATH."
}

function ConvertFrom-SecureValue([Security.SecureString]$SecureValue) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Quote-PsqlValue([string]$Value) {
    return "'" + $Value.Replace("'", "''") + "'"
}

function Invoke-Psql([string]$Executable, [string]$LoginUser, [string]$DbName, [string]$Sql) {
    $Sql | & $Executable --host=$PostgresHost --port=$PostgresPort --username=$LoginUser `
        --dbname=$DbName --no-password --set=ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) {
        throw "psql failed with exit code $LASTEXITCODE."
    }
}

$psql = Find-PostgresTool "psql"
$pgRestore = Find-PostgresTool "pg_restore"

if (-not $BackupFile) {
    $BackupFile = Join-Path $repoRoot "database\backups\mes_before_kitting_integrity_v12_20260716_222140.backup"
}
$BackupFile = [IO.Path]::GetFullPath($BackupFile)
if (-not $SkipRestore -and -not (Test-Path -LiteralPath $BackupFile -PathType Leaf)) {
    throw "Database backup not found: $BackupFile"
}

$adminSecure = Read-Host "Password for PostgreSQL superuser $Superuser" -AsSecureString
$appSecure = Read-Host "New password for local app user $AppUser (Enter to generate one)" -AsSecureString
$adminPassword = ConvertFrom-SecureValue $adminSecure
$appPassword = ConvertFrom-SecureValue $appSecure
if ([string]::IsNullOrWhiteSpace($appPassword)) {
    $bytes = [byte[]]::new(24)
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $random.GetBytes($bytes)
    }
    finally {
        $random.Dispose()
    }
    $appPassword = [Convert]::ToBase64String($bytes)
}

$oldPgPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $adminPassword
    $quotedUser = Quote-PsqlValue $AppUser
    $quotedPassword = Quote-PsqlValue $appPassword
    $quotedDatabase = Quote-PsqlValue $Database
    $roleSql = @"
SELECT format('CREATE ROLE %I LOGIN', $quotedUser)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = $quotedUser) \gexec
SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', $quotedUser, $quotedPassword) \gexec
SELECT format('CREATE DATABASE %I OWNER %I ENCODING %L', $quotedDatabase, $quotedUser, 'UTF8')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = $quotedDatabase) \gexec
SELECT format('ALTER DATABASE %I OWNER TO %I', $quotedDatabase, $quotedUser) \gexec
"@
    Invoke-Psql $psql $Superuser "postgres" $roleSql

    if (-not $SkipRestore) {
        Write-Host "Restoring local database from: $BackupFile"
        & $pgRestore --host=$PostgresHost --port=$PostgresPort --username=$Superuser `
            --dbname=$Database --no-password --clean --if-exists --no-owner --no-privileges `
            --role=$AppUser $BackupFile
        if ($LASTEXITCODE -ne 0) {
            throw "pg_restore failed with exit code $LASTEXITCODE."
        }

        foreach ($migration in @(
            "database\mes_kitting_integrity_v12.sql",
            "database\mes_work_order_ready_task_line_fix_v12.sql"
        )) {
            $migrationPath = Join-Path $repoRoot $migration
            Write-Host "Applying post-backup migration: $migration"
            Get-Content -LiteralPath $migrationPath -Raw -Encoding UTF8 |
                & $psql --host=$PostgresHost --port=$PostgresPort --username=$Superuser `
                    --dbname=$Database --no-password --set=ON_ERROR_STOP=1
            if ($LASTEXITCODE -ne 0) {
                throw "Migration failed: $migration"
            }
        }
    }

    $grantSql = @"
SELECT format('ALTER SCHEMA public OWNER TO %I', $quotedUser) \gexec
SELECT format('GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO %I', $quotedUser) \gexec
SELECT format('GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO %I', $quotedUser) \gexec
SELECT format('ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO %I', $quotedUser) \gexec
SELECT format('ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO %I', $quotedUser) \gexec
"@
    Invoke-Psql $psql $Superuser $Database $grantSql

    $env:PGPASSWORD = $appPassword
    Invoke-Psql $psql $AppUser $Database "SELECT current_database(), current_user; SELECT count(*) AS user_count FROM mes_user;"

    $localEnv = Join-Path $repoRoot ".env.local"
    $envText = @"
# Generated by scripts/setup-local-db.ps1. Local development only; do not commit.
MES_DB_HOST=$PostgresHost
MES_DB_PORT=$PostgresPort
MES_DB_NAME=$Database
MES_DB_USER=$AppUser
MES_DB_PASSWORD=$appPassword
MES_PUBLIC_URL=http://127.0.0.1:8080
MES_TRACE_STORAGE=storage/trace
AI_PLANNING_ENABLED=false
"@
    [IO.File]::WriteAllText($localEnv, $envText, [Text.UTF8Encoding]::new($false))
    Write-Host "Local database is ready. Application settings: $localEnv"
    Write-Host "Next: .\scripts\start-local.ps1"
}
finally {
    $env:PGPASSWORD = $oldPgPassword
    $adminPassword = $null
    $appPassword = $null
}
