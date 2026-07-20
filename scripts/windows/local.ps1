[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('start', 'status', 'stop')]
    [string]$Action = 'status',

    [switch]$SkipBuild,

    [ValidateRange(15, 600)]
    [int]$StartupTimeoutSeconds = 120
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$runtimeRoot = Join-Path $env:LOCALAPPDATA 'EcommerceSimulationBack'
$statePath = Join-Path $runtimeRoot 'local-processes.json'
$internalToken = $env:INTERNAL_GATEWAY_TOKEN
if ([string]::IsNullOrWhiteSpace($internalToken)) {
    $internalToken = 'local-internal-gateway-token-change-me'
}

function Find-CommandPath {
    param(
        [Parameter(Mandatory)][string]$CommandName,
        [string]$HomeVariable,
        [string]$RelativePath
    )

    if ($HomeVariable) {
        $homePath = [Environment]::GetEnvironmentVariable($HomeVariable)
        if (-not $homePath) {
            $homePath = [Environment]::GetEnvironmentVariable($HomeVariable, 'User')
        }
        if ($homePath) {
            $candidate = Join-Path $homePath $RelativePath
            if (Test-Path -LiteralPath $candidate) {
                return $candidate
            }
        }
    }

    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    throw "No se encontro $CommandName. Revisa JAVA_HOME, MAVEN_HOME y PATH."
}

function Find-PgIsReady {
    $command = Get-Command 'pg_isready.exe' -ErrorAction SilentlyContinue
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
                Join-Path $_.FullName 'bin\pg_isready.exe'
            } |
            Where-Object {
                Test-Path -LiteralPath $_
            } |
            Select-Object -First 1

        if ($tool) {
            return $tool
        }
    }

    throw 'No se encontro pg_isready.exe. Instala PostgreSQL para Windows o agrega su carpeta bin al PATH.'
}

function Get-PostgresService {
    $candidate = Get-CimInstance -ClassName Win32_Service -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -like 'postgresql*' -or
            $_.PathName -match '(?i)\\PostgreSQL\\.*\\pg_ctl\.exe'
        } |
        Sort-Object `
            @{ Expression = { if ($_.State -eq 'Running') { 1 } else { 0 } }; Descending = $true },
            @{
                Expression = {
                    if ($_.PathName -match '(?i)\\PostgreSQL\\(\d+)\\') {
                        [int]$Matches[1]
                    }
                    else {
                        0
                    }
                }
                Descending = $true
            } |
        Select-Object -First 1

    if (-not $candidate) {
        return $null
    }

    return Get-Service -Name $candidate.Name -ErrorAction SilentlyContinue
}

function Test-PostgresReady {
    try {
        $pgIsReady = Find-PgIsReady
        & $pgIsReady -h 127.0.0.1 -p 5432 | Out-Null
        return $LASTEXITCODE -eq 0
    }
    catch {
        return $false
    }
}

function Assert-PostgresReady {
    if (Test-PostgresReady) {
        return
    }

    $service = Get-PostgresService
    if (-not $service) {
        throw 'PostgreSQL no responde en 127.0.0.1:5432 y no se encontro un servicio de Windows para iniciarlo.'
    }

    if ($service.Status -ne 'Running') {
        try {
            Start-Service -Name $service.Name
            $service.WaitForStatus('Running', [TimeSpan]::FromSeconds(15))
        }
        catch {
            throw "PostgreSQL esta detenido. Abre PowerShell como administrador y ejecuta: Start-Service $($service.Name)"
        }
    }

    if (-not (Test-PostgresReady)) {
        throw 'PostgreSQL no acepta conexiones en 127.0.0.1:5432.'
    }
}

function Read-LocalState {
    if (-not (Test-Path -LiteralPath $statePath)) {
        return $null
    }

    return Get-Content -Raw -LiteralPath $statePath | ConvertFrom-Json
}

function Save-LocalState {
    param(
        [Parameter(Mandatory)][array]$Started,
        [Parameter(Mandatory)][string]$StartedAt,
        [Parameter(Mandatory)][string]$LogDirectory
    )

    New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
    [pscustomobject]@{
        StartedAt = $StartedAt
        LogDirectory = $LogDirectory
        Processes = @(
            $Started |
                ForEach-Object {
                    [pscustomobject]@{
                        Name = $_.Name
                        Module = $_.Module
                        Port = $_.Port
                        ProcessId = $_.Process.Id
                        Jar = $_.Jar
                        Stdout = $_.Stdout
                        Stderr = $_.Stderr
                    }
                }
        )
    } |
        ConvertTo-Json -Depth 5 |
        Set-Content -LiteralPath $statePath -Encoding UTF8
}

function Get-LiveStateProcesses {
    param($State)

    if (-not $State) {
        return @()
    }

    $live = @()
    foreach ($entry in @($State.Processes)) {
        $process = Get-Process -Id ([int]$entry.ProcessId) -ErrorAction SilentlyContinue
        if ($process) {
            $live += [pscustomobject]@{
                Entry = $entry
                Process = $process
            }
        }
    }

    return $live
}

function Get-ServiceJar {
    param([Parameter(Mandatory)][string]$Module)

    $target = Join-Path $projectRoot "$Module\target"
    $jar = Get-ChildItem -LiteralPath $target -Filter "$Module-*.jar" -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -notlike '*-sources.jar' -and
            $_.Name -notlike '*-javadoc.jar'
        } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $jar) {
        throw "No se encontro el JAR ejecutable de $Module."
    }

    return $jar.FullName
}

function Assert-ApplicationPortsAvailable {
    foreach ($port in 8080, 8081, 8082, 8083) {
        $listener = Get-NetTCPConnection `
            -State Listen `
            -LocalPort $port `
            -ErrorAction SilentlyContinue

        if ($listener) {
            throw "El puerto $port ya esta ocupado por el proceso $($listener[0].OwningProcess)."
        }
    }
}

