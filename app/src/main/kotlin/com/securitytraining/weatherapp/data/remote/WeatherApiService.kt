package com.securitytraining.weatherapp.data.remote

import com.securitytraining.weatherapp.BuildConfig
import com.securitytraining.weatherapp.data.local.InstallTokenStore
import com.securitytraining.weatherapp.data.remote.dto.CurrentWeatherDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter

/** Thin wrapper around the Ktor [HttpClient] for OpenWeatherMap endpoints. */
class WeatherApiService(
    private val httpClient: HttpClient,
    private val installTokenStore: InstallTokenStore,
) {
    suspend fun fetchCurrentWeather(city: String): CurrentWeatherDto =
        httpClient.get("${BASE_URL}weather") {
            parameter("q", city)
            // INSECURE (stage v0.4): BuildConfig keeps the key out of version
            // control and out of the APK's assets, but it is still inlined as a
            // plain string constant in the compiled bytecode. See the apktool
            // evidence in docs/evidencia-practica-es.md.
            parameter("appid", BuildConfig.OPEN_WEATHER_API_KEY)
            parameter("units", "metric")
            // Stage v0.7: unrelated runtime secret (not the provider key) stored
            // via EncryptedSharedPreferences/Keystore -- see InstallTokenStore.
            header("X-Install-Token", installTokenStore.getOrCreateToken())
        }.body()

    private companion object {
        const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    }
}
