package app.weather.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.weather.android.WeatherUiState
import app.weather.android.model.WeatherGlyph
import app.weather.android.model.WeatherSummary
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

@Composable
internal fun WeatherDashboard(
    state: WeatherUiState,
    onRefresh: () -> Unit,
    onOpenLocations: () -> Unit,
    onScrollStateChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { onScrollStateChange(it) }
    }
    val backgroundUrl = state.settings.appearance.backgroundImageUrl
    Box(modifier = Modifier.fillMaxSize()) {
        if (backgroundUrl.isNotBlank()) {
            AsyncImage(
                model = backgroundUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)),
            )
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.weather == null -> EmptyWeatherState(onRefresh)
            else -> {
                val weather = state.weather
                val cardOpacity = if (state.settings.appearance.frostEnabled) {
                    state.settings.appearance.cardOpacity
                } else 1f
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 14.dp,
                        top = 14.dp,
                        end = 14.dp,
                        bottom = 110.dp,
                    ),
                ) {
                    item {
                        WeatherHero(
                            weather = weather,
                            onOpenLocations = onOpenLocations,
                            onOpenSettings = onOpenSettings,
                        )
                    }
                    item {
                        DetailsCard(
                            weather = weather,
                            opacity = cardOpacity,
                        )
                    }
                    if (state.settings.appearance.showAlerts && weather.alerts.isNotEmpty()) {
                        item {
                            AlertsCard(
                                alerts = weather.alerts,
                                opacity = cardOpacity,
                            )
                        }
                    }
                    if (state.settings.appearance.showHourly) {
                        item {
                            HourlyCard(
                                points = weather.hourly,
                                opacity = cardOpacity,
                            )
                        }
                    }
                    if (state.settings.appearance.showAirQuality) {
                        item {
                            EnvironmentRow(
                                weather = weather,
                                opacity = cardOpacity,
                            )
                        }
                    }
                    if (state.settings.appearance.showDaily) {
                        item {
                            DailyCard(
                                points = weather.daily,
                                opacity = cardOpacity,
                            )
                        }
                    }
                    item {
                        Text(
                            text = "数据来源：${weather.source}${if (weather.cached) " · 本地缓存" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherHero(
    weather: WeatherSummary,
    onOpenLocations: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val heroColor = weather.glyph.heroColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(244.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(heroColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        weather.location.label,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Text(
                        weather.location.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.84f),
                    )
                }
                Row {
                    IconButton(onClick = onOpenLocations) {
                        Icon(Icons.Rounded.Search, contentDescription = "搜索地点", tint = Color.White)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "打开设置", tint = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        "${weather.currentTemp.roundToInt()}°",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                    )
                    Text(
                        weather.description,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Text(
                        "最高 ${weather.highTemp.roundToInt()}° · 最低 ${weather.lowTemp.roundToInt()}°",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                }
                if (weather.qWeatherIconUrl != null) {
                    AsyncImage(
                        model = weather.qWeatherIconUrl,
                        contentDescription = weather.description,
                        modifier = Modifier.size(82.dp),
                    )
                } else {
                    WeatherGlyphIcon(
                        glyph = weather.glyph,
                        contentDescription = weather.description,
                        modifier = Modifier.size(76.dp),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyWeatherState(onRefresh: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        IconButton(onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, contentDescription = "重试")
        }
    }
}

private fun WeatherGlyph.heroColor(): Color = when (this) {
    WeatherGlyph.CLEAR -> Color(0xFF2D7770)
    WeatherGlyph.CLOUDY -> Color(0xFF586D79)
    WeatherGlyph.RAIN -> Color(0xFF315D79)
    WeatherGlyph.DRIZZLE -> Color(0xFF4C7180)
    WeatherGlyph.STORM -> Color(0xFF554B72)
    WeatherGlyph.SNOW -> Color(0xFF557A87)
    WeatherGlyph.MIST -> Color(0xFF66786F)
}
