package com.securitytraining.weatherapp.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire format returned by OpenWeatherMap's "current weather by city name" endpoint. */
@Serializable
data class CurrentWeatherDto(
    val name: String,
    val main: MainDto,
    val weather: List<WeatherConditionDto>,
)

@Serializable
data class MainDto(
    val temp: Double,
)

@Serializable
data class WeatherConditionDto(
    val description: String,
)
