package app.weather.dto;

public record WeatherSummaryResponse(
    String locationName,
    double latitude,
    double longitude,
    double temperature,
    String description,
    String source,
    boolean cached
) {}