function Stop-StartedProcesses {
    param([array]$Started)

    for ($index = $Started.Count - 1; $index -ge 0; $index--) {
        $process = $Started[$index].Process
        if ($process -and -not $process.HasExited) {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
            Wait-Process -Id $process.Id -Timeout 10 -ErrorAction SilentlyContinue
        }
    }
}

function Wait-ForStack {
    param(
        [Parameter(Mandatory)][array]$Started,
        [Parameter(Mandatory)][hashtable]$Headers,
        [Parameter(Mandatory)][int]$TimeoutSeconds
    )

    $checks = @(
        @{ Name = 'identity'; Uri = 'http://127.0.0.1:8081/actuator/health'; Headers = $Headers },
        @{ Name = 'catalog'; Uri = 'http://127.0.0.1:8082/actuator/health'; Headers = $Headers },
        @{ Name = 'commerce'; Uri = 'http://127.0.0.1:8083/actuator/health'; Headers = $Headers },
        @{ Name = 'gateway'; Uri = 'http://127.0.0.1:8080/actuator/health'; Headers = @{} }
    )

    $ready = @{}
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    do {
        foreach ($startedProcess in $Started) {
            $startedProcess.Process.Refresh()
            if ($startedProcess.Process.HasExited) {
                throw "$($startedProcess.Name) termino durante el arranque."
            }
        }

        foreach ($check in $checks) {
            if ($ready[$check.Name]) {
                continue
            }

            try {
                $response = Invoke-RestMethod `
                    -Uri $check.Uri `
                    -Headers $check.Headers `
                    -Method Get `
                    -TimeoutSec 2

                if ($response.status -eq 'UP') {
                    $ready[$check.Name] = $true
                }
            }
            catch {
                # El servicio todavia esta arrancando.
            }
        }

        if ($ready.Count -eq $checks.Count) {
            break
        }

        Start-Sleep -Milliseconds 750
    }
    while ((Get-Date) -lt $deadline)

    $missing = @(
        $checks |
            Where-Object {
                -not $ready[$_.Name]
            } |
            ForEach-Object {
                $_.Name
            }
    )

    if ($missing.Count -gt 0) {
        throw "No alcanzaron estado UP: $($missing -join ', ')."
    }

    foreach ($uri in @(
        'http://127.0.0.1:8080/api/identity/health',
        'http://127.0.0.1:8080/api/catalog/health',
        'http://127.0.0.1:8080/api/commerce/health'
    )) {
        Invoke-RestMethod -Uri $uri -Method Get -TimeoutSec 5 | Out-Null
    }
}

function Write-RecentLogs {
    param([array]$Started)

    foreach ($startedProcess in $Started) {
        Write-Host ''
        Write-Host "--- $($startedProcess.Name) ---" -ForegroundColor Yellow
        Get-Content `
            -LiteralPath $startedProcess.Stdout `
            -Tail 20 `
            -ErrorAction SilentlyContinue
        Get-Content `
            -LiteralPath $startedProcess.Stderr `
            -Tail 20 `
            -ErrorAction SilentlyContinue
    }
}

