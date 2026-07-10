package app.weather.android.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import app.weather.android.model.LocationResult

class WeatherDatabase(context: Context) :
    SQLiteOpenHelper(context, "shizuku_weather_android.db", null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE weather_cache (
                location_key TEXT PRIMARY KEY NOT NULL,
                payload TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE saved_locations (
                location_key TEXT PRIMARY KEY NOT NULL,
                label TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                adcode TEXT,
                position INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun getCache(locationKey: String): CacheEntry? {
        readableDatabase.query(
            "weather_cache",
            arrayOf("payload", "updated_at"),
            "location_key = ?",
            arrayOf(locationKey),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return CacheEntry(
                payload = cursor.getString(0),
                updatedAt = cursor.getLong(1),
            )
        }
    }

    fun putCache(locationKey: String, payload: String, updatedAt: Long) {
        val values = ContentValues().apply {
            put("location_key", locationKey)
            put("payload", payload)
            put("updated_at", updatedAt)
        }
        writableDatabase.insertWithOnConflict(
            "weather_cache",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun savedLocations(): List<LocationResult> {
        readableDatabase.query(
            "saved_locations",
            arrayOf("location_key", "label", "subtitle", "latitude", "longitude", "adcode"),
            null,
            null,
            null,
            null,
            "position ASC",
        ).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) {
                    add(
                        LocationResult(
                            key = cursor.getString(0),
                            label = cursor.getString(1),
                            subtitle = cursor.getString(2),
                            latitude = cursor.getDouble(3),
                            longitude = cursor.getDouble(4),
                            adcode = cursor.getString(5),
                        ),
                    )
                }
            }
        }
    }

    fun saveLocation(location: LocationResult) {
        val nextPosition = readableDatabase.rawQuery(
            "SELECT COALESCE(MAX(position), -1) + 1 FROM saved_locations",
            null,
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
        val values = ContentValues().apply {
            put("location_key", location.key)
            put("label", location.label)
            put("subtitle", location.subtitle)
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("adcode", location.adcode)
            put("position", nextPosition)
        }
        writableDatabase.insertWithOnConflict(
            "saved_locations",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE,
        )
    }

    fun removeLocation(locationKey: String) {
        writableDatabase.delete(
            "saved_locations",
            "location_key = ?",
            arrayOf(locationKey),
        )
    }

    data class CacheEntry(
        val payload: String,
        val updatedAt: Long,
    )

    private companion object {
        const val DATABASE_VERSION = 1
    }
}
