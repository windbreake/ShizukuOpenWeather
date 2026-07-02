# ShizukuOpenWeather Project Plan

This document is the persistent project memory. If an agent session restarts, continue from here before making changes.

## Product Goal

Build a desktop-first weather visualization tool inspired by Overdrop, with:

- Rust as the core weather engine.
- Java + Kotlin as the backend API layer.
- Vue 3 + TypeScript as the desktop web UI.
- SQLite for local cache, providers, and preferences.
- CLI and future TUI support.
- Open-Meteo as the default free weather provider.
- Custom weather API provider support as a planned extension.

## Explicit Non-Goals

Do not add these unless the user explicitly changes scope:

- Login or registration.
- User accounts.
- OAuth.
- Roles and permissions.
- Sessions or JWT.
- Cloud sync.
- Admin dashboard.
- Payment or subscription.
- Ads, analytics, or tracking SDKs.
- Mobile-first UI in the first phase.

## First-Phase Scope

The first working version should include:

- Desktop web dashboard.
- Location search.
- Weather summary query.
- Current weather card.
- Hourly forecast chart.
- Daily forecast list.
- Air quality card.
- Local SQLite cache.
- Theme color setting.
- Background image setting.
- Background blur setting.
- CLI commands for search and summary.
- A command to open or launch the web dashboard.

## Default Provider

Use Open-Meteo first:

- Forecast API: `https://api.open-meteo.com/v1/forecast`
- Geocoding API: `https://geocoding-api.open-meteo.com/v1/search`
- Air Quality API: `https://air-quality-api.open-meteo.com/v1/air-quality`

Reasons:

- Free.
- No API key required.
- Good enough for MVP.
- Provides forecast, geocoding, and air quality.

## Architecture

```text
Vue 3 Desktop Web
  -> Java/Kotlin Spring Boot API
    -> Rust weather CLI/core JSON command boundary
      -> SQLite cache
      -> Open-Meteo / future custom providers

Rust CLI
  -> Rust weather-core
    -> SQLite cache
    -> Open-Meteo / future custom providers
```

Use a process boundary between backend and Rust core first:

- Java/Kotlin executes the Rust `weather` binary with JSON output.
- This avoids JNI/JNA/UniFFI complexity early.
- Later, if needed, replace with JNI/JNA/UniFFI without changing REST contracts.

## Module Responsibilities

### `crates/weather-core`

Owns:

- Weather provider traits.
- Open-Meteo client.
- Weather data normalization.
- Location search models.
- SQLite cache.
- User preferences models.
- Custom provider extension points.

Must not own:

- Terminal UI.
- Spring Boot API.
- Vue rendering.

### `crates/weather-cli`

Owns:

- CLI argument parsing.
- Search command.
- Summary/current/hourly/daily/aqi commands.
- Card-like terminal output.
- JSON output for scripting and backend integration.
- Future TUI.
- Opening/launching web dashboard.

### `apps/api`

Owns:

- REST API.
- API request validation.
- Calling Rust command boundary.
- Preferences endpoints.
- Provider configuration endpoints.
- Backend config.

Must not own:

- Account/login system.
- Weather provider-specific business logic beyond orchestration.

### `apps/web`

Owns:

- Desktop dashboard UI.
- Search panel.
- Settings panel.
- Theme/background customization.
- Weather visualization.
- API clients and state stores.

Must not own:

- Direct third-party weather API calls.
- Server-side caching.

## REST API Draft

Keep API small and stable:

```http
GET /api/health
GET /api/locations/search?q=沈阳
GET /api/weather/summary?lat=41.8057&lon=123.4315
GET /api/preferences
PUT /api/preferences
GET /api/providers
POST /api/providers
PUT /api/providers/{id}
DELETE /api/providers/{id}
```

The web dashboard should mainly use:

```http
GET /api/locations/search?q=...
GET /api/weather/summary?lat=...&lon=...
GET /api/preferences
PUT /api/preferences
```

## CLI Design

Binary name: `weather`.

Planned commands:

```bash
weather search 沈阳
weather current 沈阳
weather summary 沈阳
weather hourly 沈阳 --hours 24
weather daily 沈阳 --days 7
weather aqi 沈阳
weather tui
weather serve
weather open 沈阳
weather launch 沈阳
weather config show
```

Support JSON output:

```bash
weather search 沈阳 --json
weather summary 沈阳 --json
```

Location search behavior:

- If a query has one strong match, use it.
- If multiple matches exist in interactive mode, prompt for selection.
- In non-interactive mode, require `--first` or `--lat/--lon`.

Future TUI shortcuts:

```text
/        search
Enter    confirm
Tab      switch panel
o        open web dashboard
r        refresh
q        quit
```

