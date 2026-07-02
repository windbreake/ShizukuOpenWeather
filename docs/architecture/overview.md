# Architecture Overview

ShizukuOpenWeather is split into four layers:

- Rust core for weather providers, normalization, SQLite cache, and CLI reuse.
- Java/Kotlin API for REST orchestration and local preferences.
- Vue 3 desktop web dashboard for visualization.
- SQLite for cache, provider configuration, and local preferences.

No login or account system is planned for the MVP.
