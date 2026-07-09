# ShizukuOpenWeather

Desktop-first weather app with an Overdrop-inspired dashboard, a .NET 10 desktop shell, Vue 3 UI, Rust weather core, Java/Kotlin service layer, and SQLite local cache.

## Current stack

- Desktop shell: `.NET 10` WinForms + WebView2
- UI: `Vue 3` + `TypeScript`
- Weather data: `QWeather`
- Location search and reverse geocoding: `AMap Web API`
- Cache and local settings: `SQLite`
- Core and CLI: `Rust`
- Service and integration layer: `Java` / `Kotlin`
- Map tiles: `OpenStreetMap`

## What ships today

- PC-first desktop weather dashboard
- Multi-location sidebar tiles with sync state
- County-level search in China and overseas location lookup
- Current conditions, hourly trend, weekly forecast, AQI, alerts, radar, and map
- Local settings for layout, glass effect, background image, weather API, and card visibility
- Portable desktop build and Windows installer build

Mobile adaptation is intentionally left as follow-up work.

## Project structure

```text
ShizukuOpenWeather/
├── .github/workflows/               # CI/CD workflows, including devcontainer validation and Windows release packaging
├── apps/
│   ├── api/                         # Java/Kotlin Spring Boot backend API
│   ├── desktop-dotnet/              # .NET 10 desktop shell, local host bridge, installer assets
│   └── web/                         # Vue 3 + TypeScript desktop weather dashboard
├── crates/
│   ├── weather-core/                # Rust core models, providers, cache contracts
│   └── weather-cli/                 # Rust CLI / launcher surface
├── data/                            # Local development SQLite data
├── docs/                            # Architecture, API, data, and UI docs
├── scripts/                         # Development and packaging scripts
├── Dockerfile
├── docker-compose.yml
├── Cargo.toml
├── build.gradle.kts
└── README.md
```

## Local development

Recommended Windows toolchain:

- JDK 21
- Node.js 22+
- Rust toolchain
- SQLite3 CLI
- .NET 10 SDK
- Inno Setup 6 for installer packaging

Check the local environment:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-dev-env.ps1
```

Run API and web development servers:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/dev-api.ps1
powershell -ExecutionPolicy Bypass -File scripts/dev-web.ps1
```

Start the existing combined development flow:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-dev.ps1
```

Build the Rust, web, and backend workspace:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-all.ps1
```

Build the Windows desktop publish output and installer:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-desktop-installer.ps1 -Version 0.1.0
```

## Desktop packaging

The desktop packaging flow produces:

- `ShizukuWeatherDesktop.exe` self-contained desktop app
- `ShizukuOpenWeather-Setup-<version>.exe` Windows installer
- optional portable zip artifact for release upload

The installer uses the same app icon as the desktop executable, installs under the current user profile by default, and creates Start Menu and optional Desktop shortcuts.

## CI/CD

The original container-based validation chain stays in place:

- `.devcontainer/devcontainer.json`
- `.github/workflows/devcontainer-ci-cd.yml`

That path remains the source of truth for devcontainer validation and the shared Linux-oriented checks.

For Windows desktop packaging and release assets, the repository also includes a dedicated Windows workflow:

- `.github/workflows/desktop-release.yml`

This keeps the existing CI/CD path intact while giving the desktop app its own release packaging lane.
