#!/usr/bin/env bash
set -euo pipefail

mkdir -p /tmp/shizuku-open-weather

if ! pgrep -f "app.weather.WeatherApplicationKt" >/dev/null; then
  nohup gradle :apps:api:bootRun >/tmp/shizuku-open-weather/api.log 2>&1 &
fi

if ! pgrep -f "vite --host" >/dev/null; then
  nohup npm --prefix apps/web run dev -- --host 0.0.0.0 >/tmp/shizuku-open-weather/web.log 2>&1 &
fi

echo "API: http://127.0.0.1:8080/api/health"
echo "Web: http://127.0.0.1:5173"
echo "Logs: /tmp/shizuku-open-weather"
