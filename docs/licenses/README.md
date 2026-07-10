# Third-party data

The Android offline China administrative-location index uses `data.json` from
[88250/city-geo](https://github.com/88250/city-geo).

- Purpose: match Chinese provinces, prefecture-level cities, county-level cities, districts, and counties to coordinates.
- Coordinate handling: source BD-09 coordinates are converted to WGS84 before Open-Meteo requests.
- License: Mulan Permissive Software License, Version 2.
- Local license copy: `city-geo-MulanPSL2.txt`.
- Upstream data snapshot: repository `master`, downloaded 2026-07-10.

Worldwide city search remains provided by the configured online geocoding source (Open-Meteo by default, with AMap support when configured).
