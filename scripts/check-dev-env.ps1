[CmdletBinding()]
param()

. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Get-ProjectRoot
$missing = New-Object System.Collections.Generic.List[string]

$requirements = @(
    @{ Name = "java"; Hint = "Install JDK 21." },
    @{ Name = "rustc"; Hint = "Install Rust with rustup." },
    @{ Name = "cargo"; Hint = "Install Rust with rustup." },
    @{ Name = "node"; Hint = "Install Node.js 22 or newer." },
    @{ Name = "npm"; Hint = "Install Node.js 22 or newer." },
    @{ Name = "kotlinc"; Hint = "Install the Kotlin compiler." },
    @{ Name = "sqlite3"; Hint = "Install the SQLite3 CLI." }
)

foreach ($requirement in $requirements) {
    if (-not (Get-Command $requirement.Name -ErrorAction SilentlyContinue)) {
        $missing.Add("$($requirement.Name): $($requirement.Hint)")
    }
}

$wrapperPath = Join-Path $projectRoot "gradlew.bat"
if (-not (Test-Path $wrapperPath) -and -not (Get-Command "gradle" -ErrorAction SilentlyContinue)) {
    $missing.Add("gradle: Install Gradle 8+ or add gradlew.bat to the project root.")
}

if ($missing.Count -gt 0) {
    throw "Missing required commands:`n- $($missing -join "`n- ")"
}

$gradle = Get-GradleCommand

Push-Location $projectRoot
try {
    & $gradle checkDevEnvironment
}
finally {
    Pop-Location
}
