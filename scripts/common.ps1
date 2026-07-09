Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-ProjectRoot {
    [CmdletBinding()]
    param()

    return (Split-Path -Parent $PSScriptRoot)
}

function Assert-CommandExists {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [string]$InstallHint
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required command '$Name'. $InstallHint"
    }
}

function Get-GradleCommand {
    [CmdletBinding()]
    param()

    $projectRoot = Get-ProjectRoot
    $wrapperPath = Join-Path $projectRoot "gradlew.bat"
    if (Test-Path $wrapperPath) {
        return $wrapperPath
    }

    Assert-CommandExists -Name "gradle" -InstallHint "Install Gradle 8+ or add gradlew.bat to the project root."
    return "gradle"
}
