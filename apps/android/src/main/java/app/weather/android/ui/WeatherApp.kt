package app.weather.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.weather.android.AppTab
import app.weather.android.WeatherViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ShizukuWeatherApp(viewModel: WeatherViewModel = viewModel()) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var navigationVisible by remember { mutableStateOf(true) }
    var revealJob by remember { mutableStateOf<Job?>(null) }
    val onScrollStateChange: (Boolean) -> Unit = { scrolling ->
        revealJob?.cancel()
        if (scrolling) {
            navigationVisible = false
        } else {
            revealJob = scope.launch {
                delay(650)
                navigationVisible = true
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state.tab) {
                AppTab.WEATHER -> WeatherDashboard(
                    state = state,
                    onRefresh = viewModel::refresh,
                    onOpenLocations = { viewModel.navigate(AppTab.LOCATIONS) },
                    onOpenSettings = { viewModel.navigate(AppTab.SETTINGS) },
                    onScrollStateChange = onScrollStateChange,
                )
                AppTab.LOCATIONS -> LocationsScreen(
                    state = state,
                    onQueryChange = viewModel::updateSearchQuery,
                    onSelect = viewModel::selectLocation,
                    onAdd = viewModel::addLocation,
                    onRemove = viewModel::removeLocation,
                    onBack = { viewModel.navigate(AppTab.WEATHER) },
                    onScrollStateChange = onScrollStateChange,
                )
                AppTab.SETTINGS -> SettingsScreen(
                    settings = state.settings,
                    onSettingsChange = viewModel::updateSettings,
                    onSave = viewModel::saveSettings,
                    onBack = { viewModel.navigate(AppTab.WEATHER) },
                    onScrollStateChange = onScrollStateChange,
                )
            }
            FloatingGlassNavigation(
                selected = state.tab,
                visible = navigationVisible,
                onSelect = viewModel::navigate,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            )
        }
    }
}
