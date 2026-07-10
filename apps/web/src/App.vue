<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch, type Component } from 'vue'
import {
  ArrowDownUp,
  BellRing,
  ChevronDown,
  ChevronRight,
  ChevronUp,
  Database,
  Droplets,
  EllipsisVertical,
  Gauge,
  Github,
  LayoutGrid,
  MapPinned,
  Palette,
  Plus,
  Radar,
  RefreshCw,
  SatelliteDish,
  Search,
  Settings2,
  ShieldAlert,
  SlidersHorizontal,
  Sparkles,
  Star,
  Thermometer,
  Trash2,
  Umbrella,
  Wind,
} from 'lucide-vue-next'
import {
  getWeatherSummary,
  searchLocations,
  type LocationSearchResult,
  type WeatherApiConfig,
} from './api/client'
import type {
  DailyForecastPoint,
  HourlyForecastPoint,
  WeatherAlert,
  WeatherGlyph,
  WeatherSummary,
} from './types/weather'

type HourlyMetricKey = 'temperature' | 'precipitation' | 'wind'
type RadarLayerKey = 'rain' | 'cloud' | 'wind'
type CardKey = 'alerts' | 'details' | 'hourly' | 'aqi' | 'radar' | 'weekly'
type SettingsPane = 'general' | 'data' | 'alerts' | 'appearance'
type TimeFormatKey = '24h' | '12h'
type DateFormatKey = 'dd/mm/yyyy' | 'yyyy-mm-dd' | 'mm/dd/yyyy'
type SidebarSyncState = 'idle' | 'loading' | 'ready' | 'error'

type UiSettings = {
  frostEnabled: boolean
  frostOpacity: number
  backgroundImage: string
  showHeroScene: boolean
  denseMode: boolean
  adaptiveText: boolean
  playWeatherAnimation: boolean
  enableWeatherGifs: boolean
  showAlerts: boolean
  showRainAlerts: boolean
  showWindAlerts: boolean
  showAirAlerts: boolean
  timeFormat: TimeFormatKey
  dateFormat: DateFormatKey
  refreshIntervalHours: number
  useCustomApi: boolean
  apiProviderName: string
  geocodingBaseUrl: string
  weatherBaseUrl: string
  airQualityBaseUrl: string
  apiKey: string
  apiKeyParam: string
  qweatherApiKey: string
  qweatherCredentialId: string
  weatherGifUrls: Record<WeatherGlyph, string>
  visibleCards: Record<CardKey, boolean>
  cardOrder: CardKey[]
}

const settingsStorageKey = 'shizuku-open-weather-settings'
const locationsStorageKey = 'shizuku-open-weather-locations'

const metricOptions = [
  { key: 'temperature', label: '温度', icon: Thermometer },
  { key: 'precipitation', label: '降雨', icon: Umbrella },
  { key: 'wind', label: '风速', icon: Wind },
] as const

const radarLayers = [
  { key: 'rain', label: '降雨回波', description: '查看降雨带的移动方向' },
  { key: 'cloud', label: '云层覆盖', description: '观察云团厚度和扩散范围' },
  { key: 'wind', label: '风场趋势', description: '快速判断地面风速变化' },
] as const

const settingsNavItems: Array<{ key: SettingsPane; label: string; description: string; icon: Component }> = [
  { key: 'general', label: 'General', description: '布局与常用开关', icon: SlidersHorizontal },
  { key: 'data', label: '气象数据来源', description: '高德检索与和风天气', icon: SatelliteDish },
  { key: 'alerts', label: '天气通知', description: '预警与提醒规则', icon: BellRing },
  { key: 'appearance', label: '外观与动画', description: '背景、动效与字体', icon: Palette },
]

const qweatherIconCdn = 'https://cdn.jsdelivr.net/npm/qweather-icons/icons'

const weatherLabelMap: Record<WeatherGlyph, string> = {
  rain: '降雨',
  drizzle: '毛雨',
  cloudy: '多云',
  mist: '雾霾',
  mixed: '晴朗',
}

const glyphFallbackIconCode: Record<WeatherGlyph, string> = {
  rain: '306',
  drizzle: '305',
  cloudy: '104',
  mist: '501',
  mixed: '100',
}

function normalizeIconCode(code: string | undefined | null, glyph: WeatherGlyph): string {
  const trimmed = String(code ?? '').trim()
  if (/^\d{3,4}$/.test(trimmed)) return trimmed
  return glyphFallbackIconCode[glyph] ?? glyphFallbackIconCode.mixed
}

function getWeatherIconUrl(iconCode: string | undefined | null, glyph: WeatherGlyph = 'mixed') {
  return `${qweatherIconCdn}/${normalizeIconCode(iconCode, glyph)}.svg`
}

function getSummaryIconUrl(summary: WeatherSummary | null | undefined) {
  return getWeatherIconUrl(
    summary?.currentIconCode ?? summary?.hourly[0]?.iconCode ?? summary?.daily[0]?.iconCode,
    summary?.backgroundKey ?? 'mixed',
  )
}

const defaultLocations: LocationSearchResult[] = [
  { key: 'shenyang', label: '沈阳', subtitle: '辽宁省', lat: 41.8057, lon: 123.4315 },
  { key: 'shanghai', label: '上海', subtitle: '黄浦江沿岸', lat: 31.2304, lon: 121.4737 },
  { key: 'tokyo', label: '东京', subtitle: '关东地区', lat: 35.6762, lon: 139.6503 },
  { key: 'singapore', label: '新加坡', subtitle: '滨海湾', lat: 1.3521, lon: 103.8198 },
]

const cardMeta: Array<{ key: CardKey; label: string }> = [
  { key: 'alerts', label: '天气预警' },
  { key: 'details', label: '详情' },
  { key: 'hourly', label: '逐时天气' },
  { key: 'aqi', label: '空气质量' },
  { key: 'radar', label: '地图雷达' },
  { key: 'weekly', label: '本周预报' },
]

const weather = ref<WeatherSummary | null>(null)
const sidebarWeather = ref<Record<string, WeatherSummary>>({})
const sidebarSyncState = ref<Record<string, SidebarSyncState>>({})
const loading = ref(true)
const errorMessage = ref('')
const selectedMetric = ref<HourlyMetricKey>('temperature')
const selectedRadarLayer = ref<RadarLayerKey>('rain')
const showSettings = ref(false)
const activeSettingsPane = ref<SettingsPane>('general')
const locationQuery = ref('')
const searchResults = ref<LocationSearchResult[]>([])
const searchLoading = ref(false)
const expandedDayKey = ref<string | null>(null)
const savedLocations = ref<LocationSearchResult[]>(loadSavedLocations())
const selectedLocation = ref<LocationSearchResult>(savedLocations.value[0] ?? defaultLocations[0])
const weeklySectionRef = ref<HTMLElement | null>(null)
const uiSettings = ref<UiSettings>(loadSettings())

let refreshTimer: number | null = null

