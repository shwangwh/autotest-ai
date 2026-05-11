[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$stopProcessTree = Join-Path $projectRoot ".agents\skills\fast-start-stop-system\scripts\Stop-ProcessTree.ps1"
$pidRoot = Join-Path $projectRoot "runtime\pids"
$backendPid = Join-Path $pidRoot "backend.pid"
$frontendPid = Join-Path $pidRoot "frontend.pid"

& $stopProcessTree `
    -PidFile @($backendPid, $frontendPid) `
    -Ports @(8082, 3000)
