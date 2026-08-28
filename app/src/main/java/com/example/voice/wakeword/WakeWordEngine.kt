package com.example.voice.wakeword

import kotlinx.coroutines.flow.StateFlow

interface WakeWordEngine {
  val isRunning: StateFlow<Boolean>
  val status: StateFlow<WakeEngineStatus>

  fun start(
    config: WakeWordConfig,
    onWakeDetected: (WakeWordEvent) -> Unit
  ): WakeEngineResult

  fun stop()

  fun pause(reason: WakeEngineStatus = WakeEngineStatus.PAUSED_FOR_MIC)

  fun resume()

  fun updateSensitivity(sensitivity: Float)

  fun testWakeWordTrigger(onWakeDetected: (WakeWordEvent) -> Unit)
}
