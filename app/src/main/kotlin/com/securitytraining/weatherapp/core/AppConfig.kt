package com.securitytraining.weatherapp.core

import kotlinx.serialization.Serializable

/**
 * Stage v0.2 of the API-keys-&-secrets security training exercise.
 * See docs/evidencia-practica-es.md (§3.2 of the reference document).
 *
 * Shape of `config.json`, a plaintext configuration file bundled as a raw
 * APK asset (see [ConfigLoader]). Replaces the stage v0.1 hardcoded
 * `Constants.OPEN_WEATHER_API_KEY` literal.
 */
@Serializable
data class AppConfig(
    val openWeatherApiKey: String,
    val baseUrl: String,
)
