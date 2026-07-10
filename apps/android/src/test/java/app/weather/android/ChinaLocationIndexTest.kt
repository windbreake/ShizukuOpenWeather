package app.weather.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChinaLocationIndexTest {
    private val index = ChinaLocationIndex.fromEntries(
        listOf(
            entry("北京市", "市辖区", "朝阳区", 39.9264, 116.4496),
            entry("辽宁省", "朝阳市", "", 41.5768, 120.4512),
            entry("辽宁省", "营口市", "", 40.6674, 122.2352),
            entry("辽宁省", "丹东市", "", 40.0007, 124.3544),
            entry("辽宁省", "大连市", "瓦房店市", 39.6307, 122.0027),
            entry("辽宁省", "鞍山市", "海城市", 40.8826, 122.6852),
            entry("江苏省", "苏州市", "昆山市", 31.3819, 120.9581),
        ),
    )

    @Test
    fun findsPrefectureCitiesWithOrWithoutAdministrativeSuffix() {
        assertEquals("营口市", index.search("营口").first().label)
        assertEquals("营口市", index.search("营口市").first().label)
        assertEquals("丹东市", index.search("丹东市").first().label)
    }

    @Test
    fun findsCountyLevelCitiesUsingTheSameRules() {
        val queries = listOf("瓦房店", "海城市", "昆山市")
        val expected = listOf("瓦房店市", "海城市", "昆山市")

        assertEquals(expected, queries.map { index.search(it).first().label })
    }

    @Test
    fun suffixAndParentsDisambiguateSameNamePlaces() {
        val prefecture = index.search("朝阳市").first()
        val district = index.search("朝阳区").first()

        assertTrue(prefecture.subtitle.contains("辽宁省"))
        assertTrue(district.subtitle.contains("北京市"))
        assertNotEquals(prefecture.key, district.key)
    }

    @Test
    fun coordinatesAreUsedAsStableLocationIdentity() {
        val result = index.search("营口市").first()

        assertTrue(result.key.startsWith("cg-"))
        assertEquals(40.6674, result.latitude, 0.0001)
        assertEquals(122.2352, result.longitude, 0.0001)
    }

    @Test
    fun convertsBaiduCoordinatesToOpenMeteoWgs84Coordinates() {
        val (latitude, longitude) = ChinaCoordinateConverter.bd09ToWgs84(
            latitude = 40.67313683826707,
            longitude = 122.24157466449694,
        )

        assertEquals(40.665833, latitude, 0.0001)
        assertEquals(122.229828, longitude, 0.0001)
    }

    private fun entry(
        province: String,
        city: String,
        area: String,
        latitude: Double,
        longitude: Double,
    ) = ChinaLocationEntry(
        province = province,
        city = city,
        area = area,
        latitude = latitude,
        longitude = longitude,
    )
}
