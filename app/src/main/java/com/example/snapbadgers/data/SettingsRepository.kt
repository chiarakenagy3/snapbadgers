package com.example.snapbadgers.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

/**
 * Settings backed by plaintext [android.content.SharedPreferences].
 *
 * SECURITY: Spotify API keys and user PII are stored unencrypted on disk.
 * For production, migrate to EncryptedSharedPreferences (androidx.security:security-crypto).
 */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("snapbadgers_prefs", Context.MODE_PRIVATE)

    private val _spotifyKey = mutableStateOf(prefs.getString("spotify_key", "") ?: "")
    val spotifyKey: State<String> = _spotifyKey

    private val _displayName = mutableStateOf(prefs.getString("display_name", "SnapBadger User") ?: "SnapBadger User")
    val displayName: State<String> = _displayName

    private val _email = mutableStateOf(prefs.getString("email", "user@example.com") ?: "user@example.com")
    val email: State<String> = _email

    private val _themeMode = mutableStateOf(prefs.getString("theme_mode", "System") ?: "System")
    val themeMode: State<String> = _themeMode

    private val _language = mutableStateOf(prefs.getString("language", "English") ?: "English")
    val language: State<String> = _language

    private val _notificationsEnabled = mutableStateOf(prefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: State<Boolean> = _notificationsEnabled

    private val _personalizationEnabled = mutableStateOf(prefs.getBoolean("personalization_enabled", true))
    val personalizationEnabled: State<Boolean> = _personalizationEnabled

    fun updateSpotifyKey(value: String) {
        _spotifyKey.value = value
        prefs.edit().putString("spotify_key", value).apply()
    }

    fun updateDisplayName(value: String) {
        _displayName.value = value
        prefs.edit().putString("display_name", value).apply()
    }

    fun updateEmail(value: String) {
        _email.value = value
        prefs.edit().putString("email", value).apply()
    }

    fun updateThemeMode(value: String) {
        _themeMode.value = value
        prefs.edit().putString("theme_mode", value).apply()
    }

    fun updateLanguage(value: String) {
        _language.value = value
        prefs.edit().putString("language", value).apply()
    }

    fun updateNotificationsEnabled(value: Boolean) {
        _notificationsEnabled.value = value
        prefs.edit().putBoolean("notifications_enabled", value).apply()
    }

    fun updatePersonalizationEnabled(value: Boolean) {
        _personalizationEnabled.value = value
        prefs.edit().putBoolean("personalization_enabled", value).apply()
    }
}
