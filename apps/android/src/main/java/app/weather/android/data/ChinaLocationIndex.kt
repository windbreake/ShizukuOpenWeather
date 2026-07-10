package app.weather.android.data

import app.weather.android.model.LocationResult
import java.io.Reader
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.json.JSONArray

internal data class ChinaLocationEntry(
    val province: String,
    val city: String,
    val area: String,
    val latitude: Double,
    val longitude: Double,
) {
    val displayName: String
        get() = area.ifBlank {
            city.takeUnless { it in GENERIC_CITY_NAMES }.orEmpty().ifBlank { province }
        }

    companion object {
        private val GENERIC_CITY_NAMES = setOf("市辖区", "县", "省直辖县级行政区划", "自治区直辖县级行政区划")
    }
}

internal object ChinaCoordinateConverter {
    fun bd09ToWgs84(latitude: Double, longitude: Double): Pair<Double, Double> {
        val x = longitude - 0.0065
        val y = latitude - 0.006
        val z = sqrt(x * x + y * y) - 0.00002 * sin(y * X_PI)
        val theta = atan2(y, x) - 0.000003 * cos(x * X_PI)
        val gcjLongitude = z * cos(theta)
        val gcjLatitude = z * sin(theta)
        return gcj02ToWgs84(gcjLatitude, gcjLongitude)
    }

    private fun gcj02ToWgs84(latitude: Double, longitude: Double): Pair<Double, Double> {
        if (outsideChina(latitude, longitude)) return latitude to longitude
        val deltaLatitude = transformLatitude(longitude - 105.0, latitude - 35.0)
        val deltaLongitude = transformLongitude(longitude - 105.0, latitude - 35.0)
        val radians = latitude / 180.0 * PI
        var magic = sin(radians)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        val adjustedLatitude = deltaLatitude * 180.0 / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
        val adjustedLongitude = deltaLongitude * 180.0 / (A / sqrtMagic * cos(radians) * PI)
        return latitude - adjustedLatitude to longitude - adjustedLongitude
    }

    private fun outsideChina(latitude: Double, longitude: Double): Boolean =
        longitude !in 72.004..137.8347 || latitude !in 0.8293..55.8271

    private fun transformLatitude(x: Double, y: Double): Double {
        var result = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(kotlin.math.abs(x))
        result += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        result += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        result += (160.0 * sin(y / 12.0 * PI) + 320 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return result
    }

    private fun transformLongitude(x: Double, y: Double): Double {
        var result = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(kotlin.math.abs(x))
        result += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        result += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        result += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return result
    }

    private const val A = 6378245.0
    private const val EE = 0.006693421622965943
    private const val X_PI = PI * 3000.0 / 180.0
}

internal class ChinaLocationIndex private constructor(
    private val entries: List<ChinaLocationEntry>,
) {
    fun search(rawQuery: String, limit: Int = 12): List<LocationResult> {
        val query = rawQuery.trim()
        if (query.isBlank()) return emptyList()
        val normalizedQuery = normalizeName(query)
        val querySuffix = administrativeSuffix(query)

        return entries.asSequence()
            .mapNotNull { entry ->
                val score = score(entry, query, normalizedQuery, querySuffix)
                if (score <= 0) null else entry to score
            }
            .sortedWith(
                compareByDescending<Pair<ChinaLocationEntry, Int>> { it.second }
                    .thenBy { it.first.province }
                    .thenBy { it.first.city }
                    .thenBy { it.first.area },
            )
            .map { (entry, _) -> entry.toLocationResult() }
            .distinctBy { "${it.label}|${it.subtitle}|${it.latitude}|${it.longitude}" }
            .take(limit)
            .toList()
    }

    private fun score(
        entry: ChinaLocationEntry,
        query: String,
        normalizedQuery: String,
        querySuffix: String?,
    ): Int {
        val displayName = entry.displayName
        val normalizedDisplay = normalizeName(displayName)
        val normalizedCity = normalizeName(entry.city)
        val normalizedProvince = normalizeName(entry.province)

        var score = when {
            displayName == query -> 220
            entry.city == query -> 210
            entry.province == query && entry.city.isBlank() && entry.area.isBlank() -> 205
            normalizedDisplay == normalizedQuery -> 185
            normalizedCity == normalizedQuery -> 175
            normalizedProvince == normalizedQuery -> 120
            displayName.startsWith(query) -> 145
            entry.city.startsWith(query) -> 140
            normalizedDisplay.startsWith(normalizedQuery) -> 130
            normalizedCity.startsWith(normalizedQuery) -> 125
            displayName.contains(query) -> 95
            entry.city.contains(query) -> 90
            entry.province.contains(query) -> 65
            else -> 0
        }
        if (score == 0) return 0

        if (querySuffix != null) {
            if (displayName == query || entry.city == query) score += 35
            if (displayName.endsWith(querySuffix)) score += 20
        }
        if (entry.area.isBlank() && entry.city !in GENERIC_CITY_NAMES) score += 8
        return score
    }

    private fun ChinaLocationEntry.toLocationResult(): LocationResult {
        val name = displayName
        val subtitle = listOf(
            city.takeUnless { it in GENERIC_CITY_NAMES || it == name }.orEmpty(),
            province.takeUnless { it == name }.orEmpty(),
            "中国",
        ).filter { it.isNotBlank() }.distinct().joinToString(" · ")
        return LocationResult(
            key = "cg-${latitude.toBits().toString(16)}-${longitude.toBits().toString(16)}",
            label = name,
            subtitle = subtitle,
            latitude = latitude,
            longitude = longitude,
        )
    }

    companion object {
        fun fromJson(reader: Reader): ChinaLocationIndex {
            val array = JSONArray(reader.readText())
            val entries = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val latitude = item.optString("lat").toDoubleOrNull() ?: continue
                    val longitude = item.optString("lng").toDoubleOrNull() ?: continue
                    if (!latitude.isFinite() || !longitude.isFinite()) continue
                    val (wgsLatitude, wgsLongitude) = ChinaCoordinateConverter.bd09ToWgs84(latitude, longitude)
                    add(
                        ChinaLocationEntry(
                            province = item.optString("province").trim(),
                            city = item.optString("city").trim(),
                            area = item.optString("area").trim(),
                            latitude = wgsLatitude,
                            longitude = wgsLongitude,
                        ),
                    )
                }
            }
            return ChinaLocationIndex(entries)
        }

        fun fromEntries(entries: List<ChinaLocationEntry>): ChinaLocationIndex =
            ChinaLocationIndex(entries)

        private fun normalizeName(value: String): String {
            var normalized = value.trim().lowercase(Locale.ROOT).replace(" ", "")
            while (true) {
                val suffix = ADMINISTRATIVE_SUFFIXES.firstOrNull { normalized.endsWith(it) }
                    ?: break
                val candidate = normalized.dropLast(suffix.length)
                if (candidate.length < 2) break
                normalized = candidate
            }
            return normalized
        }

        private fun administrativeSuffix(value: String): String? =
            ADMINISTRATIVE_SUFFIXES.firstOrNull { value.trim().endsWith(it) }

        private val GENERIC_CITY_NAMES = setOf("市辖区", "县", "省直辖县级行政区划", "自治区直辖县级行政区划")

        private val ADMINISTRATIVE_SUFFIXES = listOf(
            "特别行政区",
            "自治区",
            "自治州",
            "自治县",
            "自治旗",
            "市辖区",
            "开发区",
            "新区",
            "地区",
            "林区",
            "省",
            "市",
            "县",
            "区",
            "州",
            "盟",
            "旗",
        )
    }
}
