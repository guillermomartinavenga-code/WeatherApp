package com.securitytraining.weatherapp.domain.usecase

import com.securitytraining.weatherapp.domain.model.CurrentWeather
import com.securitytraining.weatherapp.domain.repository.WeatherRepository

/** Fetches the current weather for a given city name. */
class GetCurrentWeatherUseCase(
    private val repository: WeatherRepository,
) {
    suspend operator fun invoke(city: String): Result<CurrentWeather> =
        repository.getCurrentWeather(city)
}
