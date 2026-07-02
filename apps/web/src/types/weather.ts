export interface WeatherSummary {
  locationName: string
  latitude: number
  longitude: number
  temperature: number
  description: string
  source: string
  cached: boolean
}
