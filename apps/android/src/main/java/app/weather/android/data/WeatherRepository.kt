package app.weather.android.data

import android.content.Context
import app.weather.android.model.AppSettings
import app.weather.android.model.LocationResult
import app.weather.android.model.WeatherJson
import app.weather.android.model.WeatherSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository(context: Context) {
    private val database = WeatherDatabase(context)
    private val settingsStore = SecureSettingsStore(context)
    private val api = WeatherApiClient()

    suspend fun settings(): AppSettings = withContext(Dispatchers.IO) {
        settingsStore.load()
    }

    suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        settingsStore.save(settings)
    }

    suspend fun savedLocations(): List<LocationResult> = withContext(Dispatchers.IO) {
        val existing = database.savedLocations()
        if (existing.isNotEmpty()) return@withContext existing

        database.saveLocation(DEFAULT_LOCATION)
        listOf(DEFAULT_LOCATION)
    }

    suspend fun saveLocation(location: LocationResult) = withContext(Dispatchers.IO) {
        database.saveLocation(location)
    }

    suspend fun removeLocation(locationKey: String) = withContext(Dispatchers.IO) {
        database.removeLocation(locationKey)
    }

    suspend fun search(query: String): List<LocationResult> {
        val settings = settingsStore.load()
        return api.search(query.trim(), settings.api)
    }

    suspend fun weather(location: LocationResult, force: Boolean = false): WeatherSummary {
        val settings = settingsStore.load()
        val cacheKey = "${location.cacheIdentity}|${settings.api.weatherCacheIdentity}"
        val cache = withContext(Dispatchers.IO) {
            database.getCache(cacheKey)
        }
        val maxAge = settings.refreshIntervalHours * 60L * 60L * 1000L
        val now = System.currentTimeMillis()

        if (!force && cache != null && now - cache.updatedAt <= maxAge) {
            return WeatherJson.decode(cache.payload, cached = true)
        }

        return try {
            val summary = api.weather(location, settings.api)
            withContext(Dispatchers.IO) {
                database.putCache(
                    cacheKey,
                    WeatherJson.encode(summary),
                    summary.updatedAtEpochMs,
                )
            }
            summary
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (cache != null) WeatherJson.decode(cache.payload, cached = true)
            else throw error
        }
    }

    companion object {
        val DEFAULT_LOCATION = LocationResult(
            key = "default-shenyang",
            label = "沈阳",
            subtitle = "中国 · 辽宁省",
            latitude = 41.8057,
            longitude = 123.4315,
            adcode = "210100",
        )
    }
}
