package com.example.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AssistantEngine
import com.example.data.SettingsRepository
import com.example.model.AssistantState
import com.example.model.ChatMessage
import com.example.model.QuickAction
import com.example.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
  val assistantState: AssistantState = AssistantState.IDLE,
  val messages: List<ChatMessage> = emptyList(),
  val audioLevels: List<Float> = emptyList(),
  val userSettings: UserSettings = UserSettings(),
  val inputText: String = "",
  val quickActions: List<QuickAction> = QuickAction.defaultActions
)

class MainViewModel(
  private val assistantEngine: AssistantEngine,
  private val settingsRepository: SettingsRepository
) : ViewModel() {

  private val _inputText = MutableStateFlow("")
  val inputText: StateFlow<String> = _inputText.asStateFlow()

  val uiState: StateFlow<MainUiState> = combine(
    assistantEngine.state,
    assistantEngine.messages,
    assistantEngine.audioLevels,
    settingsRepository.settingsFlow,
    _inputText
  ) { state, messages, audioLevels, settings, text ->
    MainUiState(
      assistantState = state,
      messages = messages,
      audioLevels = audioLevels,
      userSettings = settings,
      inputText = text
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = MainUiState()
  )

  init {
    viewModelScope.launch {
      settingsRepository.settingsFlow.collect { settings ->
        assistantEngine.initializeWithWelcome(settings.activeDisplayName)
      }
    }
  }

  fun onInputTextChanged(newText: String) {
    _inputText.value = newText
  }

  fun onMicTapped() {
    val activeName = uiState.value.userSettings.activeDisplayName
    val speed = uiState.value.userSettings.effectiveSpeechRate
    assistantEngine.onMicTapped(activeName, ttsSpeed = speed)
  }

  fun onOrbTapped() {
    onMicTapped()
  }

  fun onSendPrompt(prompt: String? = null) {
    val textToSend = prompt ?: _inputText.value
    if (textToSend.isBlank()) return

    val activeName = uiState.value.userSettings.activeDisplayName
    val speed = uiState.value.userSettings.effectiveSpeechRate
    assistantEngine.sendUserPrompt(textToSend.trim(), activeName, ttsSpeed = speed)
    _inputText.value = ""
  }

  fun onQuickActionClicked(action: QuickAction) {
    val activeName = uiState.value.userSettings.activeDisplayName
    val speed = uiState.value.userSettings.effectiveSpeechRate
    assistantEngine.sendUserPrompt(action.prompt, activeName, ttsSpeed = speed, forceState = action.targetState)
  }

  fun onConfirmAction(confirmed: Boolean) {
    val activeName = uiState.value.userSettings.activeDisplayName
    val speed = uiState.value.userSettings.effectiveSpeechRate
    assistantEngine.confirmAction(confirmed, activeName, ttsSpeed = speed)
  }

  fun onOptionSelected(option: com.example.tools.PickerOption) {
    val activeName = uiState.value.userSettings.activeDisplayName
    val speed = uiState.value.userSettings.effectiveSpeechRate
    assistantEngine.onOptionSelected(option, activeName, ttsSpeed = speed)
  }

  fun onClarifyReply(reply: String) {
    val activeName = uiState.value.userSettings.activeDisplayName
    val speed = uiState.value.userSettings.effectiveSpeechRate
    assistantEngine.sendUserPrompt(reply, activeName, ttsSpeed = speed)
  }

  fun onForceStateTest(newState: AssistantState) {
    assistantEngine.setStateExplicitly(newState)
  }

  fun onClearChat() {
    val activeName = uiState.value.userSettings.activeDisplayName
    assistantEngine.clearChat(activeName)
  }

  companion object {
    fun provideFactory(
      assistantEngine: AssistantEngine,
      settingsRepository: SettingsRepository
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(assistantEngine, settingsRepository) as T
      }
    }
  }
}
