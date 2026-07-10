package app.weather.android.model

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

enum class ProviderMode {
    OPEN_METEO,
    QWEATHER,
}

enum class WeatherGlyph {
    CLEAR,
    CLOUDY,
    RAIN,
    DRIZZLE,
    STORM,
    SNOW,
    MIST,
}

enum class AlertSeverity {
    INFO,
    WATCH,
    WARNING,
}

enum class AlertKind {
    RAIN,
    WIND,
    AIR,
    HEAT,
    COLD,
}

data class LocationResult(
    val key: String,
    val label: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val adcode: String? = null,
) {
    val cacheIdentity: String
        get() = "$key|${latitude.toBits()}|${longitude.toBits()}"
}

data class HourlyForecast(
    val time: String,
    val hourLabel: String,
    val temperature: Double,
    val precipitationChance: Int,
    val windSpeedKph: Int,
    val glyph: WeatherGlyph,
    val iconCode: String?,
    val conditionLabel: String,
)

data class DailyForecast(
    val date: String,
    val dayLabel: String,
    val dateLabel: String,
    val highTemp: Double,
    val lowTemp: Double,
    val precipitationChance: Int?,
    val precipitationAmountMm: Double? = null,
    val windSpeedKph: Int,
    val glyph: WeatherGlyph,
    val iconCode: String?,
    val conditionLabel: String,
)

data class WeatherAlert(
    val id: String,
    val title: String,
    val severity: AlertSeverity,
    val kind: AlertKind,
    val detail: String,
    val official: Boolean,
)

data class WeatherSummary(
    val location: LocationResult,
    val currentTemp: Double,
    val highTemp: Double,
    val lowTemp: Double,
    val feelsLikeTemp: Double,
    val humidityPercent: Int,
    val precipitationChance: Int,
    val windSpeedKph: Int,
    val description: String,
    val conditionLabel: String,
    val glyph: WeatherGlyph,
    val iconCode: String?,
    val source: String,
    val cached: Boolean,
    val airQualityIndex: Int,
    val airQualityLabel: String,
    val airQualitySummary: String,
    val updatedAtEpochMs: Long,
    val alerts: List<WeatherAlert>,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
) {
    val qWeatherIconUrl: String?
        get() = iconCode?.takeIf { it.isNotBlank() }?.let {
            "https://cdn.jsdelivr.net/npm/qweather-icons/icons/$it.svg"
        }
}

data class ApiSettings(
    val providerMode: ProviderMode = ProviderMode.OPEN_METEO,
    val providerName: String = "Open-Meteo",
    val openMeteoWeatherUrl: String = "https://api.open-meteo.com/v1/forecast",
    val openMeteoAirUrl: String = "https://air-quality-api.open-meteo.com/v1/air-quality",
    val geocodingUrl: String = "https://geocoding-api.open-meteo.com/v1/search",
    val qWeatherHost: String = "",
    val qWeatherAirHost: String = "",
    val qWeatherApiKey: String = "",
    val amapGeocodingUrl: String = "https://restapi.amap.com/v3/assistant/inputtips",
    val amapApiKey: String = "",
) {
    val weatherCacheIdentity: String
        get() {
            val material = listOf(
                providerMode.name,
                providerName,
                openMeteoWeatherUrl,
                openMeteoAirUrl,
                qWeatherHost,
                qWeatherAirHost,
                qWeatherApiKey,
            ).joinToString("\u001F")
            return MessageDigest.getInstance("SHA-256")
                .digest(material.toByteArray(StandardCharsets.UTF_8))
                .take(12)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }
}

data class AppearanceSettings(
    val frostEnabled: Boolean = true,
    val cardOpacity: Float = 0.88f,
    val animationsEnabled: Boolean = true,
    val adaptiveText: Boolean = true,
    val backgroundImageUrl: String = "",
    val showAlerts: Boolean = true,
    val showAirQuality: Boolean = true,
    val showHourly: Boolean = true,
    val showDaily: Boolean = true,
)

data class AppSettings(
    val api: ApiSettings = ApiSettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val refreshIntervalHours: Int = 2,
)

object WeatherCodeMapper {
    fun openMeteo(code: Int): Pair<WeatherGlyph, String> = when (code) {
        0 -> WeatherGlyph.CLEAR to "晴"
        1 -> WeatherGlyph.CLEAR to "大部晴朗"
        2 -> WeatherGlyph.CLOUDY to "局部多云"
        3 -> WeatherGlyph.CLOUDY to "阴"
        45, 48 -> WeatherGlyph.MIST to "雾"
        51, 53, 55, 56, 57 -> WeatherGlyph.DRIZZLE to "毛毛雨"
        61, 63, 65, 66, 67, 80, 81, 82 -> WeatherGlyph.RAIN to "降雨"
        71, 73, 75, 77, 85, 86 -> WeatherGlyph.SNOW to "降雪"
        95, 96, 99 -> WeatherGlyph.STORM to "雷暴"
        else -> WeatherGlyph.CLOUDY to "天气变化"
    }

    fun qWeather(iconCode: String?, text: String): WeatherGlyph {
        val code = iconCode?.toIntOrNull() ?: 0
        return when {
            code in 302..304 || "雷" in text -> WeatherGlyph.STORM
            code in 300..318 || code in 350..351 || code == 399 ->
                if ("小雨" in text || "毛毛雨" in text) WeatherGlyph.DRIZZLE else WeatherGlyph.RAIN
            code in 400..499 -> WeatherGlyph.SNOW
            code in 500..515 -> WeatherGlyph.MIST
            code in 100..103 || code in 150..153 -> WeatherGlyph.CLEAR
            else -> WeatherGlyph.CLOUDY
        }
    }

    fun aqi(value: Int): Pair<String, String> = when {
        value <= 0 -> "暂无" to "当前数据源未返回空气质量。"
        value <= 50 -> "优" to "空气质量优秀，适合户外活动。"
        value <= 100 -> "良" to "空气质量良好，敏感人群可适当关注。"
        value <= 150 -> "轻度污染" to "敏感人群建议减少长时间户外活动。"
        value <= 200 -> "中度污染" to "建议减少户外运动并做好防护。"
        else -> "重度污染" to "建议尽量停留室内并减少开窗。"
    }
}
