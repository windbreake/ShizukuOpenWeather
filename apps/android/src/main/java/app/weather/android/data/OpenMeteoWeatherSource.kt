package app.weather.android.data

import app.weather.android.model.ApiSettings
import app.weather.android.model.DailyForecast
import app.weather.android.model.HourlyForecast
import app.weather.android.model.LocationResult
import app.weather.android.model.WeatherCodeMapper
import app.weather.android.model.WeatherSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class OpenMeteoWeatherSource {
    suspend fun load(location: LocationResult, settings: ApiSettings): WeatherSummary =
        withContext(Dispatchers.IO) {
            val weatherUrl = JsonHttp.buildUrl(
                settings.openMeteoWeatherUrl,
                mapOf(
                    "latitude" to location.latitude.toString(),
                    "longitude" to location.longitude.toString(),
                    "current" to "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m",
                    "hourly" to "temperature_2m,precipitation_probability,weather_code,wind_speed_10m",
                    "daily" to "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max",
                    "timezone" to "auto",
                    "forecast_days" to "7",
                ),
            )
            val airUrl = JsonHttp.buildUrl(
                settings.openMeteoAirUrl,
                mapOf(
                    "latitude" to location.latitude.toString(),
                    "longitude" to location.longitude.toString(),
                    "current" to "us_aqi",
                    "timezone" to "auto",
                ),
            )
            val root = JsonHttp.get(weatherUrl)
            val airRoot = runCatching { JsonHttp.get(airUrl) }.getOrNull()
            buildSummary(location, settings, root, airRoot)
        }

    private fun buildSummary(
        location: LocationResult,
        settings: ApiSettings,
        root: JSONObject,
        airRoot: JSONObject?,
    ): WeatherSummary {
        val current = root.getJSONObject("current")
        val currentCode = current.optInt("weather_code")
        val currentMeta = WeatherCodeMapper.openMeteo(currentCode)
        val hourly = parseHourly(root)
        val daily = parseDaily(root)
        val aqi = airRoot?.optJSONObject("current")?.optInt("us_aqi") ?: 0
        val aqiMeta = WeatherCodeMapper.aqi(aqi)
        val precipitation = hourly.firstOrNull()?.precipitationChance ?: 0
        val wind = current.optDouble("wind_speed_10m").toInt()
        val maxWind = daily.firstOrNull()?.windSpeedKph ?: wind

        return WeatherSummary(
            location = location,
            currentTemp = current.optDouble("temperature_2m"),
            highTemp = daily.firstOrNull()?.highTemp ?: current.optDouble("temperature_2m"),
            lowTemp = daily.firstOrNull()?.lowTemp ?: current.optDouble("temperature_2m"),
            feelsLikeTemp = current.optDouble("apparent_temperature"),
            humidityPercent = current.optInt("relative_humidity_2m"),
            precipitationChance = precipitation,
            windSpeedKph = wind,
            description = currentMeta.second,
            conditionLabel = "${currentMeta.second} · 风速 $wind km/h",
            glyph = currentMeta.first,
            iconCode = null,
            source = settings.providerName.ifBlank { "Open-Meteo" },
            cached = false,
            airQualityIndex = aqi,
            airQualityLabel = aqiMeta.first,
            airQualitySummary = aqiMeta.second,
            updatedAtEpochMs = System.currentTimeMillis(),
            alerts = AlertBuilder.risks(precipitation, wind, maxWind, aqi, daily.firstOrNull()),
            hourly = hourly,
            daily = daily,
        )
    }

    private fun parseHourly(root: JSONObject): List<HourlyForecast> {
        val data = root.getJSONObject("hourly")
        val times = data.getJSONArray("time")
        val temperatures = data.getJSONArray("temperature_2m")
        val precipitation = data.getJSONArray("precipitation_probability")
        val codes = data.getJSONArray("weather_code")
        val winds = data.getJSONArray("wind_speed_10m")
        val currentTime = root.optJSONObject("current")?.optString("time").orEmpty()
        val start = (0 until times.length()).firstOrNull { times.optString(it) >= currentTime } ?: 0
        val end = minOf(start + 12, times.length())

        return (start until end).map { index ->
            val meta = WeatherCodeMapper.openMeteo(codes.optInt(index))
            val time = times.optString(index)
            HourlyForecast(
                time = time,
                hourLabel = time.substringAfter('T').take(5),
                temperature = temperatures.optDouble(index),
                precipitationChance = precipitation.optInt(index),
                windSpeedKph = winds.optDouble(index).toInt(),
                glyph = meta.first,
                iconCode = null,
                conditionLabel = meta.second,
            )
        }
    }

    private fun parseDaily(root: JSONObject): List<DailyForecast> {
        val data = root.getJSONObject("daily")
        val dates = data.getJSONArray("time")
        val codes = data.getJSONArray("weather_code")
        val highs = data.getJSONArray("temperature_2m_max")
        val lows = data.getJSONArray("temperature_2m_min")
        val precipitation = data.getJSONArray("precipitation_probability_max")
        val winds = data.getJSONArray("wind_speed_10m_max")

        return (0 until dates.length()).map { index ->
            val date = dates.optString(index)
            val localDate = LocalDate.parse(date)
            val meta = WeatherCodeMapper.openMeteo(codes.optInt(index))
            DailyForecast(
                date = date,
                dayLabel = if (index == 0) "今天" else localDate.dayOfWeek.zhLabel(),
                dateLabel = localDate.format(DateTimeFormatter.ofPattern("MM/dd")),
                highTemp = highs.optDouble(index),
                lowTemp = lows.optDouble(index),
                precipitationChance = precipitation.optInt(index),
                precipitationAmountMm = null,
                windSpeedKph = winds.optDouble(index).toInt(),
                glyph = meta.first,
                iconCode = null,
                conditionLabel = meta.second,
            )
        }
    }
}

internal fun java.time.DayOfWeek.zhLabel(): String = when (this) {
    java.time.DayOfWeek.MONDAY -> "周一"
    java.time.DayOfWeek.TUESDAY -> "周二"
    java.time.DayOfWeek.WEDNESDAY -> "周三"
    java.time.DayOfWeek.THURSDAY -> "周四"
    java.time.DayOfWeek.FRIDAY -> "周五"
    java.time.DayOfWeek.SATURDAY -> "周六"
    java.time.DayOfWeek.SUNDAY -> "周日"
}
