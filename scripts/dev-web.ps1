[CmdletBinding()]
param()

. (Join-Path $PSScriptRoot "common.ps1")

Assert-CommandExists -Name "npm" -InstallHint "Install Node.js 22 or newer."

$projectRoot = Get-ProjectRoot
$webRoot = Join-Path $projectRoot "apps\web"

Push-Location $webRoot
try {
    npm install
    npm run dev -- --host 127.0.0.1 --port 5173 --strictPort
}
finally {
    Pop-Location
}
