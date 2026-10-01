package com.securitytraining.weatherapp.domain.repository

import com.securitytraining.weatherapp.domain.model.CurrentWeather

/** Abstracts how current weather data is obtained from the domain's point of view. */
interface WeatherRepository {
    suspend fun getCurrentWeather(city: String): Result<CurrentWeather>
}
