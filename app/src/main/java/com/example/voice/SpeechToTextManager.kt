package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale

interface SpeechRecognitionCallback {
  fun onReady()
  fun onBeginningOfSpeech()
  fun onRmsChanged(rmsDb: Float)
  fun onPartialResult(partialText: String)
  fun onFinalResult(finalText: String)
  fun onError(errorCode: Int, errorMessage: String)
  fun onEndOfSpeech()
}

class SpeechToTextManager(private val context: Context) {

  companion object {
    private const val TAG = "SpeechToTextManager"
  }

  private var speechRecognizer: SpeechRecognizer? = null
  private var isCurrentlyListening = false

  private val _isAvailable = MutableStateFlow(SpeechRecognizer.isRecognitionAvailable(context))
  val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

  suspend fun startListening(
    preferredLanguage: String = "hi-IN",
    callback: SpeechRecognitionCallback
  ) = withContext(Dispatchers.Main) {
    if (isCurrentlyListening) {
      stopListening()
    }

    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      callback.onError(SpeechRecognizer.ERROR_CLIENT, "Speech recognition is not available on this device.")
      return@withContext
    }

    try {
      speechRecognizer?.destroy()
      speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(object : RecognitionListener {
          override fun onReadyForSpeech(params: Bundle?) {
            isCurrentlyListening = true
            callback.onReady()
          }

          override fun onBeginningOfSpeech() {
            callback.onBeginningOfSpeech()
          }

          override fun onRmsChanged(rmsdB: Float) {
            callback.onRmsChanged(rmsdB)
          }

          override fun onBufferReceived(buffer: ByteArray?) {}

          override fun onEndOfSpeech() {
            isCurrentlyListening = false
            callback.onEndOfSpeech()
          }

          override fun onError(error: Int) {
            isCurrentlyListening = false
            val msg = getErrorMessage(error)
            Log.w(TAG, "Speech recognition error: $error ($msg)")
            callback.onError(error, msg)
          }

          override fun onResults(results: Bundle?) {
            isCurrentlyListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            callback.onFinalResult(text)
          }

          override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
              callback.onPartialResult(text)
            }
          }

          override fun onEvent(eventType: Int, params: Bundle?) {}
        })
      }

      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, preferredLanguage)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, preferredLanguage)
        // Multi-language recognition fallback
        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "ne-NP", "en-IN", "en-US"))
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
      }

      speechRecognizer?.startListening(intent)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start speech recognition", e)
      isCurrentlyListening = false
      callback.onError(SpeechRecognizer.ERROR_CLIENT, e.localizedMessage ?: "Unknown speech error")
    }
  }

  fun stopListening() {
    try {
      if (isCurrentlyListening) {
        speechRecognizer?.stopListening()
        isCurrentlyListening = false
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error stopping SpeechRecognizer", e)
    }
  }

  fun cancel() {
    try {
      isCurrentlyListening = false
      speechRecognizer?.cancel()
    } catch (e: Exception) {
      Log.w(TAG, "Error cancelling SpeechRecognizer", e)
    }
  }

  fun destroy() {
    try {
      isCurrentlyListening = false
      speechRecognizer?.destroy()
      speechRecognizer = null
    } catch (e: Exception) {
      Log.w(TAG, "Error destroying SpeechRecognizer", e)
    }
  }

  private fun getErrorMessage(errorCode: Int): String {
    return when (errorCode) {
      SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
      SpeechRecognizer.ERROR_CLIENT -> "Client side error"
      SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions for recording"
      SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
      SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
      SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
      SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
      SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
      SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
      else -> "Speech recognition error ($errorCode)"
    }
  }
}
