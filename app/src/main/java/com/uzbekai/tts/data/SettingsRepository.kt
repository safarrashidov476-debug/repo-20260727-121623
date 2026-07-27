package com.uzbekai.tts.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.uzbekai.tts.Config
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val speechRate: Float = Config.DEFAULT_LENGTH_SCALE,
    val voiceVariation: Float = Config.DEFAULT_NOISE_SCALE,
    val themeMode: String = "system" // "system" | "light" | "dark"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val VOICE_VARIATION = floatPreferencesKey("voice_variation")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            speechRate = prefs[Keys.SPEECH_RATE] ?: Config.DEFAULT_LENGTH_SCALE,
            voiceVariation = prefs[Keys.VOICE_VARIATION] ?: Config.DEFAULT_NOISE_SCALE,
            themeMode = prefs[Keys.THEME_MODE] ?: "system"
        )
    }

    suspend fun setSpeechRate(value: Float) {
        context.dataStore.edit { it[Keys.SPEECH_RATE] = value }
    }

    suspend fun setVoiceVariation(value: Float) {
        context.dataStore.edit { it[Keys.VOICE_VARIATION] = value }
    }

    suspend fun setThemeMode(value: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = value }
    }
}
