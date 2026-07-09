package app.weather.controller

import app.weather.model.DailyForecastPoint
import app.weather.model.HourlyForecastPoint
import app.weather.model.WeatherSummaryResponse
import kotlin.math.abs
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/weather")
class WeatherSummaryController {
    @GetMapping("/summary")
    fun summary(
        @RequestParam lat: Double,
        @RequestParam lon: Double,
    ): WeatherSummaryResponse {
        val isShenyang = abs(lat - 41.8057) < 0.01 && abs(lon - 123.4315) < 0.01
        val locationName = if (isShenyang) "沈阳市" else "已选地点"
        val regionName = if (isShenyang) "辽宁省" else "本地天气"

        return WeatherSummaryResponse(
            locationName = locationName,
            regionName = regionName,
            latitude = lat,
            longitude = lon,
            currentTemp = 27.3,
            highTemp = 28.0,
            lowTemp = 20.0,
            feelsLikeTemp = 28.1,
            humidityPercent = 78,
            precipitationChance = 100,
            windSpeedKph = 18,
            description = "阴云密布",
            conditionLabel = "短时阵雨，体感偏凉",
            source = "open-meteo-mock",
            cached = false,
            airQualityIndex = 55,
            airQualityLabel = "适宜",
            airQualitySummary = "空气质量一般，敏感人群建议减少长时间室外停留。",
            updatedAt = "14:38 更新",
            hourly = listOf(
                HourlyForecastPoint(hourLabel = "16", temperature = 28.0, precipitationChance = 88, windSpeedKph = 15, icon = "rain"),
                HourlyForecastPoint(hourLabel = "17", temperature = 28.0, precipitationChance = 92, windSpeedKph = 16, icon = "rain"),
                HourlyForecastPoint(hourLabel = "18", temperature = 27.0, precipitationChance = 90, windSpeedKph = 18, icon = "drizzle"),
                HourlyForecastPoint(hourLabel = "19", temperature = 25.0, precipitationChance = 74, windSpeedKph = 14, icon = "rain"),
                HourlyForecastPoint(hourLabel = "20", temperature = 24.0, precipitationChance = 68, windSpeedKph = 12, icon = "cloudy"),
            ),
            daily = listOf(
                DailyForecastPoint(dayLabel = "周五", dateLabel = "03 七月", highTemp = 29.0, lowTemp = 20.0, precipitationChance = 97, icon = "rain"),
                DailyForecastPoint(dayLabel = "周六", dateLabel = "04 七月", highTemp = 29.0, lowTemp = 21.0, precipitationChance = 97, icon = "rain"),
                DailyForecastPoint(dayLabel = "周日", dateLabel = "05 七月", highTemp = 30.0, lowTemp = 21.0, precipitationChance = 68, icon = "drizzle"),
                DailyForecastPoint(dayLabel = "周一", dateLabel = "06 七月", highTemp = 29.0, lowTemp = 23.0, precipitationChance = 97, icon = "rain"),
                DailyForecastPoint(dayLabel = "周二", dateLabel = "07 七月", highTemp = 25.0, lowTemp = 22.0, precipitationChance = 74, icon = "cloudy"),
                DailyForecastPoint(dayLabel = "周三", dateLabel = "08 七月", highTemp = 28.0, lowTemp = 22.0, precipitationChance = 77, icon = "drizzle"),
                DailyForecastPoint(dayLabel = "周四", dateLabel = "09 七月", highTemp = 28.0, lowTemp = 22.0, precipitationChance = 55, icon = "mist"),
            ),
        )
    }
}
