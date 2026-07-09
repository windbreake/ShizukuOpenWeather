[CmdletBinding()]
param()

. (Join-Path $PSScriptRoot "common.ps1")

function Test-PortListening {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Start-LoggedScript {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptPath,

        [Parameter(Mandatory = $true)]
        [string]$LogPrefix,

        [Parameter(Mandatory = $true)]
        [string]$WorkingDirectory
    )

    $shellPath = (Get-Process -Id $PID).Path
    $stdoutPath = Join-Path $script:LogDirectory "$LogPrefix.log"
    $stderrPath = Join-Path $script:LogDirectory "$LogPrefix.err.log"

    return Start-Process `
        -FilePath $shellPath `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $ScriptPath) `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath `
        -WindowStyle Hidden `
        -PassThru
}

$projectRoot = Get-ProjectRoot
$script:LogDirectory = Join-Path $env:TEMP "shizuku-open-weather"
New-Item -ItemType Directory -Force -Path $script:LogDirectory | Out-Null

$apiScript = Join-Path $PSScriptRoot "dev-api.ps1"
$webScript = Join-Path $PSScriptRoot "dev-web.ps1"

if (Test-PortListening -Port 8080) {
    Write-Host "API already appears to be listening on http://127.0.0.1:8080"
}
else {
    $apiProcess = Start-LoggedScript -ScriptPath $apiScript -LogPrefix "api" -WorkingDirectory $projectRoot
    Write-Host "Started API process (PID: $($apiProcess.Id))"
}

if (Test-PortListening -Port 5173) {
    Write-Host "Web dev server already appears to be listening on http://127.0.0.1:5173"
}
else {
    $webProcess = Start-LoggedScript -ScriptPath $webScript -LogPrefix "web" -WorkingDirectory $projectRoot
    Write-Host "Started web process (PID: $($webProcess.Id))"
}

Write-Host "API: http://127.0.0.1:8080/api/health"
Write-Host "Web: http://127.0.0.1:5173"
Write-Host "Logs: $script:LogDirectory"
