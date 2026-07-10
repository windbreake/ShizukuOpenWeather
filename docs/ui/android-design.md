# Android Mobile Design

## Direction

The Android interface keeps the ShizukuOpenWeather identity: weather-aware hero colors, translucent information cards, QWeather icon support, configurable backgrounds, and dense but readable data. The supplied Overdrop references are used only for mobile information hierarchy and interaction rhythm.

## Primary Frames

- Compact phone: 360 x 800 dp
- Default phone: 412 x 915 dp
- Large phone / foldable narrow pane: 480 x 960 dp
- Tablet: content remains centered with a maximum width of 720 dp

## Screen Structure

### Weather

1. Weather hero: 244 dp high, 8 dp radius.
2. Current details: two-column metric grid.
3. Official alerts or calculated risk hints.
4. Twelve-hour horizontal forecast.
5. Split environment row: narrow AQI card and wide radar card.
6. Seven-day vertical forecast inside the large lower container.
7. Source and cache status.
8. Floating glass navigation overlays the bottom edge without consuming layout height.

### Locations

1. Page title and short scope hint.
2. Search field supporting districts, counties, and overseas cities.
3. Search results with explicit add action.
4. Saved locations with current selection and delete action.

### Settings

1. Weather provider and encrypted credentials.
2. Appearance and custom background.
3. Card visibility controls.
4. SQLite cache refresh interval.
5. Single save-and-refresh action.

## Visual Tokens

- Grid: 4 dp base unit.
- Page inset: 14-16 dp.
- Card gap: 12 dp.
- Card radius: 8 dp.
- Hero radius: 8 dp.
- Main text minimum contrast: WCAG AA.
- Primary teal: #276B67.
- Secondary violet: #74558B.
- Tertiary coral: #B85B4A.
- Page background: #F4F7F8.
- Card surface: #F9FBFC with user-controlled alpha.

## Motion

- Press feedback uses native ripple.
- The floating navigation hides while a list is moving.
- It fades and slides back 650 ms after scrolling stops.
- The glass surface uses translucent color, a fine highlight border, and soft elevation.
- No particle mesh, moire pattern, or decorative looping layer.
- Reduced motion can be disabled from settings.

## Figma Handoff

Each Compose screen maps to one Figma frame and each card maps to a component variant. Use Auto Layout vertically, 12 dp spacing, 16 dp horizontal padding, and an 8 dp corner radius. Provider chips, switches, sliders, icon buttons, and navigation items should remain native-control equivalents.
