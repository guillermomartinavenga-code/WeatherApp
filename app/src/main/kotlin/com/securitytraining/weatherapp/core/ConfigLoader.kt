package com.securitytraining.weatherapp.core

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * Stage v0.2 of the API-keys-&-secrets security training exercise.
 * See docs/evidencia-practica-es.md (§3.2 of the reference document).
 *
 * INSECURE ON PURPOSE: `config.json` ships inside `app/src/main/assets/`,
 * which Android packages into the APK verbatim (unlike resources under
 * `res/`, assets are not compiled or obfuscated in any way). Anyone who
 * unzips the APK -- no decompiler required -- can read this file directly:
 * `unzip app-debug.apk -d out && cat out/assets/config.json`.
 *
 * The file is also not excluded by `.gitignore`, so the same value is
 * tracked in git history just like the stage v0.1 literal.
 */
class ConfigLoader(private val context: Context) {
    fun load(): AppConfig =
        context.assets.open(CONFIG_FILE_NAME).bufferedReader().use { reader ->
            Json.decodeFromString(AppConfig.serializer(), reader.readText())
        }

    private companion object {
        const val CONFIG_FILE_NAME = "config.json"
    }
}
