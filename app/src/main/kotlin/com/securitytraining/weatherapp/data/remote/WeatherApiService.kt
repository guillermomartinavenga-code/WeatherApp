package com.securitytraining.weatherapp.data.remote

import com.securitytraining.weatherapp.core.AppConfig
import com.securitytraining.weatherapp.data.remote.dto.CurrentWeatherDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** Thin wrapper around the Ktor [HttpClient] for OpenWeatherMap endpoints. */
class WeatherApiService(
    private val httpClient: HttpClient,
    private val config: AppConfig,
) {
    suspend fun fetchCurrentWeather(city: String): CurrentWeatherDto =
        httpClient.get("${config.baseUrl}weather") {
            parameter("q", city)
            // INSECURE (stage v0.2): the API key comes from config.json, a
            // plaintext file bundled as a raw APK asset. See ConfigLoader.kt.
            parameter("appid", config.openWeatherApiKey)
            parameter("units", "metric")
        }.body()
}
