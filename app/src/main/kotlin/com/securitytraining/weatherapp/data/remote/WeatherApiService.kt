package com.securitytraining.weatherapp.data.remote

import com.securitytraining.weatherapp.core.Constants
import com.securitytraining.weatherapp.data.remote.dto.CurrentWeatherDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** Thin wrapper around the Ktor [HttpClient] for OpenWeatherMap endpoints. */
class WeatherApiService(
    private val httpClient: HttpClient,
) {
    suspend fun fetchCurrentWeather(city: String): CurrentWeatherDto =
        httpClient.get("${Constants.BASE_URL}weather") {
            parameter("q", city)
            // INSECURE (stage v0.1): the API key comes from a hardcoded constant.
            // See Constants.kt for the full rationale of this training stage.
            parameter("appid", Constants.OPEN_WEATHER_API_KEY)
            parameter("units", "metric")
        }.body()
}
