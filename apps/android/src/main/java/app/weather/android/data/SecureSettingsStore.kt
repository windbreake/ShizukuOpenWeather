package app.weather.android.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.weather.android.model.ApiSettings
import app.weather.android.model.AppSettings
import app.weather.android.model.AppearanceSettings
import app.weather.android.model.ProviderMode
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("shizuku_android_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val providerMode = runCatching {
            ProviderMode.valueOf(preferences.getString(KEY_PROVIDER_MODE, ProviderMode.OPEN_METEO.name).orEmpty())
        }.getOrDefault(ProviderMode.OPEN_METEO)

        return AppSettings(
            api = ApiSettings(
                providerMode = providerMode,
                providerName = preferences.getString(KEY_PROVIDER_NAME, defaultProviderName(providerMode))
                    .orEmpty()
                    .ifBlank { defaultProviderName(providerMode) },
                openMeteoWeatherUrl = preferences.getString(KEY_OPEN_METEO_WEATHER, DEFAULT_OPEN_METEO_WEATHER)
                    .orEmpty()
                    .ifBlank { DEFAULT_OPEN_METEO_WEATHER },
                openMeteoAirUrl = preferences.getString(KEY_OPEN_METEO_AIR, DEFAULT_OPEN_METEO_AIR)
                    .orEmpty()
                    .ifBlank { DEFAULT_OPEN_METEO_AIR },
                geocodingUrl = preferences.getString(KEY_GEOCODING_URL, DEFAULT_GEOCODING)
                    .orEmpty()
                    .ifBlank { DEFAULT_GEOCODING },
                qWeatherHost = preferences.getString(KEY_QWEATHER_HOST, "").orEmpty(),
                qWeatherAirHost = preferences.getString(KEY_QWEATHER_AIR_HOST, "").orEmpty(),
                qWeatherApiKey = decrypt(preferences.getString(KEY_QWEATHER_KEY, "").orEmpty()),
                amapGeocodingUrl = preferences.getString(KEY_AMAP_URL, DEFAULT_AMAP).orEmpty().ifBlank { DEFAULT_AMAP },
                amapApiKey = decrypt(preferences.getString(KEY_AMAP_KEY, "").orEmpty()),
            ),
            appearance = AppearanceSettings(
                frostEnabled = preferences.getBoolean(KEY_FROST, true),
                cardOpacity = preferences.getFloat(KEY_OPACITY, 0.88f).coerceIn(0.45f, 1f),
                animationsEnabled = preferences.getBoolean(KEY_ANIMATIONS, true),
                adaptiveText = preferences.getBoolean(KEY_ADAPTIVE_TEXT, true),
                backgroundImageUrl = preferences.getString(KEY_BACKGROUND_URL, "").orEmpty(),
                showAlerts = preferences.getBoolean(KEY_SHOW_ALERTS, true),
                showAirQuality = preferences.getBoolean(KEY_SHOW_AIR, true),
                showHourly = preferences.getBoolean(KEY_SHOW_HOURLY, true),
                showDaily = preferences.getBoolean(KEY_SHOW_DAILY, true),
            ),
            refreshIntervalHours = preferences.getInt(KEY_REFRESH_HOURS, 2).coerceIn(1, 12),
        )
    }

    fun save(settings: AppSettings) {
        preferences.edit()
            .putString(KEY_PROVIDER_MODE, settings.api.providerMode.name)
            .putString(KEY_PROVIDER_NAME, settings.api.providerName.trim())
            .putString(KEY_OPEN_METEO_WEATHER, settings.api.openMeteoWeatherUrl.trim())
            .putString(KEY_OPEN_METEO_AIR, settings.api.openMeteoAirUrl.trim())
            .putString(KEY_GEOCODING_URL, settings.api.geocodingUrl.trim())
            .putString(KEY_QWEATHER_HOST, settings.api.qWeatherHost.trim())
            .putString(KEY_QWEATHER_AIR_HOST, settings.api.qWeatherAirHost.trim())
            .putString(KEY_QWEATHER_KEY, encrypt(settings.api.qWeatherApiKey.trim()))
            .putString(KEY_AMAP_URL, settings.api.amapGeocodingUrl.trim())
            .putString(KEY_AMAP_KEY, encrypt(settings.api.amapApiKey.trim()))
            .putBoolean(KEY_FROST, settings.appearance.frostEnabled)
            .putFloat(KEY_OPACITY, settings.appearance.cardOpacity.coerceIn(0.45f, 1f))
            .putBoolean(KEY_ANIMATIONS, settings.appearance.animationsEnabled)
            .putBoolean(KEY_ADAPTIVE_TEXT, settings.appearance.adaptiveText)
            .putString(KEY_BACKGROUND_URL, settings.appearance.backgroundImageUrl.trim())
            .putBoolean(KEY_SHOW_ALERTS, settings.appearance.showAlerts)
            .putBoolean(KEY_SHOW_AIR, settings.appearance.showAirQuality)
            .putBoolean(KEY_SHOW_HOURLY, settings.appearance.showHourly)
            .putBoolean(KEY_SHOW_DAILY, settings.appearance.showDaily)
            .putInt(KEY_REFRESH_HOURS, settings.refreshIntervalHours.coerceIn(1, 12))
            .apply()
    }

    private fun encrypt(value: String): String {
        if (value.isBlank()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        if (value.isBlank() || ':' !in value) return ""
        return runCatching {
            val (iv, encrypted) = value.split(':', limit = 2)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
            )
            String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private fun defaultProviderName(mode: ProviderMode) =
        if (mode == ProviderMode.QWEATHER) "和风天气" else "Open-Meteo"

    private companion object {
        const val KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "shizuku_weather_api_credentials"
        const val TRANSFORMATION = "AES/GCM/NoPadding"

        const val DEFAULT_OPEN_METEO_WEATHER = "https://api.open-meteo.com/v1/forecast"
        const val DEFAULT_OPEN_METEO_AIR = "https://air-quality-api.open-meteo.com/v1/air-quality"
        const val DEFAULT_GEOCODING = "https://geocoding-api.open-meteo.com/v1/search"
        const val DEFAULT_AMAP = "https://restapi.amap.com/v3/assistant/inputtips"

        const val KEY_PROVIDER_MODE = "provider_mode"
        const val KEY_PROVIDER_NAME = "provider_name"
        const val KEY_OPEN_METEO_WEATHER = "open_meteo_weather"
        const val KEY_OPEN_METEO_AIR = "open_meteo_air"
        const val KEY_GEOCODING_URL = "geocoding_url"
        const val KEY_QWEATHER_HOST = "qweather_host"
        const val KEY_QWEATHER_AIR_HOST = "qweather_air_host"
        const val KEY_QWEATHER_KEY = "qweather_key"
        const val KEY_AMAP_URL = "amap_url"
        const val KEY_AMAP_KEY = "amap_key"
        const val KEY_FROST = "frost"
        const val KEY_OPACITY = "opacity"
        const val KEY_ANIMATIONS = "animations"
        const val KEY_ADAPTIVE_TEXT = "adaptive_text"
        const val KEY_BACKGROUND_URL = "background_url"
        const val KEY_SHOW_ALERTS = "show_alerts"
        const val KEY_SHOW_AIR = "show_air"
        const val KEY_SHOW_HOURLY = "show_hourly"
        const val KEY_SHOW_DAILY = "show_daily"
        const val KEY_REFRESH_HOURS = "refresh_hours"
    }
}
