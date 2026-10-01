package com.securitytraining.weatherapp.data.repository

import com.securitytraining.weatherapp.data.remote.WeatherApiService
import com.securitytraining.weatherapp.data.remote.dto.CurrentWeatherDto
import com.securitytraining.weatherapp.domain.model.CurrentWeather
import com.securitytraining.weatherapp.domain.repository.WeatherRepository

class WeatherRepositoryImpl(
    private val apiService: WeatherApiService,
) : WeatherRepository {
    override suspend fun getCurrentWeather(city: String): Result<CurrentWeather> =
        runCatching { apiService.fetchCurrentWeather(city).toDomain() }

    private fun CurrentWeatherDto.toDomain(): CurrentWeather =
        CurrentWeather(
            cityName = name,
            temperatureCelsius = main.temp,
            description = weather.firstOrNull()?.description.orEmpty(),
        )
}
