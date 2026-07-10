package app.weather.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.weather.android.data.WeatherRepository
import app.weather.android.model.AppSettings
import app.weather.android.model.LocationResult
import app.weather.android.model.WeatherSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    WEATHER,
    LOCATIONS,
    SETTINGS,
}

data class WeatherUiState(
    val tab: AppTab = AppTab.WEATHER,
    val settings: AppSettings = AppSettings(),
    val savedLocations: List<LocationResult> = emptyList(),
    val selectedLocation: LocationResult = WeatherRepository.DEFAULT_LOCATION,
    val weather: WeatherSummary? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val errorMessage: String? = null,
    val locationQuery: String = "",
    val searchResults: List<LocationResult> = emptyList(),
    val searchLoading: Boolean = false,
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository(application)
    private val mutableState = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = mutableState.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = repository.settings()
            val locations = repository.savedLocations()
            val selected = locations.firstOrNull() ?: WeatherRepository.DEFAULT_LOCATION
            mutableState.update {
                it.copy(
                    settings = settings,
                    savedLocations = locations,
                    selectedLocation = selected,
                )
            }
            loadWeather(force = false)
        }
    }

    fun navigate(tab: AppTab) {
        mutableState.update { it.copy(tab = tab, errorMessage = null) }
    }

    fun selectLocation(location: LocationResult) {
        mutableState.update {
            it.copy(
                selectedLocation = location,
                tab = AppTab.WEATHER,
                locationQuery = "",
                searchResults = emptyList(),
            )
        }
        viewModelScope.launch { loadWeather(force = false) }
    }

    fun refresh() {
        viewModelScope.launch { loadWeather(force = true) }
    }

    fun updateSearchQuery(query: String) {
        mutableState.update { it.copy(locationQuery = query) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            mutableState.update { it.copy(searchResults = emptyList(), searchLoading = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350)
            mutableState.update { it.copy(searchLoading = true, errorMessage = null) }
            runCatching { repository.search(query) }
                .onSuccess { results ->
                    mutableState.update { it.copy(searchResults = results, searchLoading = false) }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            searchResults = emptyList(),
                            searchLoading = false,
                            errorMessage = error.userMessage("地点搜索失败"),
                        )
                    }
                }
        }
    }

    fun addLocation(location: LocationResult) {
        viewModelScope.launch {
            repository.saveLocation(location)
            val locations = repository.savedLocations()
            mutableState.update { it.copy(savedLocations = locations) }
            selectLocation(location)
        }
    }

    fun removeLocation(location: LocationResult) {
        viewModelScope.launch {
            if (mutableState.value.savedLocations.size <= 1) {
                mutableState.update { it.copy(errorMessage = "至少保留一个地点") }
                return@launch
            }
            repository.removeLocation(location.key)
            val locations = repository.savedLocations()
            val selected = if (mutableState.value.selectedLocation.key == location.key) {
                locations.first()
            } else {
                mutableState.value.selectedLocation
            }
            mutableState.update { it.copy(savedLocations = locations, selectedLocation = selected) }
            if (selected.key != location.key) loadWeather(force = false)
        }
    }

    fun updateSettings(settings: AppSettings) {
        mutableState.update { it.copy(settings = settings) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            repository.saveSettings(mutableState.value.settings)
            mutableState.update { it.copy(errorMessage = null, tab = AppTab.WEATHER) }
            loadWeather(force = true)
        }
    }

    fun dismissError() {
        mutableState.update { it.copy(errorMessage = null) }
    }

    private suspend fun loadWeather(force: Boolean) {
        mutableState.update {
            it.copy(
                loading = it.weather == null,
                refreshing = it.weather != null,
                errorMessage = null,
            )
        }
        val location = mutableState.value.selectedLocation
        runCatching { repository.weather(location, force) }
            .onSuccess { weather ->
                mutableState.update {
                    it.copy(
                        weather = weather,
                        loading = false,
                        refreshing = false,
                        errorMessage = null,
                    )
                }
            }
            .onFailure { error ->
                mutableState.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        errorMessage = error.userMessage("天气同步失败"),
                    )
                }
            }
    }
}

private fun Throwable.userMessage(fallback: String): String =
    message?.takeIf { it.isNotBlank() } ?: fallback
