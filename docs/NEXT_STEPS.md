# Next Steps

Start here after any restart.

## Immediate Next Step

Run development inside the container:

```bash
cd /home/neko/snap/ShizukuOpenWeather
docker-compose up -d --build
docker-compose exec dev bash
gradle checkDevEnvironment
cargo metadata --no-deps
```

If the container is already running, skip the build and enter it:

```bash
docker-compose exec dev bash
```

## First Coding Milestone

Implement Rust location search using Open-Meteo Geocoding.

Files to create or edit:

```text
crates/weather-core/src/error.rs
crates/weather-core/src/models/location.rs
crates/weather-core/src/provider/open_meteo.rs
crates/weather-core/src/location/geocoding.rs
crates/weather-cli/src/args.rs
crates/weather-cli/src/commands/search.rs
```

Expected command:

```bash
weather search 沈阳 --json
weather search 沈阳
```

Expected behavior:

- Query Open-Meteo geocoding API.
- Return candidate locations.
- Print JSON when `--json` is supplied.
- Print a readable card/table otherwise.

Commit when stable:

```bash
git add .
git commit -m "feat: add Open-Meteo location search"
```

## Second Coding Milestone

Implement weather summary query.

Expected command:

```bash
weather summary 沈阳 --json
weather summary --lat 41.8057 --lon 123.4315 --json
```

Commit when stable:

```bash
git commit -m "feat: add weather summary query"
```

## GitHub Push

Remote repository exists:

```text
https://github.com/windbreake/ShizukuOpenWeather
```

Current local remote:

```bash
git remote -v
```

If push still times out or asks for credentials, request a new GitHub PAT from the user.

Token creation URL:

```text
https://github.com/settings/tokens/new
```

Required scopes for classic PAT:

- `repo` for private repositories, or `public_repo` for public only.

Fine-grained token permissions:

- Repository access: `ShizukuOpenWeather` or all repositories.
- Contents: Read and write.
- Metadata: Read-only.
