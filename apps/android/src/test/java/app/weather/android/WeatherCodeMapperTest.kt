package app.weather.android

import app.weather.android.model.ApiSettings
import app.weather.android.model.LocationResult
import app.weather.android.model.ProviderMode
import app.weather.android.model.WeatherCodeMapper
import app.weather.android.model.WeatherGlyph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

    @Test
    fun cacheIdentityIncludesCoordinates() {
        val first = LocationResult("shared", "地点 A", "", 31.2, 121.5)
        val second = LocationResult("shared", "地点 B", "", 41.8, 123.4)

        assertNotEquals(first.cacheIdentity, second.cacheIdentity)
    }

    @Test
    fun weatherCacheIdentityChangesWithApiConfiguration() {
        val openMeteo = ApiSettings()
        val qWeather = ApiSettings(
            providerMode = ProviderMode.QWEATHER,
            qWeatherHost = "weather.example.com",
            qWeatherApiKey = "secret-one",
        )
        val rotatedKey = qWeather.copy(qWeatherApiKey = "secret-two")

        assertNotEquals(openMeteo.weatherCacheIdentity, qWeather.weatherCacheIdentity)
        assertNotEquals(qWeather.weatherCacheIdentity, rotatedKey.weatherCacheIdentity)
        assertFalse(qWeather.weatherCacheIdentity.contains("secret-one"))
    }
}
