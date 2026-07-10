package app.weather.android.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import app.weather.android.model.LocationResult
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

internal class DeviceLocationSource(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    suspend fun currentLocation(): LocationResult {
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        require(fineGranted || coarseGranted) { "需要定位权限才能获取当前位置天气" }

        val providers = buildList {
            if (fineGranted) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        require(providers.isNotEmpty()) { "请开启系统定位服务，或授予精确定位权限" }

        val fallback = bestLastKnownLocation(providers)
        val fresh = withTimeoutOrNull(15_000) { awaitLocation(providers) }
        val location = fresh ?: fallback ?: error("暂时无法获取当前位置，请到开阔处后重试")
        val providerLabel = when (location.provider) {
            LocationManager.GPS_PROVIDER -> "GNSS 卫星定位"
            LocationManager.NETWORK_PROVIDER -> "网络辅助定位"
            else -> "系统融合定位"
        }
        return LocationResult(
            key = "device-current",
            label = "当前位置",
            subtitle = "$providerLabel · ${"%.4f".format(Locale.US, location.latitude)}, ${"%.4f".format(Locale.US, location.longitude)}",
            latitude = location.latitude,
            longitude = location.longitude,
        )
    }

    @SuppressLint("MissingPermission")
    private fun bestLastKnownLocation(providers: List<String>): Location? =
        providers.mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time <= LAST_LOCATION_MAX_AGE_MS }
            .sortedWith(compareByDescending<Location> { it.time }.thenBy { it.accuracy })
            .firstOrNull()

    @SuppressLint("MissingPermission")
    private suspend fun awaitLocation(providers: List<String>): Location =
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }

                @Deprecated("Deprecated in Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }

            continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
            providers.forEach { provider ->
                locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }
        }

    private companion object {
        const val LAST_LOCATION_MAX_AGE_MS = 6L * 60L * 60L * 1000L
    }
}