## Desktop UI Direction

Desktop only in phase one.

Design baseline:

- Min width: 1024px.
- Main design width: 1280px.
- Max content width: 1320px.

Visual style:

- Overdrop-inspired.
- Soft blue mist palette.
- Large rounded cards.
- Low-contrast shadows.
- Weather hero illustration feeling.
- Clean, comfortable, not an admin panel.
- Theme color configurable.
- Background image configurable.
- Background blur configurable.

Desktop layout:

```text
Top toolbar: search / location / refresh / theme / settings

Hero weather card              Current details card
Hero weather card              AQI card

Hourly forecast chart full width

Weekly forecast card           Radar/map card
```

Core components:

- `AppShell`
- `TopToolbar`
- `WeatherHeroCard`
- `CurrentDetailsCard`
- `HourlyForecastCard`
- `AirQualityCard`
- `WeeklyForecastCard`
- `RadarCard`
- `LocationSearchPanel`
- `SettingsPanel`
- `MetricTile`
- `WeatherIcon`

## SQLite Tables

Initial tables only:

- `weather_cache`
- `location_cache`
- `api_providers`
- `preferences`

Do not add:

- `users`
- `roles`
- `sessions`
- `oauth_accounts`

## Cross-Platform Rules

Keep future Windows, macOS, Linux, mobile web, and desktop app support practical.

Rules:

- Do not hard-code Linux-only paths in business logic.
- Do not hard-code `/tmp`, `/home`, or shell-specific behavior in app code.
- Use Rust `directories` or `dirs` for CLI config/cache paths.
- Use Java/Kotlin `Path` APIs, not string path concatenation.
- Keep database path configurable.
- Keep backend host/port configurable.
- Keep web API base URL configurable.
- Use Rust `open` crate for browser opening later, not OS-specific commands.
- Keep REST contracts stable for future desktop/mobile clients.
- Keep Vue app browser-only, no Electron-specific APIs in MVP.
- Provider layer must use traits/interfaces.
- Frontend theme tokens should be reusable by future mobile/desktop shells.

## Proxy And Container Isolation Policy

The user allows connecting to an existing proxy container if needed, but system safety and isolation are more important than convenience.

Rules:

- Do not use `network_mode: host` unless explicitly approved later.
- Do not mount Docker socket into the development container.
- Do not mount broad host paths such as `/`, `/home`, or `/var/run`.
- Keep ShizukuOpenWeather in its own Compose project.
- Prefer explicit environment variables for proxy access: `HTTP_PROXY`, `HTTPS_PROXY`, `NO_PROXY`.
- Prefer an optional `docker-compose.override.yml` for local proxy wiring instead of changing the default Compose file.
- If a proxy container must be joined, use a named external network only after confirming its name with the user.
- Keep service-to-service access narrow and documented.
- Never store proxy credentials in committed files.

Recommended future override pattern:

```yaml
services:
  dev:
    environment:
      HTTP_PROXY: ${HTTP_PROXY}
      HTTPS_PROXY: ${HTTPS_PROXY}
      NO_PROXY: ${NO_PROXY:-127.0.0.1,localhost}
```

This keeps the default project isolated while allowing local developers to opt into proxy routing.

## Development Order

Follow this sequence:

1. Complete project skeleton and persistent docs.
2. Verify container build and toolchain.
3. Implement Rust models and Open-Meteo location search.
4. Implement Rust weather summary query.
5. Add SQLite cache to Rust core.
6. Add CLI `search` and `summary` with JSON output.
7. Add CLI card-style human output.
8. Add Spring Boot API endpoints for health, search, summary.
9. Connect API to Rust CLI JSON boundary.
10. Build desktop web dashboard static layout.
11. Connect web dashboard to API.
12. Add preferences for theme/background/blur.
13. Add CLI `open`, `serve`, and `launch`.
14. Add TUI after basic CLI and web are stable.

## Backup Policy

Commit after each stable milestone.

Suggested commit rhythm:

- Skeleton/docs commit.
- Rust search commit.
- Rust summary/cache commit.
- CLI commands commit.
- API endpoints commit.
- Web static dashboard commit.
- Web API integration commit.
- Preferences commit.
- TUI commit.

If GitHub push is unavailable, keep local commits. Push once authentication/network is fixed.

## Current GitHub Status

Repository intended name: `ShizukuOpenWeather`.

GitHub MCP initially failed with authentication. Existing token later authenticated as `windbreake`, and the GitHub repository was created via REST API:

`https://github.com/windbreake/ShizukuOpenWeather`

Local push via Git timed out. Continue with local commits until push is fixed or a new token is provided.
