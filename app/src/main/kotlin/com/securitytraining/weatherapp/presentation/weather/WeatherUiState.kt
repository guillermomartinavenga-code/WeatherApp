package com.securitytraining.weatherapp.presentation.weather

import com.securitytraining.weatherapp.domain.model.CurrentWeather

sealed interface WeatherUiState {
    data object Idle : WeatherUiState
    data object Loading : WeatherUiState
    data class Success(val weather: CurrentWeather) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}
