[CmdletBinding()]
param(
    [string]$DatabaseHost = '127.0.0.1',
    [int]$DatabasePort = 5432,
    [string]$AdminUser = 'postgres',
    [SecureString]$AdminPassword
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Find-PostgresTool {
    param([Parameter(Mandatory)][string]$Name)

    $command = Get-Command "$Name.exe" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $postgresRoot = Join-Path $env:ProgramFiles 'PostgreSQL'
    if (Test-Path -LiteralPath $postgresRoot) {
        $tool = Get-ChildItem -LiteralPath $postgresRoot -Directory |
            Sort-Object {
                if ($_.Name -match '^\d+$') {
                    [int]$_.Name
                }
                else {
                    0
                }
            } -Descending |
            ForEach-Object {
                Join-Path $_.FullName "bin\$Name.exe"
            } |
            Where-Object {
                Test-Path -LiteralPath $_
            } |
            Select-Object -First 1

        if ($tool) {
            return $tool
        }
    }

    throw "No se encontro $Name.exe. Instala PostgreSQL para Windows o agrega su carpeta bin al PATH."
}

function Invoke-Psql {
    param(
        [Parameter(Mandatory)][string]$Psql,
        [Parameter(Mandatory)][string]$Database,
        [Parameter(Mandatory)][string]$Sql,
        [switch]$TuplesOnly
    )

    $arguments = @(
        '-X',
        '-w',
        '-v', 'ON_ERROR_STOP=1',
        '-h', $DatabaseHost,
        '-p', $DatabasePort,
        '-U', $AdminUser,
        '-d', $Database
    )

    if ($TuplesOnly) {
        $arguments += @('-A', '-t')
    }

    $arguments += @('-c', $Sql)
    $output = & $Psql @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "PostgreSQL termino con codigo $LASTEXITCODE."
    }

    return $output
}

$psql = Find-PostgresTool -Name 'psql'
$createdb = Find-PostgresTool -Name 'createdb'

if (-not $AdminPassword) {
    $AdminPassword = Read-Host 'Clave del usuario administrador de PostgreSQL' -AsSecureString
}

$passwordPointer = [IntPtr]::Zero
try {
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($AdminPassword)
    $env:PGPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)

    Invoke-Psql `
        -Psql $psql `
        -Database 'postgres' `
        -Sql 'SELECT 1;' `
        -TuplesOnly | Out-Null

    $rolesSql = @'
DO $roles$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'identity_user') THEN
        CREATE ROLE identity_user LOGIN PASSWORD 'identity_password';
    ELSE
        ALTER ROLE identity_user WITH LOGIN PASSWORD 'identity_password';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'catalog') THEN
        CREATE ROLE catalog LOGIN PASSWORD 'catalog';
    ELSE
        ALTER ROLE catalog WITH LOGIN PASSWORD 'catalog';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'commerce') THEN
        CREATE ROLE commerce LOGIN PASSWORD 'commerce';
    ELSE
        ALTER ROLE commerce WITH LOGIN PASSWORD 'commerce';
    END IF;
END
$roles$;
'@

    Invoke-Psql `
        -Psql $psql `
        -Database 'postgres' `
        -Sql $rolesSql | Out-Null

    $databases = @(
        @{ Name = 'identity_db'; Owner = 'identity_user' },
        @{ Name = 'catalogdb'; Owner = 'catalog' },
        @{ Name = 'commercedb'; Owner = 'commerce' }
    )

    foreach ($database in $databases) {
        $databaseName = $database.Name
        $databaseOwner = $database.Owner
        $exists = Invoke-Psql `
            -Psql $psql `
            -Database 'postgres' `
            -Sql "SELECT 1 FROM pg_database WHERE datname = '$databaseName';" `
            -TuplesOnly

        if (-not $exists) {
            & $createdb `
                -w `
                -h $DatabaseHost `
                -p $DatabasePort `
                -U $AdminUser `
                -O $databaseOwner `
                $databaseName

            if ($LASTEXITCODE -ne 0) {
                throw "No se pudo crear la base $databaseName."
            }
        }

        Invoke-Psql `
            -Psql $psql `
            -Database 'postgres' `
            -Sql "ALTER DATABASE $databaseName OWNER TO $databaseOwner;" | Out-Null
    }

    Write-Host ''
    Write-Host 'PostgreSQL quedo preparado para los servicios:' -ForegroundColor Green
    Write-Host '  identity_db -> identity_user'
    Write-Host '  catalogdb   -> catalog'
    Write-Host '  commercedb  -> commerce'
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}
