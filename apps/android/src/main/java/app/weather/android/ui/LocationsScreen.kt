package app.weather.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.weather.android.WeatherUiState
import app.weather.android.model.LocationResult

import kotlinx.coroutines.flow.distinctUntilChanged
@Composable
internal fun LocationsScreen(
    state: WeatherUiState,
    onQueryChange: (String) -> Unit,
    onSelect: (LocationResult) -> Unit,
    onAdd: (LocationResult) -> Unit,
    onRemove: (LocationResult) -> Unit,
    onBack: () -> Unit,
    onScrollStateChange: (Boolean) -> Unit,
) {
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
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                }
                Text(
                    "我的地点",
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
            Text(
                "搜索可精确到区县，海外城市同样支持",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            OutlinedTextField(
                value = state.locationQuery,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索城市、区县或地区") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(12.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
        }

        if (state.locationQuery.trim().length >= 2) {
            item {
                Text(
                    "搜索结果",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (!state.searchLoading && state.searchResults.isEmpty()) {
                item {
                    Text(
                        "暂未找到匹配地区",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.searchResults, key = { it.key }) { location ->
                LocationRow(
                    location = location,
                    selected = false,
                    action = {
                        IconButton(onClick = { onAdd(location) }) {
                            Icon(Icons.Rounded.Add, contentDescription = "添加地点")
                        }
                    },
                    onClick = { onAdd(location) },
                )
            }
        } else {
            item {
                Text(
                    "已保存",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(state.savedLocations, key = { it.key }) { location ->
                LocationRow(
                    location = location,
                    selected = state.selectedLocation.key == location.key,
                    action = {
                        IconButton(onClick = { onRemove(location) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除地点")
                        }
                    },
                    onClick = { onSelect(location) },
                )
            }
        }
    }
}

@Composable
private fun LocationRow(
    location: LocationResult,
    selected: Boolean,
    action: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(location.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    location.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            action()
        }
    }
}
