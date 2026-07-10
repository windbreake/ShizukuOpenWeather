package app.weather.android.data

import android.net.Uri
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import org.json.JSONObject

internal object JsonHttp {
    fun buildUrl(baseUrl: String, parameters: Map<String, String>): String {
        val builder = Uri.parse(baseUrl).buildUpon()
        parameters.forEach { (key, value) -> builder.appendQueryParameter(key, value) }
        return builder.build().toString()
    }

    fun normalizeBase(value: String): String {
        val trimmed = value.trim().trimEnd('/')
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun get(url: String, headers: Map<String, String> = emptyMap()): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 18_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Encoding", "gzip")
            connection.setRequestProperty("User-Agent", "ShizukuOpenWeather-Android/0.1.0")
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }

            val status = connection.responseCode
            val raw = if (status in 200..299) connection.inputStream else connection.errorStream
            val buffered = BufferedInputStream(raw ?: error("天气服务未返回响应"))
            val stream = if (connection.contentEncoding.equals("gzip", ignoreCase = true)) {
                GZIPInputStream(buffered)
            } else {
                buffered
            }
            val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (status !in 200..299) error("天气服务请求失败：HTTP $status")
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}
