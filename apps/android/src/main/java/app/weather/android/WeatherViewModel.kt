package app.weather.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.weather.android.data.WeatherRepository
import app.weather.android.model.AppSettings
import app.weather.android.model.LocationResult
import app.weather.android.model.WeatherSummary
import kotlinx.coroutines.CancellationException
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
    val locating: Boolean = false,
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository(application)
    private val mutableState = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = mutableState.asStateFlow()
    private var searchJob: Job? = null
    private var weatherJob: Job? = null

    init {
        viewModelScope.launch {
            try {
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
                requestWeather(force = false)
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        loading = false,
                        errorMessage = error.userMessage("应用初始化失败"),
                    )
                }
            }
        }
    }

    fun navigate(tab: AppTab) {
        mutableState.update { it.copy(tab = tab, errorMessage = null) }
    }

    fun selectLocation(location: LocationResult) {
        mutableState.update {
            val locationChanged =
                it.selectedLocation.cacheIdentity != location.cacheIdentity
            it.copy(
                selectedLocation = location,
                weather = if (locationChanged) null else it.weather,
                tab = AppTab.WEATHER,
                locationQuery = "",
                searchResults = emptyList(),
            )
        }
        requestWeather(force = false)
    }

    fun refresh() {
        requestWeather(force = true)
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            mutableState.update { it.copy(locating = true, errorMessage = null) }
            try {
                val location = repository.currentLocation()
                repository.saveLocation(location)
                val locations = repository.savedLocations()
                mutableState.update {
                    it.copy(
                        savedLocations = locations,
                        selectedLocation = location,
                        weather = null,
                        loading = true,
                        locating = false,
                        tab = AppTab.WEATHER,
                        locationQuery = "",
                        searchResults = emptyList(),
                    )
                }
                requestWeather(force = true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        locating = false,
                        errorMessage = error.userMessage("当前位置获取失败"),
                    )
                }
            }
        }
    }

    fun locationPermissionDenied() {
        mutableState.update { it.copy(errorMessage = "需要定位权限才能获取当前位置天气") }
    }

    fun updateSearchQuery(query: String) {
        searchJob?.cancel()
        val requestedQuery = query.trim()
        val shouldSearch = requestedQuery.length >= 2
        mutableState.update {
            it.copy(
                locationQuery = query,
                searchResults = emptyList(),
                searchLoading = shouldSearch,
                errorMessage = null,
            )
        }
        if (!shouldSearch) return

        searchJob = viewModelScope.launch {
            delay(350)
            try {
                val results = repository.search(requestedQuery)
                if (mutableState.value.locationQuery.trim() == requestedQuery) {
                    mutableState.update {
                        it.copy(searchResults = results, searchLoading = false)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (mutableState.value.locationQuery.trim() == requestedQuery) {
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
    }

    fun addLocation(location: LocationResult) {
        viewModelScope.launch {
            try {
                repository.saveLocation(location)
                val locations = repository.savedLocations()
                mutableState.update { it.copy(savedLocations = locations) }
                selectLocation(location)
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(errorMessage = error.userMessage("地点保存失败"))
                }
            }
        }
    }

    fun removeLocation(location: LocationResult) {
        viewModelScope.launch {
            if (mutableState.value.savedLocations.size <= 1) {
                mutableState.update { it.copy(errorMessage = "至少保留一个地点") }
                return@launch
            }
            try {
                val removingSelected =
                    mutableState.value.selectedLocation.key == location.key
                repository.removeLocation(location.key)
                val locations = repository.savedLocations()
                val selected = if (removingSelected) {
                    locations.first()
                } else {
                    mutableState.value.selectedLocation
                }
                mutableState.update {
                    it.copy(
                        savedLocations = locations,
                        selectedLocation = selected,
                        weather = if (removingSelected) null else it.weather,
                    )
                }
                if (removingSelected) requestWeather(force = false)
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(errorMessage = error.userMessage("地点删除失败"))
                }
            }
        }
    }

    fun updateSettings(settings: AppSettings) {
        mutableState.update { it.copy(settings = settings) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            try {
                repository.saveSettings(mutableState.value.settings)
                val persistedSettings = repository.settings()
                mutableState.update {
                    it.copy(
                        settings = persistedSettings,
                        errorMessage = null,
                        tab = AppTab.WEATHER,
                    )
                }
                requestWeather(force = true)
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(errorMessage = error.userMessage("设置保存失败"))
                }
            }
        }
    }

    fun dismissError() {
        mutableState.update { it.copy(errorMessage = null) }
    }

    private fun requestWeather(force: Boolean) {
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch { loadWeather(force) }
    }

    private suspend fun loadWeather(force: Boolean) {
        val location = mutableState.value.selectedLocation
        mutableState.update {
            it.copy(
                loading = it.weather == null,
                refreshing = it.weather != null,
                errorMessage = null,
            )
        }
        try {
            val weather = repository.weather(location, force)
            if (mutableState.value.selectedLocation.cacheIdentity == location.cacheIdentity) {
                mutableState.update {
                    it.copy(
                        weather = weather,
                        loading = false,
                        refreshing = false,
                        errorMessage = null,
                    )
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (mutableState.value.selectedLocation.cacheIdentity == location.cacheIdentity) {
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
}

private fun Throwable.userMessage(fallback: String): String =
    message?.takeIf { it.isNotBlank() } ?: fallback
