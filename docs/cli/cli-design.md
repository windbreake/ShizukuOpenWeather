# CLI Design

Planned binary name: `weather`.

Commands:

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
```

CLI output should use card-like terminal formatting and support JSON output for scripts.