const weatherCardStyle = computed<Record<string, string>>(() => ({
  '--glass-opacity': uiSettings.value.frostEnabled ? String(uiSettings.value.frostOpacity) : '1',
}))

const pageBackgroundStyle = computed<Record<string, string>>(() => {
  const image = uiSettings.value.backgroundImage.trim()
  const style: Record<string, string> = {}
  if (!image) return style
  style.backgroundImage = `linear-gradient(180deg, rgba(226, 237, 255, 0.78), rgba(248, 251, 255, 0.9)), url(${image})`
  style.backgroundSize = 'cover'
  style.backgroundAttachment = 'fixed'
  style.backgroundPosition = 'center'
  return style
})

const apiConfig = computed<WeatherApiConfig>(() => ({
  useCustomApi: uiSettings.value.useCustomApi,
  providerName: uiSettings.value.apiProviderName.trim() || 'open-meteo',
  geocodingBaseUrl: uiSettings.value.geocodingBaseUrl,
  weatherBaseUrl: uiSettings.value.weatherBaseUrl,
  airQualityBaseUrl: uiSettings.value.airQualityBaseUrl,
  apiKey: uiSettings.value.apiKey,
  apiKeyParam: uiSettings.value.apiKeyParam,
  qweatherApiKey: uiSettings.value.qweatherApiKey,
  qweatherCredentialId: uiSettings.value.qweatherCredentialId,
}))

const selectedSavedWeather = computed(() => sidebarWeather.value[selectedLocation.value.key] ?? weather.value)
const heroBackgroundKey = computed<WeatherGlyph>(() => weather.value?.backgroundKey ?? selectedSavedWeather.value?.backgroundKey ?? 'mixed')
const heroIconUrl = computed(() => getSummaryIconUrl(selectedSavedWeather.value ?? weather.value))
const heroMotionClass = computed(() => `weather-theme-${heroBackgroundKey.value}`)
const heroContrastClass = computed(() => {
  if (!uiSettings.value.adaptiveText) return 'hero-contrast-strong'
  return ['rain', 'cloudy', 'mist'].includes(heroBackgroundKey.value) ? 'hero-contrast-strong' : 'hero-contrast-balanced'
})
const heroGifUrl = computed(() => uiSettings.value.enableWeatherGifs ? uiSettings.value.weatherGifUrls[heroBackgroundKey.value]?.trim() ?? '' : '')
const heroSummary = computed(() => {
  if (weather.value) return `${weather.value.locationName} · ${weather.value.description}`
  if (selectedLocation.value) return `${selectedLocation.value.label} · 正在准备天气摘要`
  return '正在同步天气摘要…'
})
const temperatureLabel = computed(() => (weather.value ? `${Math.round(weather.value.currentTemp)}°` : '--'))
const displayedLocations = computed(() => (locationQuery.value.trim().length >= 2 ? searchResults.value : savedLocations.value))
const locationListTitle = computed(() => (locationQuery.value.trim().length >= 2 ? '搜索结果' : '已收藏地区'))
const locationListHint = computed(() => {
  if (locationQuery.value.trim().length >= 2) {
    return searchLoading.value ? '正在匹配地区…' : `匹配到 ${searchResults.value.length} 个结果`
  }
  return `${savedLocations.value.length} 个桌面快捷地区`
})

const orderedMainCards = computed(() => uiSettings.value.cardOrder.filter((key) => ['alerts', 'details', 'hourly', 'weekly'].includes(key) && uiSettings.value.visibleCards[key]))
const orderedSideCards = computed(() => uiSettings.value.cardOrder.filter((key) => ['aqi', 'radar'].includes(key) && uiSettings.value.visibleCards[key]))
const activeRadarLayer = computed(() => radarLayers.find((item) => item.key === selectedRadarLayer.value) ?? radarLayers[0])
const sidebarTiles = computed(() => savedLocations.value.map((location) => {
  const tileWeather = sidebarWeather.value[location.key]
  const iconKey = tileWeather?.backgroundKey ?? tileWeather?.hourly[0]?.icon ?? 'mixed'
  const state = sidebarSyncState.value[location.key] ?? 'idle'
  return {
    location,
    weather: tileWeather,
    state,
    iconUrl: getWeatherIconUrl(tileWeather?.currentIconCode ?? tileWeather?.hourly[0]?.iconCode, iconKey),
    selected: selectedLocation.value.key === location.key,
  }
}))

const activeAlerts = computed(() => {
  if (!uiSettings.value.showAlerts || !weather.value) return []
  return weather.value.alerts.filter((alert) => {
    if (alert.kind === 'rain') return uiSettings.value.showRainAlerts
    if (alert.kind === 'wind') return uiSettings.value.showWindAlerts
    if (alert.kind === 'air') return uiSettings.value.showAirAlerts
    return true
  })
})

const pendingAddLocation = computed(() => {
  const selected = selectedLocation.value
  if (!savedLocations.value.some((item) => item.key === selected.key)) return selected
  return searchResults.value.find((item) => !savedLocations.value.some((saved) => saved.key === item.key)) ?? null
})

const addLocationLabel = computed(() => {
  const pending = pendingAddLocation.value
  return pending ? `添加 ${pending.label}` : '已加入当前地区'
})

const canRemoveLocation = computed(() => savedLocations.value.length > 1 && savedLocations.value.some((item) => item.key === selectedLocation.value.key))
const providerChip = computed(() => uiSettings.value.useCustomApi ? uiSettings.value.apiProviderName.trim() || '高德 + 和风' : '高德 + 和风')

const detailCards = computed(() => {
  if (!weather.value) return []
  return [
    { label: '最高 / 最低', value: `${Math.round(weather.value.highTemp)}° | ${Math.round(weather.value.lowTemp)}°`, icon: Thermometer },
    { label: '体感温度', value: `${Math.round(weather.value.feelsLikeTemp)}°`, icon: Gauge },
    { label: '降雨概率', value: `${weather.value.precipitationChance}%`, icon: Umbrella },
    { label: '风速', value: `${weather.value.windSpeedKph} km/h`, icon: Wind },
  ]
})

const overviewPills = computed(() => {
  if (!weather.value) return []
  return [
    { label: '湿度', value: `${weather.value.humidityPercent}%`, icon: Droplets },
    { label: '坐标', value: `${weather.value.latitude.toFixed(2)}, ${weather.value.longitude.toFixed(2)}`, icon: MapPinned },
    { label: weather.value.cached ? '缓存' : '数据源', value: weather.value.cached ? weather.value.source : providerChip.value, icon: Database },
  ]
})

const heroAsideStats = computed(() => {
  if (!weather.value) return []
  return [
    { label: '空气质量', value: `${weather.value.airQualityIndex} ${weather.value.airQualityLabel}` },
    { label: '天气背景', value: weatherLabelMap[heroBackgroundKey.value] },
    { label: '刷新间隔', value: `${uiSettings.value.refreshIntervalHours} 小时` },
  ]
})

