package com.example.snapbadgers.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("snapbadgers_prefs", Context.MODE_PRIVATE)

    private val _spotifyKey = mutableStateOf(prefs.getString("spotify_key", "") ?: "")
    val spotifyKey: State<String> = _spotifyKey

    private val _aiHubKey = mutableStateOf(prefs.getString("ai_hub_key", "") ?: "")
    val aiHubKey: State<String> = _aiHubKey

    private val _useHighPrecision = mutableStateOf(prefs.getBoolean("use_high_precision", true))
    val useHighPrecision: State<Boolean> = _useHighPrecision

    fun updateSpotifyKey(key: String) {
        _spotifyKey.value = key
        prefs.edit().putString("spotify_key", key).apply()
    }

    fun updateAiHubKey(key: String) {
        _aiHubKey.value = key
        prefs.edit().putString("ai_hub_key", key).apply()
    }

    fun updateUseHighPrecision(value: Boolean) {
        _useHighPrecision.value = value
        prefs.edit().putBoolean("use_high_precision", value).apply()
    }
}
