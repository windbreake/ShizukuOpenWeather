package app.weather.android.data

import android.content.Context

import app.weather.android.model.ApiSettings
import app.weather.android.model.LocationResult
import app.weather.android.model.ProviderMode
import app.weather.android.model.WeatherSummary

class WeatherApiClient(context: Context) {
    private val locations = LocationSource(context)
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