const hourlySeries = computed(() => {
  if (!weather.value) return []
  const rawValues = weather.value.hourly.map((point) => getMetricValue(point, selectedMetric.value))
  const max = Math.max(...rawValues)
  const min = Math.min(...rawValues)
  const span = max - min || 1

  return weather.value.hourly.map((point, index) => {
    const value = rawValues[index]
    const x = weather.value && weather.value.hourly.length > 1 ? (index / (weather.value.hourly.length - 1)) * 100 : 50
    const y = 10 + (1 - (value - min) / span) * 38
    return {
      ...point,
      value,
      displayValue: formatMetricValue(value, selectedMetric.value),
      x,
      y,
      markerIconUrl: getWeatherIconUrl(point.iconCode, point.icon),
    }
  })
})

const hourlyLinePath = computed(() => buildLinePath(hourlySeries.value))
const hourlyAreaPath = computed(() => buildAreaPath(hourlySeries.value))
const airQualityStyle = computed<Record<string, string>>(() => {
  const value = weather.value?.airQualityIndex ?? 0
  const bounded = Math.max(0, Math.min(value, 150))
  return { '--aqi-progress': `${12 + (bounded / 150) * 168}deg` }
})
const mapEmbedUrl = computed(() => {
  const { lat, lon } = selectedLocation.value
  const delta = 0.18
  return `https://www.openstreetmap.org/export/embed.html?bbox=${lon - delta},${lat - delta},${lon + delta},${lat + delta}&layer=mapnik&marker=${lat},${lon}`
})

watch(locationQuery, async (value) => {
  const keyword = value.trim()
  if (keyword.length < 2) {
    searchResults.value = []
    return
  }

  searchLoading.value = true
  try {
    searchResults.value = await searchLocations(keyword, apiConfig.value)
  } catch {
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
})

watch(uiSettings, (value) => {
  localStorage.setItem(settingsStorageKey, JSON.stringify(value))
}, { deep: true })

watch(savedLocations, (value) => {
  localStorage.setItem(locationsStorageKey, JSON.stringify(value))
  if (!value.some((item) => item.key === selectedLocation.value.key)) {
    selectedLocation.value = value[0] ?? defaultLocations[0]
  }
}, { deep: true })

watch(() => uiSettings.value.refreshIntervalHours, () => {
  setupRefreshTimer()
})

function createDefaultGifUrls(): Record<WeatherGlyph, string> {
  return {
    rain: '',
    drizzle: '',
    cloudy: '',
    mist: '',
    mixed: '',
  }
}

function defaultUiSettings(): UiSettings {
  return {
    frostEnabled: true,
    frostOpacity: 0.88,
    backgroundImage: '',
    showHeroScene: true,
    denseMode: true,
    adaptiveText: true,
    playWeatherAnimation: true,
    enableWeatherGifs: false,
    showAlerts: true,
    showRainAlerts: true,
    showWindAlerts: true,
    showAirAlerts: true,
    timeFormat: '24h',
    dateFormat: 'dd/mm/yyyy',
    refreshIntervalHours: 2,
    useCustomApi: true,
    apiProviderName: '高德定位 · 和风天气',
    geocodingBaseUrl: 'https://restapi.amap.com/v3/assistant/inputtips',
    weatherBaseUrl: '',
    airQualityBaseUrl: '',
    apiKey: '',
    apiKeyParam: 'key',
    qweatherApiKey: '',
    qweatherCredentialId: '',
    weatherGifUrls: createDefaultGifUrls(),
    visibleCards: {
      alerts: true,
      details: true,
      hourly: true,
      aqi: true,
      radar: true,
      weekly: true,
    },
    cardOrder: ['alerts', 'details', 'hourly', 'aqi', 'radar', 'weekly'],
  }
}

function loadSettings(): UiSettings {
  const fallback = defaultUiSettings()
  const raw = localStorage.getItem(settingsStorageKey)
  if (!raw) return fallback

  try {
    const parsed = JSON.parse(raw) as Partial<UiSettings>
    const migrated = {
      ...fallback,
      ...parsed,
      visibleCards: { ...fallback.visibleCards, ...parsed.visibleCards },
      weatherGifUrls: { ...fallback.weatherGifUrls, ...parsed.weatherGifUrls },
      cardOrder: Array.isArray(parsed.cardOrder) && parsed.cardOrder.length ? parsed.cardOrder : fallback.cardOrder,
    }

    if (!parsed.useCustomApi || parsed.apiProviderName === 'Open-Meteo') {
      migrated.useCustomApi = true
      migrated.apiProviderName = fallback.apiProviderName
      migrated.geocodingBaseUrl = fallback.geocodingBaseUrl
      migrated.weatherBaseUrl = fallback.weatherBaseUrl
      migrated.apiKey = fallback.apiKey
      migrated.apiKeyParam = fallback.apiKeyParam
      migrated.qweatherApiKey = fallback.qweatherApiKey
      migrated.qweatherCredentialId = fallback.qweatherCredentialId
    }

    return migrated
  } catch {
    return fallback
  }
}

function loadSavedLocations(): LocationSearchResult[] {
  const raw = localStorage.getItem(locationsStorageKey)
  if (!raw) return [...defaultLocations]

  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed) || !parsed.length) return [...defaultLocations]
    const normalized = parsed
      .map(normalizeLocationSearchResult)
      .filter((value): value is LocationSearchResult => value !== null)
    return normalized.length ? normalized : [...defaultLocations]
  } catch {
    return [...defaultLocations]
  }
}

function normalizeLocationSearchResult(value: unknown): LocationSearchResult | null {
  if (!value || typeof value !== 'object') return null
  const target = value as Record<string, unknown>
  const key = typeof target.key === 'string' ? target.key : ''
  const label = typeof target.label === 'string'
    ? target.label
    : typeof target.name === 'string'
      ? target.name
      : ''
  const subtitle = typeof target.subtitle === 'string'
    ? target.subtitle
    : typeof target.regionName === 'string'
      ? target.regionName
      : typeof target.region === 'string'
        ? target.region
        : ''
  const lat = typeof target.lat === 'number' ? target.lat : Number(target.lat)
  const lon = typeof target.lon === 'number' ? target.lon : Number(target.lon)
  const adcode = typeof target.adcode === 'string' ? target.adcode : undefined

  if (!key || !label || !subtitle || Number.isNaN(lat) || Number.isNaN(lon)) return null

  return { key, label, subtitle, lat, lon, adcode }
}

function isLocationSearchResult(value: unknown): value is LocationSearchResult {
  return normalizeLocationSearchResult(value) !== null
}

async function syncWeatherFeed() {
  await Promise.all([loadWeather(), hydrateSidebarWeather()])
}

