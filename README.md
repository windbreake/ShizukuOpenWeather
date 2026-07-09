<p align="center">
  <img src="apps/desktop-dotnet/assets/app-icon-source.png" alt="ShizukuOpenWeather" width="180" />
</p>

<h1 align="center">ShizukuOpenWeather</h1>

<p align="center">
  PC-first modern weather dashboard with a .NET 10 desktop shell, Vue 3 UI, Java/Kotlin services, and SQLite local cache.
</p>

<p align="center">
  <a href="README.en.md">English</a> · <a href="README.zh-CN.md">简体中文</a>
</p>

## Overview

ShizukuOpenWeather is a desktop-focused weather application project inspired by modern card-based weather experiences such as Overdrop, while being optimized around Windows desktop workflows.

The repository currently centers on:

- a Windows desktop app built with `.NET 10` + `WebView2`
- a desktop weather UI built with `Vue 3` + `TypeScript`
- `Java / Kotlin` service-side components
- local `SQLite` cache and preferences storage
- Windows installer and portable release packaging
- preserved devcontainer-based CI/CD plus dedicated Windows release automation

The repository still contains some legacy Rust workspace files, but the current active desktop delivery path is the .NET + Vue desktop stack.

## Quick Links

- [English README](README.en.md)
- [中文 README](README.zh-CN.md)
- [GitHub Releases](https://github.com/windbreake/ShizukuOpenWeather/releases)

## Current Highlights

- Multi-location desktop weather dashboard
- County-level search in China and overseas city lookup
- Current conditions, hourly forecast, weekly forecast, alerts, AQI, radar, and map
- Custom background, frosted glass effects, card visibility, and local settings
- Windows installer, portable ZIP, and GitHub Release workflow

## Repository Structure

```text
ShizukuOpenWeather/
├── apps/
│   ├── api/               # Java / Kotlin backend services
│   ├── desktop-dotnet/    # .NET 10 desktop shell and installer assets
│   └── web/               # Vue 3 desktop weather UI
├── crates/                # legacy Rust workspace modules not used by the main desktop flow
├── docs/                  # Architecture, API, UI, and data docs
├── scripts/               # Development and packaging scripts
└── .github/workflows/     # CI/CD and release workflows
```

## Development and Packaging

For full development, packaging, and CI/CD details, use one of the language-specific READMEs above.
