package com.example.di

import android.content.Context
import com.example.ai.AiClient
import com.example.ai.GeminiAiClient
import com.example.data.AssistantEngine
import com.example.data.SettingsRepository
import com.example.memory.MemoryRepository
import com.example.planner.ActionExecutor
import com.example.planner.Planner
import com.example.tools.ToolRegistry
import com.example.voice.AssistantAudioManager
import com.example.voice.SpeechToTextManager
import com.example.voice.TextToSpeechManager
import com.example.voice.wakeword.KeywordSpotterWakeWordEngine
import com.example.voice.wakeword.WakeWordEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
  val settingsRepository: SettingsRepository
  val memoryRepository: MemoryRepository
  val assistantEngine: AssistantEngine
  val speechToTextManager: SpeechToTextManager
  val textToSpeechManager: TextToSpeechManager
  val assistantAudioManager: AssistantAudioManager
  val wakeWordEngine: WakeWordEngine
  val planner: Planner
}

class DefaultAppContainer(private val context: Context) : AppContainer {
  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override val settingsRepository: SettingsRepository by lazy {
    SettingsRepository(context)
  }

  override val memoryRepository: MemoryRepository by lazy {
    MemoryRepository(context)
  }

  val toolRegistry: ToolRegistry by lazy {
    ToolRegistry()
  }

  val aiClient: AiClient by lazy {
    GeminiAiClient(toolRegistry)
  }

  val actionExecutor: ActionExecutor by lazy {
    ActionExecutor(context = context)
  }

  override val planner: Planner by lazy {
    Planner(aiClient, toolRegistry, actionExecutor)
  }

  override val speechToTextManager: SpeechToTextManager by lazy {
    SpeechToTextManager(context)
  }

  override val textToSpeechManager: TextToSpeechManager by lazy {
    TextToSpeechManager(context)
  }

  override val assistantAudioManager: AssistantAudioManager by lazy {
    AssistantAudioManager(context)
  }

  override val wakeWordEngine: WakeWordEngine by lazy {
    KeywordSpotterWakeWordEngine(context, applicationScope)
  }

  override val assistantEngine: AssistantEngine by lazy {
    AssistantEngine(
      scope = applicationScope,
      speechToTextManager = speechToTextManager,
      textToSpeechManager = textToSpeechManager,
      planner = planner,
      memoryRepository = memoryRepository,
      wakeWordEngine = wakeWordEngine,
      assistantAudioManager = assistantAudioManager
    )
  }
}
