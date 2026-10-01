package com.securitytraining.weatherapp.domain.model

/** Domain-level representation of the current weather for a city. */
data class CurrentWeather(
    val cityName: String,
    val temperatureCelsius: Double,
    val description: String,
)
