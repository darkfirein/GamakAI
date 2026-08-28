package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AssistantEngine
import com.example.data.SettingsRepository
import com.example.memory.MemoryRepository
import com.example.model.AssistantPersona
import com.example.model.UserSettings
import com.example.ui.theme.AppThemeMode
import com.example.voice.AssistantAudioManager
import com.example.voice.wakeword.WakeEngineStatus
import com.example.voice.wakeword.WakeWordEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
  val settings: UserSettings = UserSettings(),
  val customNameDraft: String = "",
  val isCustomSelected: Boolean = false,
  val wakeEngineStatus: WakeEngineStatus = WakeEngineStatus.STOPPED,
  val feedbackMessage: String? = null
)

class SettingsViewModel(
  private val settingsRepository: SettingsRepository,
  private val wakeWordEngine: WakeWordEngine? = null,
  private val assistantEngine: AssistantEngine? = null,
  private val memoryRepository: MemoryRepository? = null,
  private val assistantAudioManager: AssistantAudioManager? = null
) : ViewModel() {

  private val _customNameDraft = MutableStateFlow("")
  val customNameDraft: StateFlow<String> = _customNameDraft.asStateFlow()

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  val uiState: StateFlow<SettingsUiState> = combine(
    settingsRepository.settingsFlow,
    _customNameDraft,
    wakeWordEngine?.status ?: MutableStateFlow(WakeEngineStatus.STOPPED),
    _feedbackMessage
  ) { settings, draft, wakeStatus, feedback ->
    SettingsUiState(
      settings = settings,
      customNameDraft = if (draft.isEmpty()) settings.customPersonaName else draft,
      isCustomSelected = settings.persona == AssistantPersona.CUSTOM,
      wakeEngineStatus = wakeStatus,
      feedbackMessage = feedback
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = SettingsUiState()
  )

  fun onPersonaSelected(persona: AssistantPersona) {
    viewModelScope.launch {
      if (persona == AssistantPersona.CUSTOM) {
        val draft = _customNameDraft.value.ifBlank { uiState.value.settings.customPersonaName }
        settingsRepository.updatePersona(persona, draft)
      } else {
        settingsRepository.updatePersona(persona, "")
      }
    }
  }

  fun onCustomNameDraftChanged(newDraft: String) {
    _customNameDraft.value = newDraft
  }

  fun onSaveCustomName() {
    val draft = _customNameDraft.value.trim()
    if (draft.isNotBlank()) {
      viewModelScope.launch {
        settingsRepository.updateCustomName(draft)
        showFeedback("Assistant name updated to '$draft'")
      }
    }
  }

  fun onThemeModeSelected(mode: AppThemeMode) {
    viewModelScope.launch {
      settingsRepository.updateThemeMode(mode)
    }
  }

  fun onWakeWordToggled(enabled: Boolean) {
    viewModelScope.launch {
      settingsRepository.updateWakeWordEnabled(enabled)
      if (enabled) {
        showFeedback("Wake-word voice activation enabled")
      } else {
        showFeedback("Wake-word voice activation disabled")
      }
    }
  }

  fun onWakeWordSensitivityChanged(sensitivity: Float) {
    viewModelScope.launch {
      settingsRepository.updateWakeWordSensitivity(sensitivity)
      wakeWordEngine?.updateSensitivity(sensitivity)
    }
  }

  fun onVoiceLanguageSelected(language: String) {
    viewModelScope.launch {
      settingsRepository.updateVoiceLanguage(language)
      showFeedback("Language updated to $language")
    }
  }

  fun onSpeechToTextOnDeviceToggled(enabled: Boolean) {
    viewModelScope.launch {
      settingsRepository.updateSpeechToTextOnDevice(enabled)
    }
  }

  fun onTestWakeWordTrigger() {
    assistantAudioManager?.triggerWakeHaptic()
    assistantAudioManager?.playActivationChime()
    wakeWordEngine?.testWakeWordTrigger { event ->
      viewModelScope.launch {
        showFeedback("⚡ Wake-word '${event.detectedKeyword}' triggered successfully!")
        assistantEngine?.onWakeWordDetected(uiState.value.settings.activeDisplayName)
      }
    } ?: run {
      showFeedback("⚡ Wake-word trigger test completed (Simulated)")
      assistantEngine?.onWakeWordDetected(uiState.value.settings.activeDisplayName)
    }
  }

  fun onHapticFeedbackToggled(enabled: Boolean) {
    viewModelScope.launch {
      settingsRepository.updateHapticFeedback(enabled)
    }
  }

  fun onSoundEffectsToggled(enabled: Boolean) {
    viewModelScope.launch {
      settingsRepository.updateSoundEffects(enabled)
    }
  }

  fun onFastResponseToggled(enabled: Boolean) {
    viewModelScope.launch {
      settingsRepository.updateFastResponse(enabled)
    }
  }

  fun onMemoryToggled(enabled: Boolean) {
    viewModelScope.launch {
      settingsRepository.updateMemoryEnabled(enabled)
    }
  }

  fun onSensitivityChanged(value: Float) {
    viewModelScope.launch {
      settingsRepository.updateSensitivity(value)
    }
  }

  fun onClearConversationHistory() {
    assistantEngine?.clearChat(uiState.value.settings.activeDisplayName)
    showFeedback("Conversation history cleared")
  }

  fun onClearLongTermMemory() {
    viewModelScope.launch {
      memoryRepository?.clearAllMemories()
      showFeedback("Long-term memory cleared")
    }
  }

  fun onResetDefaults() {
    viewModelScope.launch {
      settingsRepository.resetDefaults()
      _customNameDraft.value = ""
      showFeedback("All settings reset to defaults")
    }
  }

  private fun showFeedback(message: String) {
    viewModelScope.launch {
      _feedbackMessage.value = message
      delay(3000)
      if (_feedbackMessage.value == message) {
        _feedbackMessage.value = null
      }
    }
  }

  companion object {
    fun provideFactory(
      settingsRepository: SettingsRepository,
      wakeWordEngine: WakeWordEngine? = null,
      assistantEngine: AssistantEngine? = null,
      memoryRepository: MemoryRepository? = null,
      assistantAudioManager: AssistantAudioManager? = null
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(
          settingsRepository = settingsRepository,
          wakeWordEngine = wakeWordEngine,
          assistantEngine = assistantEngine,
          memoryRepository = memoryRepository,
          assistantAudioManager = assistantAudioManager
        ) as T
      }
    }
  }
}
