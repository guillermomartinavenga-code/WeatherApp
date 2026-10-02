package com.securitytraining.weatherapp.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

// Stage v0.7: EncryptedSharedPreferences/Keystore storage is NOT a provider
// API key control -- the OpenWeatherMap key handling (BuildConfig,
// local.properties) is unrelated and unaffected by this class. This exists
// to exercise the control the reference doc actually recommends it for: a
// runtime secret generated on-device, here a random anonymous install
// token sent as a header on every request.
class InstallTokenStore(context: Context) {

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCES_FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getOrCreateToken(): String =
        preferences.getString(TOKEN_KEY, null)
            ?: UUID.randomUUID().toString().also { token ->
                preferences.edit().putString(TOKEN_KEY, token).apply()
            }

    private companion object {
        const val PREFERENCES_FILE_NAME = "install_token_store"
        const val TOKEN_KEY = "install_token"
    }
}
