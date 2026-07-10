export type WeatherGlyph = 'rain' | 'drizzle' | 'cloudy' | 'mist' | 'mixed'

export type WeatherAlertSeverity = 'info' | 'watch' | 'warning'
export type WeatherAlertKind = 'rain' | 'wind' | 'air' | 'heat' | 'cold'

export interface HourlyForecastPoint {
  hourLabel: string
  isoTime: string
  temperature: number
  precipitationChance: number
  windSpeedKph: number
  icon: WeatherGlyph
  iconCode?: string
  conditionLabel: string
}

export interface DailyForecastPoint {
  dayLabel: string
  dateLabel: string
  isoDate: string
  highTemp: number
  lowTemp: number
  precipitationChance: number | null
  precipitationAmountMm?: number | null
  windSpeedKph: number
  icon: WeatherGlyph
  iconCode?: string
  conditionLabel: string
}

export interface WeatherAlert {
  id: string
  title: string
  severity: WeatherAlertSeverity
  kind: WeatherAlertKind
  detail: string
}

export interface WeatherSummary {
  locationName: string
  regionName: string
  latitude: number
  longitude: number
  currentTemp: number
  highTemp: number
  lowTemp: number
  feelsLikeTemp: number
  humidityPercent: number
  precipitationChance: number
  windSpeedKph: number
  description: string
  conditionLabel: string
  backgroundKey: WeatherGlyph
  currentIconCode?: string
  source: string
  cached: boolean
  airQualityIndex: number
  airQualityLabel: string
  airQualitySummary: string
  updatedAt: string
  alerts: WeatherAlert[]
  hourly: HourlyForecastPoint[]
  daily: DailyForecastPoint[]
}
