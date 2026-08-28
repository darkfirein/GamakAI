package com.example

import com.example.model.AssistantPersona
import com.example.model.UserSettings
import com.example.voice.wakeword.WakeEngineStatus
import com.example.voice.wakeword.WakeWordDetectionEvent
import com.example.voice.wakeword.WakeWordService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5WakeWordTest {

  @Test
  fun testAssistantPersonaActiveNameResolution() {
    val defaultSettings = UserSettings()
    assertEquals("Gamak", defaultSettings.activeDisplayName)

    val mayaSettings = UserSettings(persona = AssistantPersona.MAYA)
    assertEquals("Maya", mayaSettings.activeDisplayName)

    val vikramSettings = UserSettings(persona = AssistantPersona.VIKRAM)
    assertEquals("Vikram", vikramSettings.activeDisplayName)

    val customSettings = UserSettings(
      persona = AssistantPersona.CUSTOM,
      customPersonaName = "Jarvis"
    )
    assertEquals("Jarvis", customSettings.activeDisplayName)

    val blankCustomSettings = UserSettings(
      persona = AssistantPersona.CUSTOM,
      customPersonaName = "   "
    )
    assertEquals("Gamak", blankCustomSettings.activeDisplayName)
  }

  @Test
  fun testWakeWordDetectionEventData() {
    val now = System.currentTimeMillis()
    val event = WakeWordDetectionEvent(
      detectedKeyword = "Gamak",
      confidence = 0.94f,
      rmsEnergy = 0.45f,
      timestamp = now
    )

    assertEquals("Gamak", event.detectedKeyword)
    assertEquals(0.94f, event.confidence, 0.01f)
    assertEquals(0.45f, event.rmsEnergy, 0.01f)
    assertEquals(now, event.timestamp)
  }

  @Test
  fun testWakeEngineStatusValues() {
    assertEquals("Active", WakeEngineStatus.LISTENING.displayLabel)
    assertEquals("Paused (Mic Active)", WakeEngineStatus.PAUSED_FOR_MIC.displayLabel)
    assertEquals("Paused (Speaking)", WakeEngineStatus.PAUSED_FOR_TTS.displayLabel)
    assertEquals("No Mic Permission", WakeEngineStatus.NO_PERMISSION.displayLabel)
    assertEquals("Inactive", WakeEngineStatus.STOPPED.displayLabel)
  }

  @Test
  fun testWakeWordServiceConstants() {
    assertEquals("com.example.ACTION_START_WAKE_WORD", WakeWordService.ACTION_START_LISTENING)
    assertEquals("com.example.ACTION_STOP_WAKE_WORD", WakeWordService.ACTION_STOP_LISTENING)
    assertEquals("com.example.ACTION_WAKE_WORD_TRIGGERED", WakeWordService.ACTION_WAKE_WORD_TRIGGERED)
    assertNotNull(WakeWordService.EXTRA_KEYWORD)
    assertNotNull(WakeWordService.EXTRA_SENSITIVITY)
  }

  @Test
  fun testUserSettingsDefaultValues() {
    val settings = UserSettings()
    assertTrue("Wake word should be enabled by default", settings.wakeWordEnabled)
    assertEquals(0.65f, settings.wakeWordSensitivity, 0.01f)
    assertEquals("auto", settings.voiceLanguage)
    assertTrue(settings.speechToTextOnDevice)
    assertTrue(settings.hapticFeedbackEnabled)
    assertTrue(settings.soundEffectsEnabled)
  }
}
