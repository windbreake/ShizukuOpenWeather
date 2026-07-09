# Directory Structure

```text
ShizukuOpenWeather/
├── apps/
│   ├── api/                              # Java/Kotlin Spring Boot backend API
│   │   ├── build.gradle.kts              # Backend Gradle module
│   │   └── src/
│   │       └── main/
│   │           ├── java/                 # Java DTOs and backend shared models
│   │           ├── kotlin/               # Kotlin app, controllers, services
│   │           └── resources/            # application.yml and SQLite schema
│   └── web/                              # Vue 3 + TypeScript desktop web UI
│       ├── public/                       # Static public assets
│       ├── src/
│       │   ├── api/                      # REST API clients
│       │   ├── components/               # Dashboard components
│       │   ├── composables/              # Reusable Vue composition functions
│       │   ├── stores/                   # Pinia state stores
│       │   ├── styles/                   # Reset, tokens, themes, app styles
│       │   └── types/                    # TypeScript API/UI contracts
│       ├── package.json
│       └── vite.config.ts
├── crates/
│   ├── weather-core/                     # Rust core: provider, normalization, SQLite cache
│   │   ├── Cargo.toml
│   │   └── src/
│   └── weather-cli/                      # Rust CLI/TUI: search, query, open, launch
│       ├── Cargo.toml
│       └── src/
├── data/                                 # Local SQLite data directory
├── docs/
│   ├── api/                              # REST API contracts
│   ├── architecture/                     # Architecture and platform notes
│   ├── cli/                              # CLI and TUI design
│   ├── data/                             # SQLite schema notes
│   └── ui/                               # Desktop UI design
├── scripts/                              # Development helper scripts
├── .devcontainer/                        # Dev Containers configuration
├── Cargo.toml                            # Rust workspace root
├── Dockerfile                            # Development image
├── docker-compose.yml                    # Containerized dev environment
├── build.gradle.kts                      # Root Gradle orchestration tasks
├── settings.gradle.kts                   # Gradle project settings
├── gradle.properties
└── README.md
```
