# ShizukuOpenWeather

Desktop-first weather visualization app with a Rust weather core, Java/Kotlin API, Vue 3 web UI, SQLite cache, and CLI.

## Scope

- Desktop web dashboard first; mobile support is intentionally deferred.
- No login, registration, roles, sessions, or cloud account system.
- Default weather provider: Open-Meteo.
- Local SQLite cache and local preferences.
- CLI support for search, weather query, TUI, and opening the web dashboard.

## Project Structure

```text
ShizukuOpenWeather/
├── apps/
│   ├── api/                         # Java/Kotlin Spring Boot backend API
│   │   ├── src/main/kotlin/          # Kotlin application, controllers, services
│   │   ├── src/main/java/            # Java DTOs and shared backend models
│   │   ├── src/main/resources/       # application config and SQLite schema
│   │   └── build.gradle.kts
│   └── web/                         # Vue 3 + TypeScript desktop web UI
│       ├── public/
│       ├── src/
│       │   ├── api/                 # API clients
│       │   ├── components/          # Weather dashboard components
│       │   ├── composables/         # UI/data hooks
│       │   ├── stores/              # Pinia stores
│       │   ├── styles/              # Tokens, themes, app CSS
│       │   └── types/               # TypeScript contracts
│       ├── package.json
│       └── vite.config.ts
├── crates/
│   ├── weather-core/                # Rust core: providers, normalization, SQLite cache
│   └── weather-cli/                 # Rust CLI/TUI: search/query/open/launch
├── data/                            # Local development SQLite data
├── docs/
│   ├── api/                         # REST contracts
│   ├── architecture/                # System and platform architecture
│   ├── cli/                         # CLI/TUI design
│   ├── data/                        # SQLite schema and cache model
│   └── ui/                          # Desktop Overdrop-inspired UI spec
├── scripts/                         # Development helper scripts
├── Dockerfile                       # Development container image
├── docker-compose.yml               # Containerized dev environment
├── Cargo.toml                       # Rust workspace
├── settings.gradle.kts              # Gradle root settings
├── build.gradle.kts                 # Gradle orchestration tasks
└── README.md
```

## Development

### Local Development on Windows

Recommended host toolchain:

- JDK 21
- Gradle 8+
- Node.js 22+
- Rust toolchain (`rustc`, `cargo`, `clippy`, `rustfmt`)
- Kotlin compiler (`kotlinc`)
- SQLite3 CLI

These scripts use gradlew.bat when it exists; otherwise they fall back to the system gradle command.

Validate the local toolchain:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-dev-env.ps1
```

Run the backend and frontend in separate terminals:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/dev-api.ps1
powershell -ExecutionPolicy Bypass -File scripts/dev-web.ps1
```

Or start both in the background with log files under `%TEMP%\\shizuku-open-weather`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-dev.ps1
```

Build the full workspace locally:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-all.ps1
```

Local host ports:

- API: http://127.0.0.1:8080
- Web dev server: http://127.0.0.1:5173
- Web preview server: http://127.0.0.1:4173

### Container Development and CI/CD

The existing container path is still the source of truth for CI/CD and devcontainer validation.

```bash
docker compose up -d --build
docker compose exec dev bash
gradle checkDevEnvironment
scripts/start-dev.sh
```

GitHub Actions continues to use `.devcontainer/devcontainer.json` and `.github/workflows/devcontainer-ci-cd.yml` to validate and publish the devcontainer image. If this folder was copied out of a container without `.git` metadata, reconnect it to the original repository before relying on push-triggered CI.

Legacy Compose users can replace `docker compose` with `docker-compose`.

## Planned Commands

```bash
weather search 沈阳
weather summary 沈阳
weather current 沈阳
weather tui
weather serve
weather open 沈阳
weather launch 沈阳
```

