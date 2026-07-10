<p align="center">
  <img src="apps/desktop-dotnet/assets/app-icon-source.png" alt="ShizukuOpenWeather" width="160" />
</p>

<h1 align="center">ShizukuOpenWeather</h1>

<p align="center">
  A modern weather app for Windows and Android. The desktop app keeps the large-screen card dashboard, while Android is rebuilt with native Kotlin and Jetpack Compose.
</p>

<p align="center">
  <a href="README.md">Home</a> · <a href="README.zh-CN.md">简体中文</a> ·
  <a href="https://github.com/windbreake/ShizukuOpenWeather/releases">Releases</a>
</p>

## Overview

ShizukuOpenWeather is a local-first weather application for Windows desktop and Android. Its visual direction is inspired by modern card-based weather products such as Overdrop, while keeping this project's own frosted glass surfaces, tile cards, weather-aware backgrounds, and dense dashboard layout.

The project has been moved out of the early container-only development copy. The repository still keeps the Dev Container and CI/CD chain, and now also includes Windows installer packaging, portable desktop builds, Android APK builds, and GitHub Release automation.

## Current Main Path

Rust is not the current product path. A small legacy Rust workspace is still kept in the repository for compatibility and backup purposes, but active product development is now centered on:

- Android: `Kotlin` + `Jetpack Compose`
- Windows desktop: `.NET 10` + `WebView2` + `Vue 3` + `TypeScript`
- Service and networking layer: `Java / Kotlin`
- Local cache and preferences: `SQLite`

## Features

- Multi-location weather dashboard with sidebar tiles, detailed cards, and desktop-friendly layout
- Native Android weather screen, locations screen, settings screen, and auto-hiding liquid-glass bottom navigation
- Offline China administrative-location search for provinces, cities, districts, counties, and county-level cities
- Overseas city search through Open-Meteo geocoding by default
- Current-location weather through Android system location; the device decides whether to use GPS, BeiDou, Galileo, GLONASS, network positioning, or a fused result
- Open-Meteo as the default weather provider, so the repository does not need bundled private API keys
- Optional QWeather, QWeather icons, and AMap Web API configuration
- API credentials are not committed to the repository; Android users enter them locally, and they are encrypted with Android Keystore
- Current weather, hourly trends, 7-day forecast, AQI, weather alerts, map, and radar views
- Custom background, frosted glass intensity, card visibility, and cache refresh preferences
- Windows desktop EXE, Inno Setup installer, portable ZIP, and Android APK release paths

## Data and Privacy

The default mode does not require private weather API keys. China search uses an offline administrative-location coordinate index bundled in the repository, then queries weather from Open-Meteo. Overseas search uses Open-Meteo's free geocoding service by default.

Optional API credentials should only be entered in the local app settings. They should not be written into README files, source code, sample configs, or GitHub Actions logs. Tokens that were ever exposed publicly should be treated as risky and rotated in the provider console.

## Tech Stack

- `Kotlin` + `Jetpack Compose`
- `.NET 10` WinForms + `WebView2`
- `Vue 3` + `TypeScript` + `Vite`
- `Java / Kotlin`
- `SQLite`
- `Open-Meteo`
- Optional: `QWeather`, `AMap Web API`, `OpenStreetMap`

## Repository Structure

```text
ShizukuOpenWeather/
├── .devcontainer/                 # Dev Container development environment
├── .github/workflows/             # Android, desktop, and container CI/CD
├── apps/
│   ├── android/                   # Kotlin + Jetpack Compose Android app
│   ├── api/                       # Java / Kotlin service and API layer
│   ├── desktop-dotnet/            # .NET 10 desktop shell, icons, installer assets
│   └── web/                       # Vue 3 + TypeScript desktop weather UI
├── data/                          # Offline and local development data
├── docs/                          # Architecture, API, UI, data source, and license docs
├── scripts/                       # Development, validation, and packaging scripts
├── Dockerfile                     # Container development image
├── docker-compose.yml             # Container development environment
├── build.gradle.kts               # Top-level Gradle build
└── README.md
```

## Local Development

Recommended environment:

- JDK 21
- Node.js 22+
- Gradle 8.9
- Android SDK 35 or Android Studio
- .NET 10 SDK
- SQLite3 CLI
- Inno Setup 6 for the Windows installer

Environment check:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-dev-env.ps1
```

Desktop development:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/dev-api.ps1
powershell -ExecutionPolicy Bypass -File scripts/dev-web.ps1
powershell -ExecutionPolicy Bypass -File scripts/start-dev.ps1
```

Android build:

```powershell
gradle :apps:android:testDebugUnitTest :apps:android:assembleDebug
```

Android APK output:

```text
apps/android/build/outputs/apk/debug/android-debug.apk
```

Windows desktop packaging:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-desktop-installer.ps1 -Version 0.1.0
```

Desktop packaging outputs:

- `ShizukuWeatherDesktop.exe`
- `ShizukuOpenWeather-Setup-<version>.exe`
- `ShizukuOpenWeather-portable-<version>.zip`

## CI/CD

- `Devcontainer CI/CD`: keeps the original container development path and validates the multi-language project
- `Android CI and Release`: runs Android unit tests, builds the APK, uploads CI artifacts, and appends APKs to tagged releases
- `Desktop Release`: packages the Windows desktop EXE, installer, and portable ZIP

Release assets are available from:

- [GitHub Releases](https://github.com/windbreake/ShizukuOpenWeather/releases)

## License and Third-party Data

Third-party data and resource license notes live in [docs/licenses](docs/licenses). The offline China administrative-location index uses the `city-geo` data set; details are documented in [docs/licenses/README.md](docs/licenses/README.md).
