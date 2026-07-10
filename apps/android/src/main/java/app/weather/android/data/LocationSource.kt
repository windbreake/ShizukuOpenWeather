package app.weather.android.data

import android.content.Context
import app.weather.android.R
import app.weather.android.model.ApiSettings
import app.weather.android.model.LocationResult
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

internal class LocationSource(context: Context) {
    private val appContext = context.applicationContext
    private val chinaIndex by lazy {
        appContext.resources.openRawResource(R.raw.china_city_geo)
            .bufferedReader(Charsets.UTF_8)
            .use(ChinaLocationIndex::fromJson)
    }

    suspend fun search(query: String, settings: ApiSettings): List<LocationResult> =
        withContext(Dispatchers.IO) {
            val normalizedQuery = query.trim()
            val localResults = chinaIndex.search(normalizedQuery)
            if (localResults.isNotEmpty()) return@withContext localResults
            val remoteResults = if (settings.amapApiKey.isNotBlank()) {
                runCatching { searchAmap(normalizedQuery, settings) }
                    .getOrElse { runCatching { searchOpenMeteo(normalizedQuery, settings) }.getOrDefault(emptyList()) }
            } else {
                runCatching { searchOpenMeteo(normalizedQuery, settings) }.getOrDefault(emptyList())
            }
            mergeResults(normalizedQuery, localResults, remoteResults)
        }

    private fun searchOpenMeteo(query: String, settings: ApiSettings): List<LocationResult> {
        val url = JsonHttp.buildUrl(
            settings.geocodingUrl,
            mapOf(
                "name" to query,
                "count" to "20",
                "language" to "zh",
                "format" to "json",
            ),
        )
        val items = JsonHttp.get(url).optJSONArray("results") ?: JSONArray()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val latitude = item.optDouble("latitude", Double.NaN)
                val longitude = item.optDouble("longitude", Double.NaN)
                if (!latitude.isFinite() || !longitude.isFinite()) continue
                val label = item.optString("name").ifBlank { query }
                val subtitle = listOf(
                    item.optString("admin4"),
                    item.optString("admin3"),
                    item.optString("admin2"),
                    item.optString("admin1"),
                    item.optString("country"),
                ).filter { it.isNotBlank() }.distinct().joinToString(" · ")
                add(
                    LocationResult(
                        key = "om-${item.optString("id", "$latitude,$longitude")}",
                        label = label,
                        subtitle = subtitle,
                        latitude = latitude,
                        longitude = longitude,
                    ),
                )
            }
        }.distinctBy { "${it.label}|${it.subtitle}|${it.latitude}|${it.longitude}" }
    }

    private fun searchAmap(query: String, settings: ApiSettings): List<LocationResult> {
        val results = mutableListOf<LocationResult>()
        val geocodeUrl = JsonHttp.buildUrl(
            "https://restapi.amap.com/v3/geocode/geo",
            mapOf("address" to query, "output" to "JSON", "key" to settings.amapApiKey),
        )
        val geocodes = JsonHttp.get(geocodeUrl).optJSONArray("geocodes") ?: JSONArray()
        for (index in 0 until geocodes.length()) {
            val item = geocodes.optJSONObject(index) ?: continue
            parseAmapLocation(item.optString("location"))?.let { (latitude, longitude) ->
                val label = item.optString("district")
                    .ifBlank { item.optString("city") }
                    .ifBlank { query }
                    .trimEnd('市', '区', '县')
                results += LocationResult(
                    key = item.optString("adcode").ifBlank { "amap-$latitude,$longitude" },
                    label = label,
                    subtitle = listOf(
                        item.optString("province"),
                        item.optString("city"),
                        item.optString("district"),
                    ).filter { it.isNotBlank() }.distinct().joinToString(" · "),
                    latitude = latitude,
                    longitude = longitude,
                    adcode = item.optString("adcode").takeIf { it.isNotBlank() },
                )
            }
        }

        val tipsUrl = JsonHttp.buildUrl(
            settings.amapGeocodingUrl,
            mapOf(
                "keywords" to query,
                "datatype" to "all",
                "citylimit" to "false",
                "output" to "JSON",
                "key" to settings.amapApiKey,
            ),
        )
        val tips = JsonHttp.get(tipsUrl).optJSONArray("tips") ?: JSONArray()
        for (index in 0 until tips.length()) {
            val item = tips.optJSONObject(index) ?: continue
            val coordinates = parseAmapLocation(item.optString("location")) ?: continue
            val label = item.optString("name").ifBlank { query }
            val adcode = item.optString("adcode").takeIf { it.isNotBlank() }
            results += LocationResult(
                key = item.optString("id").ifBlank {
                    adcode ?: "amap-${coordinates.first},${coordinates.second}"
                },
                label = label,
                subtitle = listOf(item.optString("district"), item.optString("address"))
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                latitude = coordinates.first,
                longitude = coordinates.second,
                adcode = adcode,
            )
        }
        return results.distinctBy { "${it.label}|${it.subtitle}|${it.latitude}|${it.longitude}" }
    }

    private fun mergeResults(
        query: String,
        localResults: List<LocationResult>,
        remoteResults: List<LocationResult>,
    ): List<LocationResult> {
        val merged = localResults.toMutableList()
        remoteResults.sortedByDescending { resultMatchScore(query, it) }.forEach { candidate ->
            val duplicate = merged.any { existing ->
                canonicalName(existing.label) == canonicalName(candidate.label) &&
                    abs(existing.latitude - candidate.latitude) < 0.45 &&
                    abs(existing.longitude - candidate.longitude) < 0.45
            }
            if (!duplicate) merged += candidate
        }
        return merged.take(20)
    }

    private fun resultMatchScore(query: String, result: LocationResult): Int {
        val canonicalQuery = canonicalName(query)
        val canonicalLabel = canonicalName(result.label)
        return when {
            result.label == query -> 100
            canonicalLabel == canonicalQuery -> 90
            result.label.startsWith(query) -> 70
            canonicalLabel.startsWith(canonicalQuery) -> 60
            result.subtitle.contains(query) -> 50
            else -> 0
        }
    }

    private fun canonicalName(value: String): String {
        var result = value.trim().lowercase(Locale.ROOT)
        val suffixes = listOf("特别行政区", "自治区", "自治州", "自治县", "自治旗", "地区", "新区", "省", "市", "县", "区", "州", "盟", "旗")
        val suffix = suffixes.firstOrNull { result.endsWith(it) }
        if (suffix != null && result.length - suffix.length >= 2) result = result.dropLast(suffix.length)
        return result
    }

    private fun parseAmapLocation(raw: String): Pair<Double, Double>? {
        val parts = raw.split(',', limit = 2)
        if (parts.size != 2) return null
        val longitude = parts[0].toDoubleOrNull() ?: return null
        val latitude = parts[1].toDoubleOrNull() ?: return null
        return latitude to longitude
    }
}
