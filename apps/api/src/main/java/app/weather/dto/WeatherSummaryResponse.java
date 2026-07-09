package app.weather.dto;

import java.util.List;

public record WeatherSummaryResponse(
    String locationName,
    String regionName,
    double latitude,
    double longitude,
    double currentTemp,
    double highTemp,
    double lowTemp,
    double feelsLikeTemp,
    int humidityPercent,
    int precipitationChance,
    int windSpeedKph,
    String description,
    String conditionLabel,
    String source,
    boolean cached,
    int airQualityIndex,
    String airQualityLabel,
    String airQualitySummary,
    String updatedAt,
    List<HourlyForecastPoint> hourly,
    List<DailyForecastPoint> daily
) {}

record HourlyForecastPoint(
    String hourLabel,
    double temperature,
    int precipitationChance,
    int windSpeedKph,
    String icon
) {}

record DailyForecastPoint(
    String dayLabel,
    String dateLabel,
    double highTemp,
    double lowTemp,
    int precipitationChance,
    String icon
) {}
