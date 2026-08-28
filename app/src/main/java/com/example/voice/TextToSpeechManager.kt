package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

  companion object {
    private const val TAG = "TextToSpeechManager"
  }

  private var tts: TextToSpeech? = null

  private val _isReady = MutableStateFlow(false)
  val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

  private var activeUtteranceId: String? = null
  private var onStartCallback: (() -> Unit)? = null
  private var onDoneCallback: (() -> Unit)? = null
  private var onErrorCallback: (() -> Unit)? = null

  init {
    initialize()
  }

  fun initialize() {
    try {
      tts = TextToSpeech(context, this)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize TextToSpeech", e)
    }
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      Log.d(TAG, "TextToSpeech initialized successfully.")
      _isReady.value = true
      setupProgressListener()
    } else {
      Log.e(TAG, "TextToSpeech initialization failed with status $status")
      _isReady.value = false
    }
  }

  private fun setupProgressListener() {
    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(utteranceId: String?) {
        if (utteranceId == activeUtteranceId) {
          onStartCallback?.invoke()
        }
      }

      override fun onDone(utteranceId: String?) {
        if (utteranceId == activeUtteranceId) {
          onDoneCallback?.invoke()
          clearCallbacks()
        }
      }

      @Deprecated("Deprecated in Java")
      override fun onError(utteranceId: String?) {
        if (utteranceId == activeUtteranceId) {
          onErrorCallback?.invoke()
          clearCallbacks()
        }
      }

      override fun onError(utteranceId: String?, errorCode: Int) {
        if (utteranceId == activeUtteranceId) {
          Log.w(TAG, "TTS utterance error: $errorCode")
          onErrorCallback?.invoke()
          clearCallbacks()
        }
      }
    })
  }

  fun speak(
    text: String,
    speechRate: Float = 1.0f,
    speechPitch: Float = 1.0f,
    onStart: () -> Unit = {},
    onDone: () -> Unit = {},
    onError: () -> Unit = {}
  ) {
    if (text.isBlank()) {
      onDone()
      return
    }

    if (tts == null || !_isReady.value) {
      Log.w(TAG, "TextToSpeech is not ready yet. Simulating completion.")
      // Re-try initialization
      if (tts == null) initialize()
      onError()
      return
    }

    try {
      stop()

      val utteranceId = UUID.randomUUID().toString()
      activeUtteranceId = utteranceId
      onStartCallback = onStart
      onDoneCallback = onDone
      onErrorCallback = onError

      // Dynamic locale selection
      val targetLocale = selectBestLocaleForText(text)
      val langResult = tts?.setLanguage(targetLocale)
      if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
        // Fallback to default or English
        tts?.setLanguage(Locale.getDefault())
      }

      tts?.setSpeechRate(speechRate)
      tts?.setPitch(speechPitch)

      val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
      if (result != TextToSpeech.SUCCESS) {
        Log.w(TAG, "TTS speak failed with code $result")
        onError()
        clearCallbacks()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error in TTS speak", e)
      onError()
      clearCallbacks()
    }
  }

  fun stop() {
    try {
      tts?.stop()
      clearCallbacks()
    } catch (e: Exception) {
      Log.w(TAG, "Error stopping TTS", e)
    }
  }

  fun shutdown() {
    try {
      stop()
      tts?.shutdown()
      tts = null
      _isReady.value = false
    } catch (e: Exception) {
      Log.w(TAG, "Error shutting down TTS", e)
    }
  }

  private fun clearCallbacks() {
    activeUtteranceId = null
    onStartCallback = null
    onDoneCallback = null
    onErrorCallback = null
  }

  private fun selectBestLocaleForText(text: String): Locale {
    val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
    return if (hasDevanagari) {
      // Check for Nepali specific words or default Hindi
      if (text.contains("छ") || text.contains("हुनुहुन्छ") || text.contains("गर") || text.contains("बाटो")) {
        Locale.forLanguageTag("ne-NP")
      } else {
        Locale.forLanguageTag("hi-IN")
      }
    } else {
      Locale.forLanguageTag("en-IN")
    }
  }
}
