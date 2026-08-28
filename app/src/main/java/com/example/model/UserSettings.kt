package com.example.model

import com.example.ui.theme.AppThemeMode

data class UserSettings(
  val assistantName: String = "Gamak",
  val persona: AssistantPersona = AssistantPersona.GAMAK,
  val customPersonaName: String = "",
  val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  val hapticFeedbackEnabled: Boolean = true,
  val soundEffectsEnabled: Boolean = true,
  val fastResponseMode: Boolean = true,
  val voiceSensitivity: Float = 0.8f,
  val speechRate: Float = 1.0f,
  val memoryEnabled: Boolean = true,
  val wakeWordEnabled: Boolean = true,
  val wakeWordSensitivity: Float = 0.65f,
  val voiceLanguage: String = "auto",
  val speechToTextOnDevice: Boolean = true
) {
  val activeDisplayName: String
    get() = when {
      persona == AssistantPersona.CUSTOM && customPersonaName.isNotBlank() -> customPersonaName.trim()
      persona != AssistantPersona.CUSTOM -> persona.defaultName
      else -> assistantName.ifBlank { "Gamak" }
    }

  val effectiveSpeechRate: Float
    get() = if (fastResponseMode) (speechRate * 1.08f).coerceIn(0.7f, 1.8f) else speechRate
}
