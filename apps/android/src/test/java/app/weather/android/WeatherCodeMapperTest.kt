package app.weather.android

import app.weather.android.model.WeatherCodeMapper
import app.weather.android.model.WeatherGlyph
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeMapperTest {
    @Test
    fun mapsOpenMeteoThunderstorm() {
        assertEquals(WeatherGlyph.STORM, WeatherCodeMapper.openMeteo(95).first)
    }

    @Test
    fun mapsQWeatherThunderstormBeforeGenericRain() {
        assertEquals(WeatherGlyph.STORM, WeatherCodeMapper.qWeather("302", "雷阵雨"))
    }

    @Test
    fun mapsQWeatherLightRainToDrizzle() {
        assertEquals(WeatherGlyph.DRIZZLE, WeatherCodeMapper.qWeather("305", "小雨"))
    }

    @Test
    fun mapsHealthyAirQuality() {
        assertEquals("优", WeatherCodeMapper.aqi(38).first)
    }
}
