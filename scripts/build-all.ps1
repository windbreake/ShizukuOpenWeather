[CmdletBinding()]
param()

. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Get-ProjectRoot
$gradle = Get-GradleCommand

Push-Location $projectRoot
try {
    & $gradle buildAll
}
finally {
    Pop-Location
}
