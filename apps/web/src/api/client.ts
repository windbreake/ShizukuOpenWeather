import type { WeatherSummary } from '../types/weather'

export type LocationSearchResult = {
  key: string
  label: string
  subtitle: string
  lat: number
  lon: number
  adcode?: string
}

export type WeatherApiConfig = {
  useCustomApi: boolean
  providerName: string
  geocodingBaseUrl: string
  weatherBaseUrl: string
  airQualityBaseUrl: string
  apiKey: string
  apiKeyParam: string
  qweatherApiKey: string
  qweatherCredentialId: string
}

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`)
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }
  return response.json() as Promise<T>
}

function appendApiConfig(params: URLSearchParams, config?: WeatherApiConfig) {
  if (!config) return

  params.set('providerName', config.providerName)
  params.set('useCustomApi', String(config.useCustomApi))

  if (!config.useCustomApi) return

  if (config.geocodingBaseUrl.trim()) {
    params.set('geocodingUrl', config.geocodingBaseUrl.trim())
  }

  if (config.weatherBaseUrl.trim()) {
    params.set('weatherUrl', config.weatherBaseUrl.trim())
  }

  if (config.airQualityBaseUrl.trim()) {
    params.set('airQualityUrl', config.airQualityBaseUrl.trim())
  }

  if (config.apiKey.trim()) {
    params.set('apiKey', config.apiKey.trim())
  }

  if (config.apiKeyParam.trim()) {
    params.set('apiKeyParam', config.apiKeyParam.trim())
  }

  if (config.qweatherApiKey.trim()) {
    params.set('qweatherApiKey', config.qweatherApiKey.trim())
  }

  if (config.qweatherCredentialId.trim()) {
    params.set('qweatherCredentialId', config.qweatherCredentialId.trim())
  }
}

export function getWeatherSummary(
  lat: number,
  lon: number,
  locationName?: string,
  regionName?: string,
  apiConfig?: WeatherApiConfig,
  adcode?: string,
) {
  const params = new URLSearchParams({
    lat: String(lat),
    lon: String(lon),
  })

  if (locationName) {
    params.set('locationName', locationName)
  }

  if (regionName) {
    params.set('regionName', regionName)
  }

  if (adcode?.trim()) {
    params.set('adcode', adcode.trim())
  }

  appendApiConfig(params, apiConfig)

  return getJson<WeatherSummary>(`/api/weather/summary?${params.toString()}`)
}

export function searchLocations(query: string, apiConfig?: WeatherApiConfig) {
  const params = new URLSearchParams({ q: query })
  appendApiConfig(params, apiConfig)
  return getJson<LocationSearchResult[]>(`/api/locations/search?${params.toString()}`)
}

