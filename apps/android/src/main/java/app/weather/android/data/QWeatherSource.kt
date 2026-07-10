package app.weather.android.data

import app.weather.android.model.ApiSettings
import app.weather.android.model.LocationResult
import app.weather.android.model.WeatherCodeMapper
import app.weather.android.model.WeatherSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject

internal class QWeatherSource {
    suspend fun load(location: LocationResult, settings: ApiSettings): WeatherSummary =
        coroutineScope {
            require(settings.qWeatherHost.isNotBlank()) {
                "请先在设置中填写和风天气 API Host"
            }
            require(settings.qWeatherApiKey.isNotBlank()) {
                "请先在设置中填写和风天气 API Key"
            }

            val locationQuery = "${location.longitude},${location.latitude}"
            val headers = mapOf("X-QW-Api-Key" to settings.qWeatherApiKey)
            val nowTask = async(Dispatchers.IO) {
                JsonHttp.get(url(settings.qWeatherHost, "v7/weather/now", locationQuery), headers)
            }
            val hourlyTask = async(Dispatchers.IO) {
                JsonHttp.get(url(settings.qWeatherHost, "v7/weather/24h", locationQuery), headers)
            }
            val dailyTask = async(Dispatchers.IO) {
                JsonHttp.get(url(settings.qWeatherHost, "v7/weather/7d", locationQuery), headers)
            }
            val airTask = async(Dispatchers.IO) {
                runCatching { loadAir(location, settings, headers) }.getOrNull()
            }
            val warningTask = async(Dispatchers.IO) {
                runCatching {
                    JsonHttp.get(
                        url(settings.qWeatherHost, "v7/warning/now", locationQuery),
                        headers,
                    )
                }.getOrNull()
            }

            val current = nowTask.await().getJSONObject("now")
            val hourly = QWeatherParser.hourly(hourlyTask.await())
            val daily = QWeatherParser.daily(dailyTask.await())
            val airIndex = airTask.await()?.optJSONArray("indexes")?.optJSONObject(0)
            val officialAlerts = QWeatherParser.warnings(warningTask.await())
            val aqi = airIndex?.optInt("aqi") ?: 0
            val aqiMeta = WeatherCodeMapper.aqi(aqi)
            val text = current.optString("text", "天气变化")
            val iconCode = current.optString("icon").takeIf { it.isNotBlank() }
            val wind = current.optDouble("windSpeed").toInt()
            val precipitation = hourly.firstOrNull()?.precipitationChance ?: 0
            val riskAlerts = AlertBuilder.risks(
                precipitation,
                wind,
                daily.firstOrNull()?.windSpeedKph ?: wind,
                aqi,
                daily.firstOrNull(),
            )

            WeatherSummary(
                location = location,
                currentTemp = current.optDouble("temp"),
                highTemp = daily.firstOrNull()?.highTemp ?: current.optDouble("temp"),
                lowTemp = daily.firstOrNull()?.lowTemp ?: current.optDouble("temp"),
                feelsLikeTemp = current.optDouble("feelsLike", current.optDouble("temp")),
                humidityPercent = current.optInt("humidity"),
                precipitationChance = precipitation,
                windSpeedKph = wind,
                description = text,
                conditionLabel = "$text · 风速 $wind km/h · 湿度 ${current.optInt("humidity")}%",
                glyph = WeatherCodeMapper.qWeather(iconCode, text),
                iconCode = iconCode,
                source = settings.providerName.ifBlank { "和风天气" },
                cached = false,
                airQualityIndex = aqi,
                airQualityLabel = airIndex?.optString("category")
                    .orEmpty()
                    .ifBlank { aqiMeta.first },
                airQualitySummary = airIndex?.optJSONObject("health")
                    ?.optString("effect")
                    .orEmpty()
                    .ifBlank { aqiMeta.second },
                updatedAtEpochMs = System.currentTimeMillis(),
                alerts = if (officialAlerts.isNotEmpty()) officialAlerts else riskAlerts,
                hourly = hourly,
                daily = daily,
            )
        }

    private fun loadAir(
        location: LocationResult,
        settings: ApiSettings,
        headers: Map<String, String>,
    ): JSONObject {
        val host = settings.qWeatherAirHost.ifBlank { settings.qWeatherHost }
        val path = "airquality/v1/current/${location.latitude}/${location.longitude}"
        return JsonHttp.get(
            JsonHttp.buildUrl(
                JsonHttp.normalizeBase(host) + "/" + path,
                mapOf("lang" to "zh"),
            ),
            headers,
        )
    }

    private fun url(host: String, path: String, location: String): String =
        JsonHttp.buildUrl(
            JsonHttp.normalizeBase(host) + "/" + path,
            mapOf("location" to location, "lang" to "zh"),
        )
}
