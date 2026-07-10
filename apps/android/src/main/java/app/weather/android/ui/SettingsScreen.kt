package app.weather.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.weather.android.model.AppSettings
import app.weather.android.model.ProviderMode
import kotlin.math.roundToInt

import kotlinx.coroutines.flow.distinctUntilChanged
@Composable
internal fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onScrollStateChange: (Boolean) -> Unit,
) {
    val api = settings.api
    val appearance = settings.appearance

    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { onScrollStateChange(it) }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 110.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
                Text(
                    "设置",
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
            Text(
                "数据源、显示内容与个性化",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }

        item {
            WeatherCard(opacity = 1f) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SectionHeader("气象数据来源", "默认免费使用 Open-Meteo")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProviderMode.entries.forEach { mode ->
                            FilterChip(
                                selected = api.providerMode == mode,
                                onClick = {
                                    onSettingsChange(
                                        settings.copy(
                                            api = api.copy(
                                                providerMode = mode,
                                                providerName = if (mode == ProviderMode.QWEATHER) {
                                                    "和风天气"
                                                } else {
                                                    "Open-Meteo"
                                                },
                                            ),
                                        ),
                                    )
                                },
                                label = {
                                    Text(if (mode == ProviderMode.QWEATHER) "和风天气" else "Open-Meteo")
                                },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = api.providerName,
                        onValueChange = {
                            onSettingsChange(settings.copy(api = api.copy(providerName = it)))
                        },
                        label = { Text("数据源显示名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    if (api.providerMode == ProviderMode.QWEATHER) {
                        OutlinedTextField(
                            value = api.qWeatherHost,
                            onValueChange = {
                                onSettingsChange(settings.copy(api = api.copy(qWeatherHost = it)))
                            },
                            label = { Text("和风天气 API Host") },
                            placeholder = { Text("https://your-host.re.qweatherapi.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = api.qWeatherApiKey,
                            onValueChange = {
                                onSettingsChange(settings.copy(api = api.copy(qWeatherApiKey = it)))
                            },
                            label = { Text("和风天气 API Key") },
                            supportingText = { Text("仅加密保存在本机，不会写入仓库") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }

                    OutlinedTextField(
                        value = api.amapApiKey,
                        onValueChange = {
                            onSettingsChange(settings.copy(api = api.copy(amapApiKey = it)))
                        },
                        label = { Text("高德 Web API Key（可选）") },
                        supportingText = { Text("留空时使用免费的 Open-Meteo 全球地点搜索") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        }

        item {
            WeatherCard(opacity = 1f) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionHeader("外观与卡片", "延续桌面端的磨砂天气风格")
                    SettingSwitch(
                        title = "磨砂卡片",
                        subtitle = "使用半透明天气卡片",
                        checked = appearance.frostEnabled,
                        onCheckedChange = {
                            onSettingsChange(
                                settings.copy(
                                    appearance = appearance.copy(frostEnabled = it),
                                ),
                            )
                        },
                    )
                    Text(
                        "卡片不透明度 ${(appearance.cardOpacity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = appearance.cardOpacity,
                        onValueChange = {
                            onSettingsChange(
                                settings.copy(
                                    appearance = appearance.copy(cardOpacity = it),
                                ),
                            )
                        },
                        valueRange = 0.45f..1f,
                    )
                    SettingSwitch(
                        title = "界面动效",
                        subtitle = "保留轻量点击与刷新动画",
                        checked = appearance.animationsEnabled,
                        onCheckedChange = {
                            onSettingsChange(
                                settings.copy(
                                    appearance = appearance.copy(animationsEnabled = it),
                                ),
                            )
                        },
                    )
                    SettingSwitch(
                        title = "文字对比增强",
                        subtitle = "自定义背景下加深遮罩，提高文字可读性",
                        checked = appearance.adaptiveText,
                        onCheckedChange = {
                            onSettingsChange(
                                settings.copy(
                                    appearance = appearance.copy(adaptiveText = it),
                                ),
                            )
                        },
                    )
                    OutlinedTextField(
                        value = appearance.backgroundImageUrl,
                        onValueChange = {
                            onSettingsChange(
                                settings.copy(
                                    appearance = appearance.copy(backgroundImageUrl = it),
                                ),
                            )
                        },
                        label = { Text("自定义背景图片 URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        }

        item {
            WeatherCard(opacity = 1f) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SectionHeader("首页内容", "按需控制天气卡片")
                    SettingSwitch(
                        "天气提醒",
                        "显示官方预警和风险提示",
                        appearance.showAlerts,
                    ) {
                        onSettingsChange(
                            settings.copy(appearance = appearance.copy(showAlerts = it)),
                        )
                    }
                    SettingSwitch(
                        "逐时天气",
                        "显示未来 12 小时",
                        appearance.showHourly,
                    ) {
                        onSettingsChange(
                            settings.copy(appearance = appearance.copy(showHourly = it)),
                        )
                    }
                    SettingSwitch(
                        "空气质量",
                        "显示 AQI 与健康建议",
                        appearance.showAirQuality,
                    ) {
                        onSettingsChange(
                            settings.copy(appearance = appearance.copy(showAirQuality = it)),
                        )
                    }
                    SettingSwitch(
                        "未来一周",
                        "显示 7 日天气趋势",
                        appearance.showDaily,
                    ) {
                        onSettingsChange(
                            settings.copy(appearance = appearance.copy(showDaily = it)),
                        )
                    }
                }
            }
        }

        item {
            WeatherCard(opacity = 1f) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SectionHeader("同步", "SQLite 缓存刷新策略")
                    Text(
                        "自动刷新间隔 ${settings.refreshIntervalHours} 小时",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Slider(
                        value = settings.refreshIntervalHours.toFloat(),
                        onValueChange = {
                            onSettingsChange(
                                settings.copy(refreshIntervalHours = it.roundToInt()),
                            )
                        },
                        valueRange = 1f..12f,
                        steps = 10,
                    )
                }
            }
        }

        item {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("保存设置并刷新")
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
