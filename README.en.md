<p align="center">
  <img src="apps/desktop-dotnet/assets/app-icon-source.png" alt="ShizukuOpenWeather" width="180" />
</p>

<h1 align="center">ShizukuOpenWeather</h1>

<p align="center">
  A PC-first modern weather application currently delivered through a .NET 10 desktop shell, Vue 3 UI, Java/Kotlin services, and SQLite local cache.
</p>

<p align="center">
  <strong>English</strong> · <a href="README.zh-CN.md">简体中文</a>
</p>

## Overview

ShizukuOpenWeather is a modern weather application project centered on the Windows desktop experience. Its visual direction is inspired by products such as Overdrop, while its interaction and layout decisions are optimized for desktop use.

The project has already been moved out of its original container-only development copy into a local repository workflow, while preserving the existing container validation and CI/CD chain. It now also includes Windows installer packaging, portable builds, and GitHub Release publishing.

## Current State

The active delivery path in this repository currently centers on:

- `.NET 10` desktop shell
- `Vue 3 + TypeScript` desktop UI
- `Java / Kotlin` networking and service integration
- `SQLite` local cache and preferences storage

The repository still contains some Rust workspace files and folders such as `crates/`, `Cargo.toml`, and `Cargo.lock`, but they are no longer the main path described by the current desktop README and are better treated as legacy or reserved modules.

## Project Goals

- Desktop-first weather dashboard and multi-location management
- Modern UI, card-based information design, and customizable appearance
- Local-first usage without accounts, cloud sync, or heavy deployment requirements
- Multi-language extensibility for future desktop and mobile packaging targets

The current focus is PC. Mobile adaptation is intentionally left for future work.

## Current Features

- Multi-location weather view with sidebar tile cards
- County-level lookup in China and city-level overseas search
- Current conditions, hourly trend, and 7-day forecast
- AQI, weather alerts, map, and radar presentation
- Custom background, frosted glass effect, card visibility, and layout preferences
- Local SQLite cache and local settings storage
- Windows desktop app, portable package, and installer output

## Tech Stack

### Current Main Path

- `Vue 3` + `TypeScript`
- `Vite`
- `.NET 10` WinForms + `WebView2`
- `Java / Kotlin`
- `SQLite`

### Data Sources

- Weather data: `QWeather`
- Geocoding and location search: `AMap Web API`
- Map tiles: `OpenStreetMap`

### Legacy / Reserved Modules

- `Rust` workspace files are still present in the repository, but they are not the primary delivery path described by this README

## Repository Structure

```text
ShizukuOpenWeather/
├── .devcontainer/                   # Dev Container configuration
├── .github/workflows/               # GitHub Actions workflows
├── apps/
│   ├── api/                         # Java / Kotlin backend services
│   ├── desktop-dotnet/              # .NET 10 desktop shell, local host bridge, installer assets
│   └── web/                         # Vue 3 + TypeScript desktop weather UI
├── crates/                          # Legacy Rust workspace modules
├── data/                            # Local development data
├── docs/                            # Architecture, API, data, and UI docs
├── scripts/                         # Development, build, and packaging scripts
├── Dockerfile                       # Container development image
├── docker-compose.yml               # Container development environment
├── build.gradle.kts                 # Top-level build orchestration
└── README.md
```

## Local Development

### Recommended Environment

- JDK 21
- Node.js 22+
- SQLite3 CLI
- .NET 10 SDK
- Inno Setup 6 for Windows installer packaging

### Validate the environment

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-dev-env.ps1
```

### Run API and Web development services separately

```powershell
powershell -ExecutionPolicy Bypass -File scripts/dev-api.ps1
powershell -ExecutionPolicy Bypass -File scripts/dev-web.ps1
```

### Start the current combined development flow

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-dev.ps1
```

## Windows Desktop Packaging

The repository already supports complete desktop output, including application icon wiring, installer icon wiring, and release asset packaging.

### Build the desktop app and installer

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-desktop-installer.ps1 -Version 0.1.0
```

### Output artifacts

The build script generates:

- `ShizukuWeatherDesktop.exe` for desktop publish output
- `ShizukuOpenWeather-Setup-<version>.exe` for the Windows installer
- `ShizukuOpenWeather-portable-<version>.zip` for the portable package

The installer defaults to a per-user installation path and can create both Start Menu and Desktop shortcuts.

## CI/CD

### Existing container path

The following path remains in place as the shared validation and devcontainer foundation:

- `.devcontainer/devcontainer.json`
- `.github/workflows/devcontainer-ci-cd.yml`

### Windows desktop release path

The repository also includes a dedicated Windows desktop packaging workflow:

- `.github/workflows/desktop-release.yml`

This keeps the original container CI/CD path intact while giving the desktop application its own release lane.

## Release

Published assets are available from GitHub Releases:

- [Releases](https://github.com/windbreake/ShizukuOpenWeather/releases)

## Note

The content shown on the GitHub repository homepage comes directly from the root `README.md`. Once that file is pushed to the default `main` branch, the homepage updates automatically.