async function loadWeather() {
  loading.value = true
  errorMessage.value = ''
  sidebarSyncState.value[selectedLocation.value.key] = 'loading'

  try {
    const summary = await getWeatherSummary(
      selectedLocation.value.lat,
      selectedLocation.value.lon,
      selectedLocation.value.label,
      selectedLocation.value.subtitle,
      apiConfig.value,
    )
    weather.value = summary
    sidebarWeather.value[selectedLocation.value.key] = summary
    sidebarSyncState.value[selectedLocation.value.key] = 'ready'
    if (!expandedDayKey.value && summary.daily.length) {
      expandedDayKey.value = dailyKey(summary.daily[0])
    }
  } catch (error) {
    weather.value = null
    sidebarSyncState.value[selectedLocation.value.key] = 'error'
    errorMessage.value = error instanceof Error ? error.message : 'Unknown request error'
  } finally {
    loading.value = false
  }
}

async function hydrateSidebarWeather() {
  const entries = await Promise.all(
    savedLocations.value.map(async (location) => {
      sidebarSyncState.value[location.key] = 'loading'
      try {
        const summary = await getWeatherSummary(location.lat, location.lon, location.label, location.subtitle, apiConfig.value, location.adcode)
        sidebarSyncState.value[location.key] = 'ready'
        return [location.key, summary] as const
      } catch {
        sidebarSyncState.value[location.key] = 'error'
        return null
      }
    }),
  )

  for (const entry of entries) {
    if (entry) {
      sidebarWeather.value[entry[0]] = entry[1]
    }
  }
}

function setupRefreshTimer() {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
  }

  const hours = Math.max(1, uiSettings.value.refreshIntervalHours)
  refreshTimer = window.setInterval(() => {
    void syncWeatherFeed()
  }, hours * 60 * 60 * 1000)
}

function selectLocation(location: LocationSearchResult) {
  selectedLocation.value = location
  void loadWeather()
}

function addPendingLocation() {
  const pending = pendingAddLocation.value
  if (!pending) return
  if (savedLocations.value.some((item) => item.key === pending.key)) return

  savedLocations.value = [pending, ...savedLocations.value]
  selectedLocation.value = pending
  locationQuery.value = ''
  searchResults.value = []
  void loadWeather()
}

function removeSelectedLocation() {
  if (!canRemoveLocation.value) return
  savedLocations.value = savedLocations.value.filter((item) => item.key !== selectedLocation.value.key)
  selectedLocation.value = savedLocations.value[0] ?? defaultLocations[0]
  void loadWeather()
}

function moveCard(key: CardKey, direction: -1 | 1) {
  const currentIndex = uiSettings.value.cardOrder.indexOf(key)
  const nextIndex = currentIndex + direction
  if (currentIndex < 0 || nextIndex < 0 || nextIndex >= uiSettings.value.cardOrder.length) return
  const order = [...uiSettings.value.cardOrder]
  ;[order[currentIndex], order[nextIndex]] = [order[nextIndex], order[currentIndex]]
  uiSettings.value.cardOrder = order
}

function toggleDaily(day: DailyForecastPoint) {
  const key = dailyKey(day)
  expandedDayKey.value = expandedDayKey.value === key ? null : key
}

function isDayExpanded(day: DailyForecastPoint) {
  return expandedDayKey.value === dailyKey(day)
}

function dailyKey(day: DailyForecastPoint) {
  return `${day.dayLabel}-${day.isoDate}`
}

