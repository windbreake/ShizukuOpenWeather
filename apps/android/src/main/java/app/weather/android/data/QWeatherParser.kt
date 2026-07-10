package app.weather.android.data

import app.weather.android.model.AlertKind
import app.weather.android.model.AlertSeverity
import app.weather.android.model.DailyForecast
import app.weather.android.model.HourlyForecast
import app.weather.android.model.WeatherAlert
import app.weather.android.model.WeatherCodeMapper
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

internal object QWeatherParser {
    fun hourly(root: JSONObject): List<HourlyForecast> {
        val items = root.optJSONArray("hourly") ?: JSONArray()
        return (0 until minOf(12, items.length())).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val text = item.optString("text", "天气变化")
            val iconCode = item.optString("icon").takeIf { it.isNotBlank() }
            val time = item.optString("fxTime")
            HourlyForecast(
                time = time,
                hourLabel = runCatching {
                    OffsetDateTime.parse(time).format(DateTimeFormatter.ofPattern("HH:mm"))
                }.getOrDefault(time.substringAfter('T').take(5)),
                temperature = item.optDouble("temp"),
                precipitationChance = item.optInt("pop"),
                windSpeedKph = item.optDouble("windSpeed").toInt(),
                glyph = WeatherCodeMapper.qWeather(iconCode, text),
                iconCode = iconCode,
                conditionLabel = text,
            )
        }
    }

    fun daily(root: JSONObject): List<DailyForecast> {
        val items = root.optJSONArray("daily") ?: JSONArray()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val date = item.optString("fxDate")
            val localDate = runCatching { LocalDate.parse(date) }
                .getOrDefault(LocalDate.now().plusDays(index.toLong()))
            val dayText = item.optString("textDay", "天气变化")
            val nightText = item.optString("textNight")
            val iconCode = item.optString("iconDay").takeIf { it.isNotBlank() }
            DailyForecast(
                date = date,
                dayLabel = if (index == 0) "今天" else localDate.dayOfWeek.zhLabel(),
                dateLabel = localDate.format(DateTimeFormatter.ofPattern("MM/dd")),
                highTemp = item.optDouble("tempMax"),
                lowTemp = item.optDouble("tempMin"),
                precipitationChance = null,
                precipitationAmountMm = item.optDouble("precip", Double.NaN)
                    .takeIf { it.isFinite() },
                windSpeedKph = item.optDouble("windSpeedDay").toInt(),
                glyph = WeatherCodeMapper.qWeather(iconCode, dayText),
                iconCode = iconCode,
                conditionLabel = listOf(dayText, nightText)
                    .filter { it.isNotBlank() }
                    .joinToString(" / "),
            )
        }
    }

    fun warnings(root: JSONObject?): List<WeatherAlert> {
        val items = root?.optJSONArray("warning") ?: return emptyList()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val title = item.optString("title")
                .ifBlank { item.optString("typeName", "天气预警") }
            WeatherAlert(
                id = item.optString("id").ifBlank { "warning-$index" },
                title = title,
                severity = AlertSeverity.WARNING,
                kind = if ("风" in title) AlertKind.WIND else AlertKind.RAIN,
                detail = item.optString("text").ifBlank { title },
                official = true,
            )
        }
    }
}
