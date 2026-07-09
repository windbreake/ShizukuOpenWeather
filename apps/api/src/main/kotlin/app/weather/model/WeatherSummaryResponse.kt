package app.weather.model

data class WeatherSummaryResponse(
    val locationName: String,
    val regionName: String,
    val latitude: Double,
    val longitude: Double,
    val currentTemp: Double,
    val highTemp: Double,
    val lowTemp: Double,
    val feelsLikeTemp: Double,
    val humidityPercent: Int,
    val precipitationChance: Int,
    val windSpeedKph: Int,
    val description: String,
    val conditionLabel: String,
    val source: String,
    val cached: Boolean,
    val airQualityIndex: Int,
    val airQualityLabel: String,
    val airQualitySummary: String,
    val updatedAt: String,
    val hourly: List<HourlyForecastPoint>,
    val daily: List<DailyForecastPoint>,
)

data class HourlyForecastPoint(
    val hourLabel: String,
    val temperature: Double,
    val precipitationChance: Int,
    val windSpeedKph: Int,
    val icon: String,
)

data class DailyForecastPoint(
    val dayLabel: String,
    val dateLabel: String,
    val highTemp: Double,
    val lowTemp: Double,
    val precipitationChance: Int,
    val icon: String,
)