function Start-LocalStack {
    $existingState = Read-LocalState
    $existingProcesses = @(Get-LiveStateProcesses -State $existingState)
    if ($existingProcesses.Count -gt 0) {
        throw 'El stack ya tiene procesos activos. Usa local.cmd status o local.cmd stop.'
    }

    if ($existingState) {
        Remove-Item -LiteralPath $statePath -Force
    }

    Assert-PostgresReady
    Assert-ApplicationPortsAvailable

    $java = Find-CommandPath `
        -CommandName 'java.exe' `
        -HomeVariable 'JAVA_HOME' `
        -RelativePath 'bin\java.exe'

    if (-not $SkipBuild) {
        $maven = Find-CommandPath `
            -CommandName 'mvn.cmd' `
            -HomeVariable 'MAVEN_HOME' `
            -RelativePath 'bin\mvn.cmd'

        Push-Location $projectRoot
        try {
            & $maven '-DskipTests' 'package'
            if ($LASTEXITCODE -ne 0) {
                throw "Maven termino con codigo $LASTEXITCODE."
            }
        }
        finally {
            Pop-Location
        }
    }

    $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $logDirectory = Join-Path $runtimeRoot "logs\$timestamp"
    New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null

    $specs = @(
        @{ Name = 'identity'; Module = 'identity-service'; Port = 8081; Extra = '' },
        @{ Name = 'catalog'; Module = 'catalog-service'; Port = 8082; Extra = '' },
        @{
            Name = 'commerce'
            Module = 'commerce-service'
            Port = 8083
            Extra = '--clients.catalog.base-url=http://localhost:8082'
        },
        @{ Name = 'gateway'; Module = 'api-gateway'; Port = 8080; Extra = '' }
    )

    $started = @()
    $startedAt = (Get-Date).ToString('o')
    $succeeded = $false
    try {
        foreach ($spec in $specs) {
            $jar = Get-ServiceJar -Module $spec.Module
            $stdout = Join-Path $logDirectory "$($spec.Name).out.log"
            $stderr = Join-Path $logDirectory "$($spec.Name).err.log"
            $arguments = "-jar `"$jar`" --spring.profiles.active=local --server.port=$($spec.Port)"
            if ($spec.Extra) {
                $arguments += " $($spec.Extra)"
            }

            $process = Start-Process `
                -FilePath $java `
                -ArgumentList $arguments `
                -WorkingDirectory $projectRoot `
                -WindowStyle Hidden `
                -RedirectStandardOutput $stdout `
                -RedirectStandardError $stderr `
                -PassThru

            $started += [pscustomobject]@{
                Name = $spec.Name
                Module = $spec.Module
                Port = $spec.Port
                Jar = $jar
                Process = $process
                Stdout = $stdout
                Stderr = $stderr
            }
            Save-LocalState `
                -Started $started `
                -StartedAt $startedAt `
                -LogDirectory $logDirectory
        }

        $headers = @{
            'X-Internal-Gateway-Token' = $internalToken
        }
        Wait-ForStack `
            -Started $started `
            -Headers $headers `
            -TimeoutSeconds $StartupTimeoutSeconds

        $succeeded = $true
        Write-Host ''
        Write-Host 'Stack local iniciado correctamente.' -ForegroundColor Green
        Write-Host 'Gateway: http://localhost:8080'
        Write-Host 'Swagger: http://localhost:8080/swagger-ui.html'
        Write-Host "Logs:    $logDirectory"
    }
    catch {
        Write-RecentLogs -Started $started
        throw
    }
    finally {
        if (-not $succeeded) {
            Stop-StartedProcesses -Started $started
            Remove-Item -LiteralPath $statePath -Force -ErrorAction SilentlyContinue
        }
    }
}

function Show-LocalStatus {
    $postgres = Get-PostgresService
    if ($postgres) {
        Write-Host "PostgreSQL: $($postgres.Status) ($($postgres.Name))"
    }
    elseif (Test-PostgresReady) {
        Write-Host 'PostgreSQL: Ready (instancia manual o servicio personalizado)'
    }
    else {
        Write-Host 'PostgreSQL: no encontrado'
    }

    $state = Read-LocalState
    if (-not $state) {
        Write-Host 'Stack Java: detenido'
        return
    }

    $rows = foreach ($entry in @($state.Processes)) {
        $process = Get-Process -Id ([int]$entry.ProcessId) -ErrorAction SilentlyContinue
        [pscustomobject]@{
            Service = $entry.Name
            Port = $entry.Port
            ProcessId = $entry.ProcessId
            Status = if ($process) { 'Running' } else { 'Stopped' }
        }
    }

    $rows | Format-Table -AutoSize | Out-Host

    try {
        $gateway = Invoke-RestMethod `
            -Uri 'http://127.0.0.1:8080/actuator/health' `
            -Method Get `
            -TimeoutSec 3
        Write-Host "Gateway health: $($gateway.status)"
    }
    catch {
        Write-Host 'Gateway health: no disponible'
    }

    Write-Host "Logs: $($state.LogDirectory)"
}

function Stop-LocalStack {
    $state = Read-LocalState
    if (-not $state) {
        Write-Host 'El stack local ya esta detenido.'
        return
    }

    $entries = @($state.Processes)
    for ($index = $entries.Count - 1; $index -ge 0; $index--) {
        $entry = $entries[$index]
        $processId = [int]$entry.ProcessId
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if (-not $process) {
            continue
        }

        $systemProcess = Get-CimInstance `
            -ClassName Win32_Process `
            -Filter "ProcessId = $processId" `
            -ErrorAction SilentlyContinue

        if ($systemProcess -and $systemProcess.CommandLine -like "*$($entry.Jar)*") {
            Stop-Process -Id $processId -Force
            Wait-Process -Id $processId -Timeout 10 -ErrorAction SilentlyContinue
            Write-Host "Detenido: $($entry.Name)"
        }
        else {
            Write-Warning "No se detuvo PID $processId porque ya no coincide con $($entry.Name)."
        }
    }

    Remove-Item -LiteralPath $statePath -Force
    Write-Host 'Stack local detenido. PostgreSQL permanece activo.' -ForegroundColor Green
}

try {
    switch ($Action) {
        'start' {
            Start-LocalStack
        }
        'status' {
            Show-LocalStatus
        }
        'stop' {
            Stop-LocalStack
        }
    }
}
catch {
    Write-Error $_
    exit 1
}
