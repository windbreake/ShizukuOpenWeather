package app.weather.android.model

import org.json.JSONArray
import org.json.JSONObject

object WeatherJson {
    fun encode(summary: WeatherSummary): String = JSONObject().apply {
        put("location", encodeLocation(summary.location))
        put("currentTemp", summary.currentTemp)
        put("highTemp", summary.highTemp)
        put("lowTemp", summary.lowTemp)
        put("feelsLikeTemp", summary.feelsLikeTemp)
        put("humidityPercent", summary.humidityPercent)
        put("precipitationChance", summary.precipitationChance)
        put("windSpeedKph", summary.windSpeedKph)
        put("description", summary.description)
        put("conditionLabel", summary.conditionLabel)
        put("glyph", summary.glyph.name)
        put("iconCode", summary.iconCode)
        put("source", summary.source)
        put("airQualityIndex", summary.airQualityIndex)
        put("airQualityLabel", summary.airQualityLabel)
        put("airQualitySummary", summary.airQualitySummary)
        put("updatedAtEpochMs", summary.updatedAtEpochMs)
        put("alerts", JSONArray().apply {
            summary.alerts.forEach { alert ->
                put(JSONObject().apply {
                    put("id", alert.id)
                    put("title", alert.title)
                    put("severity", alert.severity.name)
                    put("kind", alert.kind.name)
                    put("detail", alert.detail)
                    put("official", alert.official)
                })
            }
        })
        put("hourly", JSONArray().apply {
            summary.hourly.forEach { point ->
                put(JSONObject().apply {
                    put("time", point.time)
                    put("hourLabel", point.hourLabel)
                    put("temperature", point.temperature)
                    put("precipitationChance", point.precipitationChance)
                    put("windSpeedKph", point.windSpeedKph)
                    put("glyph", point.glyph.name)
                    put("iconCode", point.iconCode)
                    put("conditionLabel", point.conditionLabel)
                })
            }
        })
        put("daily", JSONArray().apply {
            summary.daily.forEach { point ->
                put(JSONObject().apply {
                    put("date", point.date)
                    put("dayLabel", point.dayLabel)
                    put("dateLabel", point.dateLabel)
                    put("highTemp", point.highTemp)
                    put("lowTemp", point.lowTemp)
                    put("precipitationChance", point.precipitationChance)
                    put("windSpeedKph", point.windSpeedKph)
                    put("glyph", point.glyph.name)
                    put("iconCode", point.iconCode)
                    put("conditionLabel", point.conditionLabel)
                })
            }
        })
    }.toString()

    fun decode(payload: String, cached: Boolean): WeatherSummary {
        val root = JSONObject(payload)
        val location = decodeLocation(root.getJSONObject("location"))
        val alerts = root.optJSONArray("alerts").mapObjects { item ->
            WeatherAlert(
                id = item.optString("id"),
                title = item.optString("title"),
                severity = item.enumValue("severity", AlertSeverity.INFO),
                kind = item.enumValue("kind", AlertKind.RAIN),
                detail = item.optString("detail"),
                official = item.optBoolean("official"),
            )
        }
        val hourly = root.optJSONArray("hourly").mapObjects { item ->
            HourlyForecast(
                time = item.optString("time"),
                hourLabel = item.optString("hourLabel"),
                temperature = item.optDouble("temperature"),
                precipitationChance = item.optInt("precipitationChance"),
                windSpeedKph = item.optInt("windSpeedKph"),
                glyph = item.enumValue("glyph", WeatherGlyph.CLOUDY),
                iconCode = item.optNullableString("iconCode"),
                conditionLabel = item.optString("conditionLabel"),
            )
        }
        val daily = root.optJSONArray("daily").mapObjects { item ->
            DailyForecast(
                date = item.optString("date"),
                dayLabel = item.optString("dayLabel"),
                dateLabel = item.optString("dateLabel"),
                highTemp = item.optDouble("highTemp"),
                lowTemp = item.optDouble("lowTemp"),
                precipitationChance = item.optInt("precipitationChance"),
                windSpeedKph = item.optInt("windSpeedKph"),
                glyph = item.enumValue("glyph", WeatherGlyph.CLOUDY),
                iconCode = item.optNullableString("iconCode"),
                conditionLabel = item.optString("conditionLabel"),
            )
        }

        return WeatherSummary(
            location = location,
            currentTemp = root.optDouble("currentTemp"),
            highTemp = root.optDouble("highTemp"),
            lowTemp = root.optDouble("lowTemp"),
            feelsLikeTemp = root.optDouble("feelsLikeTemp"),
            humidityPercent = root.optInt("humidityPercent"),
            precipitationChance = root.optInt("precipitationChance"),
            windSpeedKph = root.optInt("windSpeedKph"),
            description = root.optString("description"),
            conditionLabel = root.optString("conditionLabel"),
            glyph = root.enumValue("glyph", WeatherGlyph.CLOUDY),
            iconCode = root.optNullableString("iconCode"),
            source = root.optString("source"),
            cached = cached,
            airQualityIndex = root.optInt("airQualityIndex"),
            airQualityLabel = root.optString("airQualityLabel"),
            airQualitySummary = root.optString("airQualitySummary"),
            updatedAtEpochMs = root.optLong("updatedAtEpochMs"),
            alerts = alerts,
            hourly = hourly,
            daily = daily,
        )
    }

    fun encodeLocation(location: LocationResult): JSONObject = JSONObject().apply {
        put("key", location.key)
        put("label", location.label)
        put("subtitle", location.subtitle)
        put("latitude", location.latitude)
        put("longitude", location.longitude)
        put("adcode", location.adcode)
    }

    fun decodeLocation(root: JSONObject): LocationResult = LocationResult(
        key = root.getString("key"),
        label = root.getString("label"),
        subtitle = root.optString("subtitle"),
        latitude = root.getDouble("latitude"),
        longitude = root.getDouble("longitude"),
        adcode = root.optNullableString("adcode"),
    )
}

private inline fun <reified T : Enum<T>> JSONObject.enumValue(key: String, fallback: T): T =
    runCatching { enumValueOf<T>(optString(key)) }.getOrDefault(fallback)

private fun JSONObject.optNullableString(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(transform(it)) }
        }
    }
}
