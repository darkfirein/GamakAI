package com.example.voice.wakeword

enum class WakeEngineStatus(val displayLabel: String) {
  UNINITIALIZED("Uninitialized"),
  LISTENING("Active"),
  PAUSED_FOR_MIC("Paused (Mic Active)"),
  PAUSED_FOR_TTS("Paused (Speaking)"),
  NO_PERMISSION("No Mic Permission"),
  MODEL_UNAVAILABLE("Model Unavailable"),
  STOPPED("Inactive"),
  ERROR("Error")
}

enum class WakeEngineTechnology(val displayName: String, val description: String) {
  ACOUSTIC_HEURISTIC(
    displayName = "Acoustic Signal & Energy Spotter",
    description = "Low-latency on-device RMS energy and Zero-Crossing Rate syllable pattern detector."
  ),
  TRAINED_OFFLINE_MODEL(
    displayName = "Trained Neural Keyword Model",
    description = "Deep learning acoustic phoneme spotter (e.g. Porcupine / ONNX / TFLite)."
  )
}

data class WakeWordEvent(
  val detectedKeyword: String,
  val confidence: Float,
  val rmsEnergy: Float = 0f,
  val timestamp: Long = System.currentTimeMillis()
)

// Legacy alias for compatibility with test assertions
typealias WakeWordDetectionEvent = WakeWordEvent

data class WakeWordConfig(
  val keyword: String = "Gamak",
  val sensitivity: Float = 0.8f,
  val isBackgroundServiceEnabled: Boolean = false,
  val supportedPredefinedKeywords: List<String> = listOf(
    "Gamak",
    "Hey Gamak",
    "Maya",
    "Vikram",
    "Sathi",
    "Mitra",
    "Riya",
    "Jarvis"
  )
) {
  val isPretrainedSupported: Boolean
    get() = supportedPredefinedKeywords.any { it.equals(keyword.trim(), ignoreCase = true) }

  val modelStatusMessage: String
    get() = if (isPretrainedSupported) {
      "Supported offline acoustic model for '$keyword'. Continuous low-power syllable listening active."
    } else {
      "Custom wake word '$keyword' requires a compatible wake-word model. Running in Acoustic Syllable Mode with Push-to-Talk as primary."
    }
}

sealed class WakeEngineResult {
  data object Success : WakeEngineResult()
  data class SupportedWithFallback(val message: String) : WakeEngineResult()
  data class Error(val message: String) : WakeEngineResult()
}

