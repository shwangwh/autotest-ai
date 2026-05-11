[CmdletBinding()]
param(
    [switch]$Restart
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimeRoot = Join-Path $projectRoot "runtime"
$pidRoot = Join-Path $runtimeRoot "pids"
$logRoot = Join-Path $runtimeRoot "logs"
$measureScript = Join-Path $projectRoot ".agents\skills\fast-start-stop-system\scripts\Measure-Startup.ps1"
$stopScript = Join-Path $projectRoot "scripts\stop-fast.ps1"
$backendDir = Join-Path $projectRoot "backend"
$frontendDir = Join-Path $projectRoot "frontend"
$backendPid = Join-Path $pidRoot "backend.pid"
$frontendPid = Join-Path $pidRoot "frontend.pid"
$backendLog = Join-Path $logRoot "backend.log"
$frontendLog = Join-Path $logRoot "frontend.log"
$backendPort = 8082
$frontendPort = 3000

New-Item -ItemType Directory -Force -Path $pidRoot | Out-Null
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null

function Get-ListeningProcessIds {
    param(
        [Parameter(Mandatory = $true)]
        [int[]]$Ports
    )

    $result = New-Object System.Collections.Generic.List[int]
    foreach ($port in $Ports) {
        try {
            $connections = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction Stop
            foreach ($connection in $connections) {
                if ($connection.OwningProcess -gt 0 -and -not $result.Contains([int]$connection.OwningProcess)) {
                    $result.Add([int]$connection.OwningProcess)
                }
            }
        }
        catch {
        }
    }

    return @($result)
}

if ($Restart) {
    & $stopScript | Out-Null
}

$listeningPids = @(Get-ListeningProcessIds -Ports @($frontendPort, $backendPort))
if ($listeningPids.Count -gt 0) {
    throw "Ports $frontendPort/$backendPort are already in use. Run scripts\stop-fast.ps1 first, or rerun start-fast.ps1 with -Restart."
}

$mavenCmd = Join-Path $projectRoot "apache-maven-3.9.9\bin\mvn.cmd"
if (-not (Test-Path -LiteralPath $mavenCmd)) {
    $mavenCmd = "mvn.cmd"
}

$worker = {
    param(
        $MeasureScript,
        $Command,
        $WorkingDirectory,
        $ReadyPort,
        $TimeoutSeconds,
        $LogPath,
        $PidPath
    )

    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $MeasureScript `
        -Command $Command `
        -WorkingDirectory $WorkingDirectory `
        -ReadyPort $ReadyPort `
        -TimeoutSeconds $TimeoutSeconds `
        -LogPath $LogPath `
        -PidPath $PidPath | ConvertTo-Json -Compress -Depth 4
}

$backendCommand = "& '$mavenCmd' clean spring-boot:run -DskipTests"
$frontendCommand = "& npm.cmd run dev -- --host 0.0.0.0 --port $frontendPort"

$backendJob = Start-Job -ScriptBlock $worker -ArgumentList $measureScript, $backendCommand, $backendDir, $backendPort, 180, $backendLog, $backendPid
$frontendJob = Start-Job -ScriptBlock $worker -ArgumentList $measureScript, $frontendCommand, $frontendDir, $frontendPort, 90, $frontendLog, $frontendPid

$jobs = @($backendJob, $frontendJob)
$null = Wait-Job -Job $jobs -Timeout 240

$results = foreach ($job in $jobs) {
    $payload = Receive-Job -Job $job
    if ([string]::IsNullOrWhiteSpace($payload)) {
        [pscustomobject]@{
            Ready = $false
            Exited = $true
            ExitCode = -1
            ElapsedMilliseconds = 0
            ReadyPort = $null
            LogPath = $null
            PidPath = $null
        }
        continue
    }

    foreach ($item in @($payload | ConvertFrom-Json)) {
        if ($item -is [psobject] -and $item.PSObject.Properties.Name -contains "ReadyPort") {
            $item
        }
    }
}

Remove-Job -Job $jobs -Force | Out-Null

$backendResult = $results | Where-Object { $_ -and $_.PSObject.Properties.Name -contains "ReadyPort" -and $_.ReadyPort -eq $backendPort } | Select-Object -First 1
$frontendResult = $results | Where-Object { $_ -and $_.PSObject.Properties.Name -contains "ReadyPort" -and $_.ReadyPort -eq $frontendPort } | Select-Object -First 1

# 确保结果对象存在
if (-not $backendResult) {
    $backendResult = [pscustomobject]@{
        Ready = $false
        Exited = $true
        ExitCode = -1
        ElapsedMilliseconds = 0
        ReadyPort = $backendPort
        LogPath = $null
        PidPath = $null
    }
}

if (-not $frontendResult) {
    $frontendResult = [pscustomobject]@{
        Ready = $false
        Exited = $true
        ExitCode = -1
        ElapsedMilliseconds = 0
        ReadyPort = $frontendPort
        LogPath = $null
        PidPath = $null
    }
}

[pscustomobject]@{
    StartedAt = Get-Date
    Backend = $backendResult
    Frontend = $frontendResult
    BackendUrl = "http://localhost:$backendPort"
    FrontendUrl = "http://localhost:$frontendPort"
    Ready = [bool]($backendResult.Ready -and $frontendResult.Ready)
} | ConvertTo-Json -Depth 6

if (-not ($backendResult.Ready -and $frontendResult.Ready)) {
    exit 1
}
