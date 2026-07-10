package app.weather.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.weather.android.model.DailyForecast
import app.weather.android.model.HourlyForecast
import app.weather.android.model.WeatherAlert
import app.weather.android.model.WeatherSummary
import kotlin.math.roundToInt

@Composable
internal fun DetailsCard(weather: WeatherSummary, opacity: Float) {
    WeatherCard(opacity = opacity) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionHeader("详情", "当前天气")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Metric("最高 / 最低", "${weather.highTemp.roundToInt()}° / ${weather.lowTemp.roundToInt()}°", Icons.Rounded.DeviceThermostat)
                Metric("体感温度", "${weather.feelsLikeTemp.roundToInt()}°", Icons.Rounded.DeviceThermostat)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Metric("降雨概率", "${weather.precipitationChance}%", Icons.Rounded.Umbrella)
                Metric("风速", "${weather.windSpeedKph} km/h", Icons.Rounded.Air)
            }
            Text(
                "湿度 ${weather.humidityPercent}% · ${weather.conditionLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun AlertsCard(alerts: List<WeatherAlert>, opacity: Float) {
    WeatherCard(opacity = opacity) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader("天气提醒", "正式预警与风险提示")
            alerts.forEachIndexed { index, alert ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        alert.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (alert.official) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                    )
                    Text(
                        alert.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (alert.official) "官方预警" else "基于预报数据的风险提示",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (index != alerts.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
internal fun HourlyCard(points: List<HourlyForecast>, opacity: Float) {
    WeatherCard(opacity = opacity) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                SectionHeader("逐时天气", "未来 12 小时")
            }
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(points, key = { it.time }) { point ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.width(52.dp),
                    ) {
                        Text(point.hourLabel, style = MaterialTheme.typography.labelLarge)
                        WeatherGlyphIcon(
                            glyph = point.glyph,
                            contentDescription = point.conditionLabel,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            "${point.temperature.roundToInt()}°",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "${point.precipitationChance}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AirQualityCard(weather: WeatherSummary, opacity: Float) {
    val progress = (weather.airQualityIndex / 300f).coerceIn(0f, 1f)
    WeatherCard(opacity = opacity) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader("空气质量", "AQI")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        weather.airQualityLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        color = aqiColor(weather.airQualityIndex),
                    )
                    Text(
                        weather.airQualitySummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(18.dp))
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(86.dp),
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(82.dp),
                        color = aqiColor(weather.airQualityIndex),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 9.dp,
                    )
                    Text(
                        weather.airQualityIndex.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
internal fun DailyCard(points: List<DailyForecast>, opacity: Float) {
    WeatherCard(opacity = opacity) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SectionHeader("未来一周", "7 日天气趋势")
            Spacer(Modifier.height(8.dp))
            points.forEachIndexed { index, point ->
                val precipitationLabel = point.precipitationChance?.let {
                    "降雨 $it%"
                } ?: point.precipitationAmountMm?.let {
                    "降水 ${(it * 10).roundToInt() / 10.0} mm"
                } ?: "暂无降水数据"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.width(64.dp)) {
                        Text(point.dayLabel, style = MaterialTheme.typography.titleMedium)
                        Text(
                            point.dateLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    WeatherGlyphIcon(
                        glyph = point.glyph,
                        contentDescription = point.conditionLabel,
                        modifier = Modifier.size(30.dp),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp),
                    ) {
                        Text(point.conditionLabel, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "$precipitationLabel · 风速 ${point.windSpeedKph} km/h",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${point.highTemp.roundToInt()}° / ${point.lowTemp.roundToInt()}°",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (index != points.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
internal fun EnvironmentRow(weather: WeatherSummary, opacity: Float) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WeatherCard(
            opacity = opacity,
            modifier = Modifier.weight(0.4f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("AQI", style = MaterialTheme.typography.titleLarge)
                CircularProgressIndicator(
                    progress = { (weather.airQualityIndex / 300f).coerceIn(0f, 1f) },
                    modifier = Modifier.size(62.dp),
                    color = aqiColor(weather.airQualityIndex),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 7.dp,
                )
                Text(
                    "${weather.airQualityIndex} · ${weather.airQualityLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    color = aqiColor(weather.airQualityIndex),
                )
            }
        }
        WeatherCard(
            opacity = opacity,
            modifier = Modifier
                .weight(0.6f)
                .clickable {
                    uriHandler.openUri(
                        "https://www.windy.com/?${weather.location.latitude},${weather.location.longitude},8",
                    )
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                SectionHeader("气象雷达", "免费图层")
                WeatherGlyphIcon(
                    glyph = weather.glyph,
                    contentDescription = null,
                    modifier = Modifier.size(58.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    "查看降水与风场",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun aqiColor(aqi: Int): Color = when {
    aqi <= 50 -> Color(0xFF2E8B57)
    aqi <= 100 -> Color(0xFF8B7D20)
    aqi <= 150 -> Color(0xFFB56A24)
    else -> Color(0xFFB13A3A)
}