function scrollToWeeklySection() {
  weeklySectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function getMetricValue(point: HourlyForecastPoint, metric: HourlyMetricKey) {
  if (metric === 'precipitation') return point.precipitationChance
  if (metric === 'wind') return point.windSpeedKph
  return point.temperature
}

function formatMetricValue(value: number, metric: HourlyMetricKey) {
  if (metric === 'precipitation') return `${Math.round(value)}%`
  if (metric === 'wind') return `${Math.round(value)} km/h`
  return `${Math.round(value)}°`
}

function formatHourLabel(point: HourlyForecastPoint) {
  const date = new Date(point.isoTime)
  if (Number.isNaN(date.getTime())) return point.hourLabel
  if (uiSettings.value.timeFormat === '12h') {
    return date.toLocaleTimeString('zh-CN', { hour: 'numeric', hour12: true }).replace(/\s/g, '')
  }
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', hour12: false }).slice(0, 2)
}

function formatDateLabel(day: DailyForecastPoint) {
  const date = new Date(day.isoDate)
  if (Number.isNaN(date.getTime())) return day.dateLabel

  const dd = `${date.getDate()}`.padStart(2, '0')
  const mm = `${date.getMonth() + 1}`.padStart(2, '0')
  const yyyy = `${date.getFullYear()}`

  if (uiSettings.value.dateFormat === 'yyyy-mm-dd') return `${yyyy}-${mm}-${dd}`
  if (uiSettings.value.dateFormat === 'mm/dd/yyyy') return `${mm}/${dd}/${yyyy}`
  return `${dd}/${mm}/${yyyy}`
}

function formatDailyPrecipitation(day: DailyForecastPoint) {
  if (day.precipitationChance !== null && Number.isFinite(day.precipitationChance)) {
    return `降雨概率 ${Math.round(day.precipitationChance)}%`
  }
  if (day.precipitationAmountMm !== null && day.precipitationAmountMm !== undefined) {
    return `预计降水 ${day.precipitationAmountMm.toFixed(1)} mm`
  }
  return '暂无降水数据'
}

function alertSeverityLabel(alert: WeatherAlert) {
  if (alert.severity === 'warning') return '预警'
  if (alert.severity === 'watch') return '关注'
  return '提示'
}

function buildLinePath(points: Array<{ x: number; y: number }>) {
  if (!points.length) return ''
  return points.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(2)} ${point.y.toFixed(2)}`).join(' ')
}

function buildAreaPath(points: Array<{ x: number; y: number }>) {
  if (!points.length) return ''
  const start = points[0]
  const end = points[points.length - 1]
  return `${buildLinePath(points)} L ${end.x.toFixed(2)} 56 L ${start.x.toFixed(2)} 56 Z`
}

function restoreDefaultSettings() {
  uiSettings.value = defaultUiSettings()
}

function applySettingsAndRefresh() {
  setupRefreshTimer()
  void syncWeatherFeed()
}

onMounted(async () => {
  setupRefreshTimer()
  await syncWeatherFeed()
})

onUnmounted(() => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
  }
})
</script>

<template>
  <main class="weather-page" :style="pageBackgroundStyle">
    <div class="app-layout" :class="{ dense: uiSettings.denseMode }">
      <aside class="app-rail" :style="weatherCardStyle">
        <button class="rail-button active pressable" type="button" aria-label="天气页">
          <Radar :size="18" />
        </button>
        <button class="rail-button pressable" type="button" @click="showSettings = true; activeSettingsPane = 'general'" aria-label="布局设置">
          <LayoutGrid :size="18" />
        </button>
        <button class="rail-button pressable" type="button" @click="showSettings = true; activeSettingsPane = 'appearance'" aria-label="界面设置">
          <Settings2 :size="18" />
        </button>
        <a class="rail-button pressable" href="https://github.com" target="_blank" rel="noreferrer" aria-label="GitHub 预留">
          <Github :size="18" />
        </a>
      </aside>

      <aside class="location-rail" :style="weatherCardStyle">
        <div class="location-search-head">
          <div class="location-head-copy">
            <p class="location-rail-title">城市与地区</p>
            <p class="location-head-note">桌面磁贴、搜索与收藏切换</p>
          </div>
          <div class="location-search-box">
            <Search :size="16" />
            <input v-model="locationQuery" type="text" placeholder="搜索中国区县 / 海外地区" />
          </div>
        </div>

        <section class="location-spotlight pressable">
          <div class="location-spotlight-head">
            <div>
              <p class="spotlight-kicker">当前地区</p>
              <h2>{{ selectedLocation.label }}</h2>
              <p class="spotlight-region">{{ selectedLocation.subtitle }}</p>
            </div>
            <img class="weather-icon weather-icon-spotlight" :src="heroIconUrl" alt="" />
          </div>
          <div class="location-spotlight-meta">
            <p class="spotlight-temp">{{ selectedSavedWeather ? `${Math.round(selectedSavedWeather.currentTemp)}°` : '--' }}</p>
            <p class="spotlight-copy">{{ selectedSavedWeather?.description ?? '等待天气同步' }}</p>
          </div>
        </section>

        <div class="location-list-wrap">
          <div class="list-header">
            <div>
              <p class="location-list-title">{{ locationListTitle }}</p>
              <p class="location-list-hint">{{ locationListHint }}</p>
            </div>
            <Star :size="16" />
          </div>

          <div class="location-list">
            <button
              v-for="location in displayedLocations"
              :key="location.key"
              type="button"
              class="location-list-item pressable"
              :class="{ active: selectedLocation.key === location.key }"
              @click="selectLocation(location)"
            >
              <div class="location-list-thumb">
                <img class="weather-icon weather-icon-list" :src="getSummaryIconUrl(sidebarWeather[location.key])" alt="" />
              </div>
              <div class="location-list-copy">
                <p class="location-list-name">{{ location.label }}</p>
                <p class="location-list-subtitle">{{ location.subtitle }}</p>
              </div>
              <div class="location-list-metrics">
                <p class="location-list-temp">{{ sidebarWeather[location.key] ? `${Math.round(sidebarWeather[location.key].currentTemp)}°` : '--' }}</p>
                <p class="location-list-desc">{{ sidebarWeather[location.key]?.description ?? (searchLoading ? '检索中' : '点选查看') }}</p>
              </div>
            </button>

            <div v-if="!displayedLocations.length" class="location-empty-state">
              <p class="empty-title">没有匹配结果</p>
              <p class="empty-copy">试试更完整的区县、城市或国家名称</p>
            </div>
          </div>
        </div>

        <div class="location-rail-actions">
          <button class="location-action-button pressable" type="button" :disabled="!pendingAddLocation" @click="addPendingLocation">
            <Plus :size="16" />
            {{ addLocationLabel }}
          </button>
          <div class="location-action-row">
            <button class="location-action-button secondary pressable" type="button" :disabled="!canRemoveLocation" @click="removeSelectedLocation">
              <Trash2 :size="16" />
              删除当前
            </button>
            <button class="location-action-button secondary pressable" type="button" @click="showSettings = true; activeSettingsPane = 'data'">
              <ArrowDownUp :size="16" />
              数据与布局
            </button>
          </div>
        </div>
      </aside>

      <main class="weather-stage">
        <section
          class="hero-card"
          :class="[heroMotionClass, heroContrastClass, { 'weather-motion-disabled': !uiSettings.playWeatherAnimation }]"
          :style="weatherCardStyle"
        >
          <img v-if="heroGifUrl" class="hero-gif" :src="heroGifUrl" alt="" />
          <div class="hero-overlay"></div>

          <div v-if="uiSettings.showHeroScene" class="scene-backdrop" aria-hidden="true">
            <div class="cloud cloud-a"></div>
            <div class="cloud cloud-b"></div>
            <div class="cloud cloud-c"></div>
            <div class="hill hill-back"></div>
            <div class="hill hill-front"></div>
            <div class="weather-particles"></div>
          </div>

          <div class="hero-toolbar">
            <div>
              <p class="hero-temperature">{{ temperatureLabel }}</p>
              <p class="hero-summary">{{ heroSummary }}</p>
            </div>
            <div class="hero-actions">

              <button class="icon-button pressable" type="button" @click="showSettings = true; activeSettingsPane = 'appearance'" aria-label="界面设置">
                <Settings2 :size="18" />
              </button>
              <button class="icon-button pressable" type="button" @click="scrollToWeeklySection" aria-label="查看本周预报">
                <EllipsisVertical :size="18" />
              </button>
            </div>
          </div>

          <div v-if="activeAlerts.length" class="hero-alert-strip">
            <article
              v-for="alert in activeAlerts.slice(0, 3)"
              :key="alert.id"
              class="hero-alert-pill"
              :class="`severity-${alert.severity}`"
            >
              <ShieldAlert :size="14" />
              <span>{{ alert.title }}</span>
            </article>
          </div>

          <div v-if="loading" class="panel-state panel-state-light">
            <p>正在同步天气摘要...</p>
          </div>
          <div v-else-if="errorMessage" class="panel-state panel-state-error">
            <p>天气加载失败</p>
            <p>{{ errorMessage }}</p>
          </div>
          <div v-else-if="weather" class="hero-content">
            <div class="hero-split">
              <div class="hero-copy">
                <div class="hero-location-row">
                  <img class="weather-icon weather-icon-hero" :src="heroIconUrl" alt="" />
                  <div>
                    <h1>{{ weather.locationName }}</h1>
                    <p class="hero-subtitle">{{ weather.regionName }} · {{ weather.conditionLabel }}</p>
                  </div>
                </div>

                <div class="hero-pills">
                  <article v-for="pill in overviewPills" :key="pill.label" class="overview-pill pressable">
                    <component :is="pill.icon" :size="16" />
                    <div>
                      <p class="overview-pill-label">{{ pill.label }}</p>
                      <p class="overview-pill-value">{{ pill.value }}</p>
                    </div>
                  </article>
                </div>
              </div>

              <div class="hero-aside">
                <article v-for="item in heroAsideStats" :key="item.label" class="hero-aside-tile pressable">
                  <p class="hero-aside-label">{{ item.label }}</p>
                  <p class="hero-aside-value">{{ item.value }}</p>
                </article>
              </div>
            </div>

            <div class="hero-meta">
              <p class="hero-range">{{ Math.round(weather.highTemp) }}° / {{ Math.round(weather.lowTemp) }}°</p>
              <p class="hero-updated">{{ weather.updatedAt }}</p>
            </div>
          </div>
        </section>

        <div class="stage-grid">
          <div class="main-stack">
            <section v-if="orderedMainCards.includes('alerts') && activeAlerts.length" class="panel-section alert-section" :style="weatherCardStyle">
              <div class="section-heading">
                <div>
                  <p class="section-kicker">预警提示</p>
                  <h2>天气关注事项</h2>
                </div>
                <p class="section-caption">{{ activeAlerts.length }} 条</p>
              </div>

              <div class="alert-grid">
                <article v-for="alert in activeAlerts" :key="alert.id" class="alert-card pressable" :class="`severity-${alert.severity}`">
                  <div class="alert-card-head">
                    <div class="alert-icon-wrap">
                      <ShieldAlert :size="18" />
                    </div>
                    <div>
                      <p class="alert-title">{{ alert.title }}</p>
                      <p class="alert-severity">{{ alertSeverityLabel(alert) }}</p>
                    </div>
                  </div>
                  <p class="alert-detail">{{ alert.detail }}</p>
                </article>
              </div>
            </section>

            <section v-if="orderedMainCards.includes('details')" class="panel-section detail-section" :style="weatherCardStyle">
              <div class="section-heading">
                <div>
                  <p class="section-kicker">详情</p>
                  <h2>当前天气概览</h2>
                </div>
                <p class="section-caption">可读性增强</p>
              </div>
              <div class="detail-grid">
                <article v-for="card in detailCards" :key="card.label" class="detail-tile pressable">
                  <div class="detail-icon-wrap">
                    <component :is="card.icon" :size="18" />
                  </div>
                  <div>
                    <p class="detail-value">{{ card.value }}</p>
                    <p class="detail-label">{{ card.label }}</p>
                  </div>
                </article>
              </div>
            </section>

            <section v-if="orderedMainCards.includes('hourly')" class="panel-section hourly-section" :style="weatherCardStyle">
              <div class="section-heading">
                <div>
                  <p class="section-kicker">逐时天气</p>
                  <h2>未来时段趋势</h2>
                </div>
                <div class="segmented-control">
                  <button
                    v-for="option in metricOptions"
                    :key="option.key"
                    type="button"
                    class="segment-button pressable"
                    :class="{ active: selectedMetric === option.key }"
                    @click="selectedMetric = option.key"
                  >
                    <component :is="option.icon" :size="15" />
                    <span>{{ option.label }}</span>
                  </button>
                </div>
              </div>

              <div v-if="weather" class="hourly-chart-shell">
                <div class="chart-surface">
                  <svg class="chart-svg" viewBox="0 0 100 56" preserveAspectRatio="none" aria-hidden="true">
                    <path :d="hourlyAreaPath" class="chart-area" />
                    <path :d="hourlyLinePath" class="chart-line" />
                  </svg>
                  <div class="hourly-grid">
                    <article v-for="point in hourlySeries" :key="point.isoTime" class="hourly-column">
                      <p class="hourly-value">{{ point.displayValue }}</p>
                      <img :src="point.markerIconUrl" alt="" class="weather-icon weather-icon-hourly hourly-icon" />
                      <p class="hourly-time">{{ formatHourLabel(point) }}</p>
                      <p class="hourly-meta-text">{{ point.conditionLabel }}</p>
                    </article>
                  </div>
                </div>
              </div>
            </section>

            <section v-if="orderedMainCards.includes('weekly')" ref="weeklySectionRef" class="panel-section weekly-section" :style="weatherCardStyle">
              <div class="section-heading">
                <div>
                  <p class="section-kicker">本周</p>
                  <h2>一周预报</h2>
                </div>
              </div>
              <div v-if="weather" class="daily-list">
                <article v-for="day in weather.daily" :key="dailyKey(day)" class="daily-card" :class="{ expanded: isDayExpanded(day) }">
                  <button class="daily-row pressable" type="button" @click="toggleDaily(day)">
                    <div class="daily-main">
                      <img class="weather-icon weather-icon-daily" :src="getWeatherIconUrl(day.iconCode, day.icon)" alt="" />
                      <div>
                        <p class="daily-title">{{ day.dayLabel }} {{ formatDateLabel(day) }}</p>
                        <p class="daily-subtitle">{{ formatDailyPrecipitation(day) }}</p>
                      </div>
                    </div>
                    <div class="daily-actions">
                      <p class="daily-range">{{ Math.round(day.highTemp) }}° | {{ Math.round(day.lowTemp) }}°</p>
                      <component :is="isDayExpanded(day) ? ChevronUp : ChevronDown" :size="18" />
                    </div>
                  </button>
                  <div v-if="isDayExpanded(day)" class="daily-expand">
                    <p class="daily-expand-copy">{{ day.conditionLabel }}</p>
                    <div class="daily-expand-metrics">
                      <span>风速 {{ day.windSpeedKph }} km/h</span>
                      <span>{{ formatDailyPrecipitation(day) }}</span>
                    </div>
                  </div>
                </article>
              </div>
            </section>
          </div>

          <div class="side-stack">
            <section class="panel-section saved-locations-card" :style="weatherCardStyle">
              <div class="section-heading compact-heading">
                <div>
                  <p class="section-kicker">侧栏磁贴</p>
                  <h2>已收藏天气</h2>
                </div>
                <div class="section-caption tile-hint">
                  <Sparkles :size="14" />
                  点击反馈
                </div>
              </div>
              <div class="saved-tile-grid">
                <button
                  v-for="tile in sidebarTiles"
                  :key="tile.location.key"
                  type="button"
                  class="saved-weather-tile pressable"
                  :class="{ active: tile.selected }"
                  @click="selectLocation(tile.location)"
                >
                  <div class="saved-weather-head">
                    <div>
                      <p class="saved-weather-city">{{ tile.location.label }}</p>
                      <p class="saved-weather-region">{{ tile.location.subtitle }}</p>
                    </div>
                    <img class="weather-icon weather-icon-tile" :src="tile.iconUrl" alt="" />
                  </div>
                  <div class="saved-weather-meta">
                    <p class="saved-weather-temp">{{ tile.weather ? `${Math.round(tile.weather.currentTemp)}°` : tile.state === 'error' ? '!!' : '--' }}</p>
                    <p class="saved-weather-copy">{{ tile.weather?.description ?? (tile.state === 'error' ? '同步失败' : tile.state === 'loading' ? '同步中' : '等待同步') }}</p>
                  </div>
                </button>
              </div>
            </section>

            <section v-if="orderedSideCards.includes('aqi')" class="panel-section air-card" :style="weatherCardStyle">
              <div class="section-heading compact-heading">
                <div>
                  <p class="section-kicker">AQI</p>
                  <h2>空气质量指数</h2>
                </div>
                <p v-if="weather" class="aqi-badge">{{ weather.airQualityLabel }}</p>
              </div>
              <div v-if="weather" class="aqi-content">
                <div>
                  <p class="aqi-highlight">{{ weather.airQualityLabel }}</p>
                  <p class="aqi-copy">{{ weather.airQualitySummary }}</p>
                </div>
                <div class="aqi-gauge" :style="airQualityStyle">
                  <div class="aqi-gauge-inner">
                    <span class="aqi-number">{{ weather.airQualityIndex }}</span>
                    <span class="aqi-unit">AQI</span>
                  </div>
                </div>
              </div>
            </section>

            <section v-if="orderedSideCards.includes('radar')" class="panel-section radar-card" :style="weatherCardStyle">
              <div class="section-heading compact-heading">
                <div>
                  <p class="section-kicker">地图与雷达</p>
                  <h2>免费地图预览</h2>
                </div>
                <Radar :size="18" />
              </div>
              <div class="radar-layer-row">
                <button
                  v-for="layer in radarLayers"
                  :key="layer.key"
                  type="button"
                  class="radar-layer-chip pressable"
                  :class="{ active: selectedRadarLayer === layer.key }"
                  @click="selectedRadarLayer = layer.key"
                >
                  {{ layer.label }}
                </button>
              </div>
              <div class="radar-surface">
                <div class="map-frame-wrap">
                  <iframe class="map-frame" :src="mapEmbedUrl" loading="lazy" referrerpolicy="no-referrer-when-downgrade"></iframe>
                  <div class="radar-overlay" :class="`radar-overlay-${selectedRadarLayer}`">
                    <div class="radar-ring"></div>
                    <div class="radar-ring radar-ring-delayed"></div>
                    <div class="radar-dot"></div>
                  </div>
                </div>
                <div class="radar-footer">
                  <div>
                    <p class="radar-title">{{ selectedLocation.label }} {{ activeRadarLayer.label }}</p>
                    <p class="radar-caption">{{ activeRadarLayer.description }}</p>
                  </div>
                  <p class="radar-status">OpenStreetMap</p>
                </div>
              </div>
            </section>
          </div>
        </div>
      </main>
    </div>

    <div v-if="showSettings" class="overlay-mask" @click.self="showSettings = false">
      <div class="overlay-card settings-shell" :style="weatherCardStyle">
        <aside class="settings-nav">
          <div class="settings-nav-head">
            <p class="section-kicker">设置</p>
            <h3>Weather Preferences</h3>
          </div>

          <button
            v-for="item in settingsNavItems"
            :key="item.key"
            type="button"
            class="settings-nav-item pressable"
            :class="{ active: activeSettingsPane === item.key }"
            @click="activeSettingsPane = item.key"
          >
            <div class="settings-nav-icon">
              <component :is="item.icon" :size="18" />
            </div>
            <div class="settings-nav-copy">
              <p class="settings-nav-title">{{ item.label }}</p>
              <p class="settings-nav-desc">{{ item.description }}</p>
            </div>
            <ChevronRight :size="16" />
          </button>
        </aside>

        <section class="settings-detail">
          <div class="overlay-head settings-detail-head">
            <div>
              <p class="section-kicker">{{ settingsNavItems.find((item) => item.key === activeSettingsPane)?.label }}</p>
              <h3>{{ settingsNavItems.find((item) => item.key === activeSettingsPane)?.description }}</h3>
            </div>
            <div class="settings-detail-actions">
              <button class="icon-button pressable overlay-close" type="button" @click="applySettingsAndRefresh()">
                <RefreshCw :size="18" />
              </button>
              <button class="icon-button pressable overlay-close" type="button" @click="showSettings = false">
                <ChevronUp :size="18" />
              </button>
            </div>
          </div>

          <template v-if="activeSettingsPane === 'general'">
            <section class="settings-group card-section">
              <div class="settings-title-row">
                <LayoutGrid :size="16" />
                <span>布局与卡片</span>
              </div>

              <label class="setting-row">
                <div>
                  <p class="setting-label">紧凑布局</p>
                  <p class="setting-note">减少留白，保持桌面信息密度</p>
                </div>
                <input v-model="uiSettings.denseMode" type="checkbox" class="switch-input" />
              </label>

              <label class="setting-row">
                <div>
                  <p class="setting-label">顶部场景背景</p>
                  <p class="setting-note">显示英雄区天气场景与氛围层</p>
                </div>
                <input v-model="uiSettings.showHeroScene" type="checkbox" class="switch-input" />
              </label>

              <div class="settings-card-list">
                <div v-for="item in cardMeta" :key="item.key" class="settings-card-item">
                  <label class="setting-row card-inline">
                    <div>
                      <p class="setting-label">{{ item.label }}</p>
                      <p class="setting-note">控制模块显示与顺序</p>
                    </div>
                    <input v-model="uiSettings.visibleCards[item.key]" type="checkbox" class="switch-input" />
                  </label>
                  <div class="card-order-actions">
                    <button type="button" class="mini-icon-button pressable" @click="moveCard(item.key, -1)" aria-label="上移">
                      <ChevronUp :size="14" />
                    </button>
                    <button type="button" class="mini-icon-button pressable" @click="moveCard(item.key, 1)" aria-label="下移">
                      <ChevronDown :size="14" />
                    </button>
                  </div>
                </div>
              </div>
            </section>
          </template>

          <template v-else-if="activeSettingsPane === 'data'">
            <section class="settings-group">
              <div class="settings-title-row">
                <Database :size="16" />
                <span>数据</span>
              </div>

              <div class="setting-row">
                <div>
                  <p class="setting-label">时间格式</p>
                  <p class="setting-note">影响逐时天气显示</p>
                </div>
                <div class="pill-select">
                  <button type="button" class="pill-select-button pressable" :class="{ active: uiSettings.timeFormat === '24h' }" @click="uiSettings.timeFormat = '24h'">24h</button>
                  <button type="button" class="pill-select-button pressable" :class="{ active: uiSettings.timeFormat === '12h' }" @click="uiSettings.timeFormat = '12h'">12h</button>
                </div>
              </div>

              <div class="setting-row">
                <div>
                  <p class="setting-label">日期格式</p>
                  <p class="setting-note">影响每周预报日期显示</p>
                </div>
                <div class="pill-select date-pills">
                  <button type="button" class="pill-select-button pressable" :class="{ active: uiSettings.dateFormat === 'dd/mm/yyyy' }" @click="uiSettings.dateFormat = 'dd/mm/yyyy'">dd/mm/yyyy</button>
                  <button type="button" class="pill-select-button pressable" :class="{ active: uiSettings.dateFormat === 'yyyy-mm-dd' }" @click="uiSettings.dateFormat = 'yyyy-mm-dd'">yyyy-mm-dd</button>
                  <button type="button" class="pill-select-button pressable" :class="{ active: uiSettings.dateFormat === 'mm/dd/yyyy' }" @click="uiSettings.dateFormat = 'mm/dd/yyyy'">mm/dd/yyyy</button>
                </div>
              </div>

              <div class="setting-row">
                <div>
                  <p class="setting-label">刷新间隔</p>
                  <p class="setting-note">自动同步天气数据与缓存更新</p>
                </div>
                <div class="pill-select">
                  <button type="button" class="pill-select-button pressable" :class="{ active: uiSettings.refreshIntervalHours === 1 }" @click="uiSettings.refreshIntervalHours = 1">1 小时</button>
                  <button type="button" class="pill-select-button pressable" :class="{ active: uiSettings.refreshIntervalHours === 2 }" @click="uiSettings.refreshIntervalHours = 2">2 小时</button>
                  <button type="button" class="pill-select-button pressable" :class="{ active: uiSettings.refreshIntervalHours === 6 }" @click="uiSettings.refreshIntervalHours = 6">6 小时</button>
                </div>
              </div>
            </section>

            <section class="settings-group">
              <div class="settings-title-row">
                <SatelliteDish :size="16" />
                <span>天气数据源</span>
              </div>

              <label class="setting-row">
                <div>
                  <p class="setting-label">启用高德与和风接口</p>
                  <p class="setting-note">地区检索使用高德，实况天气与空气质量使用和风天气</p>
                </div>
                <input v-model="uiSettings.useCustomApi" type="checkbox" class="switch-input" />
              </label>

              <label class="setting-field">
                <span>数据源名称</span>
                <input v-model="uiSettings.apiProviderName" type="text" placeholder="高德定位 · 和风天气" />
              </label>
              <label class="setting-field">
                <span>高德检索接口</span>
                <input v-model="uiSettings.geocodingBaseUrl" type="text" placeholder="https://restapi.amap.com/..." />
              </label>
              <label class="setting-field">
                <span>和风 API Host</span>
                <input v-model="uiSettings.weatherBaseUrl" type="text" placeholder="https://你的和风 API Host" />
              </label>
              <label class="setting-field">
                <span>空气质量接口</span>
                <input v-model="uiSettings.airQualityBaseUrl" type="text" placeholder="留空则跟随和风 Host" />
              </label>
              <div class="setting-field-grid">
                <label class="setting-field">
                  <span>高德 Key</span>
                  <input v-model="uiSettings.apiKey" type="password" placeholder="用于地区检索，不会提交到仓库" autocomplete="off" />
                </label>
                <label class="setting-field">
                  <span>Key 参数名</span>
                  <input v-model="uiSettings.apiKeyParam" type="text" placeholder="key" />
                </label>
              </div>
              <div class="setting-field-grid">
                <label class="setting-field">
                  <span>和风 API Key</span>
                  <input v-model="uiSettings.qweatherApiKey" type="password" placeholder="用于天气实况与预报，不会提交到仓库" autocomplete="off" />
                </label>
                <label class="setting-field">
                  <span>和风凭据 ID</span>
                  <input v-model="uiSettings.qweatherCredentialId" type="password" placeholder="可留空" autocomplete="off" />
                </label>
              </div>
            </section>
          </template>

          <template v-else-if="activeSettingsPane === 'alerts'">
            <section class="settings-group">
              <div class="settings-title-row">
                <BellRing :size="16" />
                <span>应用程序中的天气预警</span>
              </div>

              <label class="setting-row">
                <div>
                  <p class="setting-label">启用预警显示</p>
                  <p class="setting-note">在首页展示暴雨、大风与空气提醒</p>
                </div>
                <input v-model="uiSettings.showAlerts" type="checkbox" class="switch-input" />
              </label>
              <label class="setting-row">
                <div>
                  <p class="setting-label">暴雨和强降雨提醒</p>
                  <p class="setting-note">根据降雨概率与天气码生成预警提示</p>
                </div>
                <input v-model="uiSettings.showRainAlerts" type="checkbox" class="switch-input" />
              </label>
              <label class="setting-row">
                <div>
                  <p class="setting-label">大风提醒</p>
                  <p class="setting-note">根据当前风速与日最大风速给出提示</p>
                </div>
                <input v-model="uiSettings.showWindAlerts" type="checkbox" class="switch-input" />
              </label>
              <label class="setting-row">
                <div>
                  <p class="setting-label">空气质量提醒</p>
                  <p class="setting-note">AQI 偏高时给出注意事项</p>
                </div>
                <input v-model="uiSettings.showAirAlerts" type="checkbox" class="switch-input" />
              </label>
            </section>
          </template>

          <template v-else-if="activeSettingsPane === 'appearance'">
            <section class="settings-group">
              <div class="settings-title-row">
                <Palette :size="16" />
                <span>外观</span>
              </div>

              <label class="setting-row">
                <div>
                  <p class="setting-label">磨砂玻璃</p>
                  <p class="setting-note">卡片保留半透明与毛玻璃效果</p>
                </div>
                <input v-model="uiSettings.frostEnabled" type="checkbox" class="switch-input" />
              </label>
              <label class="setting-field">
                <span>玻璃透明度</span>
                <input v-model="uiSettings.frostOpacity" type="range" min="0.55" max="1" step="0.05" />
              </label>
              <label class="setting-row">
                <div>
                  <p class="setting-label">字体根据背景增强对比</p>
                  <p class="setting-note">提高深浅背景上的文字清晰度</p>
                </div>
                <input v-model="uiSettings.adaptiveText" type="checkbox" class="switch-input" />
              </label>
              <label class="setting-row">
                <div>
                  <p class="setting-label">播放天气动效</p>
                  <p class="setting-note">包含卡片反馈、天气背景与雷达动画</p>
                </div>
                <input v-model="uiSettings.playWeatherAnimation" type="checkbox" class="switch-input" />
              </label>
              <label class="setting-row">
                <div>
                  <p class="setting-label">播放天气 GIF 背景</p>
                  <p class="setting-note">可为不同天气填入独立 GIF 地址</p>
                </div>
                <input v-model="uiSettings.enableWeatherGifs" type="checkbox" class="switch-input" />
              </label>
              <label class="setting-field">
                <span>全局背景图片 URL</span>
                <input v-model="uiSettings.backgroundImage" type="text" placeholder="https://..." />
              </label>
            </section>

            <section v-if="uiSettings.enableWeatherGifs" class="settings-group">
              <div class="settings-title-row">
                <Sparkles :size="16" />
                <span>按天气配置 GIF</span>
              </div>
              <div class="setting-field-grid">
                <label v-for="entry in Object.keys(uiSettings.weatherGifUrls) as WeatherGlyph[]" :key="entry" class="setting-field">
                  <span>{{ weatherLabelMap[entry] }} GIF</span>
                  <input v-model="uiSettings.weatherGifUrls[entry]" type="text" placeholder="https://..." />
                </label>
              </div>
            </section>
          </template>

          <div class="settings-footer">
            <button class="location-action-button secondary pressable" type="button" @click="restoreDefaultSettings()">恢复默认</button>
            <button class="location-action-button pressable" type="button" @click="applySettingsAndRefresh()">应用并刷新</button>
          </div>
        </section>
      </div>
    </div>
  </main>
</template>















