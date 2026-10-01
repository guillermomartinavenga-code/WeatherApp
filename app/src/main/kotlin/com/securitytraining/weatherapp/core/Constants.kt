package com.securitytraining.weatherapp.core

/**
 * Stage v0.1 of the API-keys-&-secrets security training exercise.
 * See docs/evidencia-practica-es.md and the reference document
 * `api-keys-y-secretos-en-el-codigo-v0.4.md` (§3.1, §8.1).
 *
 * INSECURE ON PURPOSE: the OpenWeatherMap API key is hardcoded as a string
 * literal directly in source code. This is the exact antipattern described
 * in §3.1 — anyone with read access to this repository (or its git history,
 * even after the line is later removed) can recover this value.
 *
 * The value below is NOT a real key. It is a fake, format-plausible
 * placeholder (OpenWeatherMap keys are 32-character hex strings), used only
 * to demonstrate the antipattern — including whether it triggers GitHub's
 * pattern-based secret scanning once this is pushed to a public repository.
 */
object Constants {
    const val OPEN_WEATHER_API_KEY = "b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b"
    const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
}
