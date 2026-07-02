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
