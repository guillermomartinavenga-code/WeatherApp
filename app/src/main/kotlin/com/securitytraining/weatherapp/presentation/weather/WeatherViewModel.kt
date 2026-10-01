package com.securitytraining.weatherapp.presentation.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securitytraining.weatherapp.domain.usecase.GetCurrentWeatherUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val getCurrentWeatherUseCase: GetCurrentWeatherUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    fun onSearch(city: String) {
        if (city.isBlank()) return
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            getCurrentWeatherUseCase(city)
                .onSuccess { _uiState.value = WeatherUiState.Success(it) }
                .onFailure { _uiState.value = WeatherUiState.Error(it.message.orEmpty()) }
        }
    }
}
