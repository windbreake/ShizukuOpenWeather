package app.weather.android.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) viewModel.useCurrentLocation()
        else viewModel.locationPermissionDenied()
    }
    val useCurrentLocation: () -> Unit = {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            viewModel.useCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }
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
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
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
                    onUseCurrentLocation = useCurrentLocation,
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
                animationsEnabled = state.settings.appearance.animationsEnabled,
                onSelect = viewModel::navigate,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            )
        }
    }
}
