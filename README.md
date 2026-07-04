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

```bash
docker compose up -d --build
docker compose exec dev bash
gradle checkDevEnvironment
```

Start the API and web UI inside the container:

```bash
scripts/start-dev.sh
```

Host ports:

- API: http://127.0.0.1:8080
- Web dev server: http://127.0.0.1:5173
- Web preview server: http://127.0.0.1:4173

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
