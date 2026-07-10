package app.weather.android.data

import app.weather.android.model.ApiSettings
import app.weather.android.model.LocationResult
import app.weather.android.model.ProviderMode
import app.weather.android.model.WeatherSummary

class WeatherApiClient {
    private val locations = LocationSource()
    private val openMeteo = OpenMeteoWeatherSource()
    private val qWeather = QWeatherSource()

    suspend fun search(query: String, settings: ApiSettings): List<LocationResult> =
        locations.search(query, settings)

    suspend fun weather(location: LocationResult, settings: ApiSettings): WeatherSummary =
        when (settings.providerMode) {
            ProviderMode.OPEN_METEO -> openMeteo.load(location, settings)
            ProviderMode.QWEATHER -> qWeather.load(location, settings)
        }
}
