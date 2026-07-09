# REST API Draft

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

The frontend should primarily consume `/api/weather/summary` for dashboard rendering.

## Weather Summary

```http
GET /api/weather/summary?lat=41.8057&lon=123.4315
```

Current implementation returns a deterministic development response while the Rust provider integration is being built.

```json
{
  "locationName": "Shenyang",
  "latitude": 41.8057,
  "longitude": 123.4315,
  "temperature": 24.6,
  "description": "Partly cloudy with steady visibility",
  "source": "open-meteo-mock",
  "cached": false
}
```
