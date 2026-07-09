[CmdletBinding()]
param(
    [string]$Version = "0.1.0",
    [string]$Configuration = "Release",
    [string]$Runtime = "win-x64",
    [string]$DotnetCommand = "dotnet",
    [string]$InnoSetupCompiler = "C:\Program Files (x86)\Inno Setup 6\ISCC.exe",
    [string]$ArtifactDir = "",
    [switch]$SkipWebBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Get-ProjectRoot
$desktopProject = Join-Path $projectRoot "apps\desktop-dotnet\ShizukuWeatherDesktop.csproj"
$desktopRoot = Split-Path $desktopProject -Parent
$webRoot = Join-Path $projectRoot "apps\web"
$publishDir = Join-Path $desktopRoot "publish"
$installerScript = Join-Path $desktopRoot "installer\ShizukuOpenWeather.iss"
$iconFile = Join-Path $desktopRoot "assets\app-icon.ico"

if ([string]::IsNullOrWhiteSpace($ArtifactDir)) {
    $ArtifactDir = Join-Path $projectRoot "artifacts\desktop"
}

if (-not (Test-Path -LiteralPath $InnoSetupCompiler)) {
    $compilerCommand = Get-Command iscc -ErrorAction Stop
    $InnoSetupCompiler = $compilerCommand.Source
}

New-Item -ItemType Directory -Path $ArtifactDir -Force | Out-Null

Push-Location $projectRoot
try {
    if (-not $SkipWebBuild) {
        Push-Location $webRoot
        try {
            if (Test-Path -LiteralPath (Join-Path $webRoot "package-lock.json")) {
                npm ci
            }
            else {
                npm install
            }

            npm run build
        }
        finally {
            Pop-Location
        }
    }

    if (Test-Path -LiteralPath $publishDir) {
        Remove-Item -LiteralPath $publishDir -Recurse -Force
    }

    & $DotnetCommand publish $desktopProject `
        -c $Configuration `
        -r $Runtime `
        --self-contained true `
        -p:PublishSingleFile=true `
        -p:IncludeNativeLibrariesForSelfExtract=true `
        -o $publishDir

    $portableZip = Join-Path $ArtifactDir ("ShizukuOpenWeather-portable-{0}.zip" -f $Version)
    if (Test-Path -LiteralPath $portableZip) {
        Remove-Item -LiteralPath $portableZip -Force
    }

    Compress-Archive -Path (Join-Path $publishDir "*") -DestinationPath $portableZip

    & $InnoSetupCompiler `
        "/DAppVersion=$Version" `
        "/DSourcePublishDir=$publishDir" `
        "/DSourceIconFile=$iconFile" `
        "/DOutputBaseDir=$ArtifactDir" `
        "/DOutputBaseFilename=ShizukuOpenWeather-Setup-$Version" `
        $installerScript
}
finally {
    Pop-Location
}
