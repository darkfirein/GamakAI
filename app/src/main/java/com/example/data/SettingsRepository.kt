package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AssistantPersona
import com.example.model.UserSettings
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gamak_ai_settings")

class SettingsRepository(private val context: Context) {

  private object Keys {
    val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
    val PERSONA_ENUM = stringPreferencesKey("persona_enum")
    val CUSTOM_PERSONA_NAME = stringPreferencesKey("custom_persona_name")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
    val FAST_RESPONSE = booleanPreferencesKey("fast_response")
    val SENSITIVITY = floatPreferencesKey("voice_sensitivity")
    val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
    val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
    val WAKE_WORD_SENSITIVITY = floatPreferencesKey("wake_word_sensitivity")
    val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
    val STT_ON_DEVICE = booleanPreferencesKey("stt_on_device")
  }

  val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
    val personaStr = prefs[Keys.PERSONA_ENUM] ?: AssistantPersona.GAMAK.name
    val persona = try {
      AssistantPersona.valueOf(personaStr)
    } catch (_: Exception) {
      AssistantPersona.GAMAK
    }

    val themeStr = prefs[Keys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
    val theme = try {
      AppThemeMode.valueOf(themeStr)
    } catch (_: Exception) {
      AppThemeMode.SYSTEM
    }

    UserSettings(
      assistantName = prefs[Keys.ASSISTANT_NAME] ?: "Gamak",
      persona = persona,
      customPersonaName = prefs[Keys.CUSTOM_PERSONA_NAME] ?: "",
      themeMode = theme,
      hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
      soundEffectsEnabled = prefs[Keys.SOUND_EFFECTS] ?: true,
      fastResponseMode = prefs[Keys.FAST_RESPONSE] ?: true,
      voiceSensitivity = prefs[Keys.SENSITIVITY] ?: 0.8f,
      memoryEnabled = prefs[Keys.MEMORY_ENABLED] ?: true,
      wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
      wakeWordSensitivity = prefs[Keys.WAKE_WORD_SENSITIVITY] ?: 0.8f,
      voiceLanguage = prefs[Keys.VOICE_LANGUAGE] ?: "auto",
      speechToTextOnDevice = prefs[Keys.STT_ON_DEVICE] ?: false
    )
  }

  suspend fun updatePersona(persona: AssistantPersona, customName: String = "") {
    context.dataStore.edit { prefs ->
      prefs[Keys.PERSONA_ENUM] = persona.name
      if (persona == AssistantPersona.CUSTOM) {
        prefs[Keys.CUSTOM_PERSONA_NAME] = customName
        prefs[Keys.ASSISTANT_NAME] = customName.ifBlank { "Gamak" }
      } else {
        prefs[Keys.ASSISTANT_NAME] = persona.defaultName
      }
    }
  }

  suspend fun updateCustomName(name: String) {
    context.dataStore.edit { prefs ->
      prefs[Keys.CUSTOM_PERSONA_NAME] = name
      prefs[Keys.ASSISTANT_NAME] = name.ifBlank { "Gamak" }
      prefs[Keys.PERSONA_ENUM] = AssistantPersona.CUSTOM.name
    }
  }

  suspend fun updateThemeMode(mode: AppThemeMode) {
    context.dataStore.edit { prefs ->
      prefs[Keys.THEME_MODE] = mode.name
    }
  }

  suspend fun updateHapticFeedback(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.HAPTIC_FEEDBACK] = enabled
    }
  }

  suspend fun updateSoundEffects(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.SOUND_EFFECTS] = enabled
    }
  }

  suspend fun updateFastResponse(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.FAST_RESPONSE] = enabled
    }
  }

  suspend fun updateSensitivity(value: Float) {
    context.dataStore.edit { prefs ->
      prefs[Keys.SENSITIVITY] = value
    }
  }

  suspend fun updateMemoryEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.MEMORY_ENABLED] = enabled
    }
  }

  suspend fun updateWakeWordEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.WAKE_WORD_ENABLED] = enabled
    }
  }

  suspend fun updateWakeWordSensitivity(sensitivity: Float) {
    context.dataStore.edit { prefs ->
      prefs[Keys.WAKE_WORD_SENSITIVITY] = sensitivity
    }
  }

  suspend fun updateVoiceLanguage(language: String) {
    context.dataStore.edit { prefs ->
      prefs[Keys.VOICE_LANGUAGE] = language
    }
  }

  suspend fun updateSpeechToTextOnDevice(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.STT_ON_DEVICE] = enabled
    }
  }

  suspend fun resetDefaults() {
    context.dataStore.edit { prefs ->
      prefs.clear()
    }
  }
}
