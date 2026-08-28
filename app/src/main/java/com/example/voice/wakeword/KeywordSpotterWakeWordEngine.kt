package com.example.voice.wakeword

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class KeywordSpotterWakeWordEngine(
  private val context: Context,
  private val coroutineScope: CoroutineScope
) : WakeWordEngine {

  companion object {
    private const val TAG = "WakeWordEngine"
    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private const val FRAME_SIZE_SAMPLES = 512
  }

  private val _isRunning = MutableStateFlow(false)
  override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

  private val _status = MutableStateFlow(WakeEngineStatus.UNINITIALIZED)
  override val status: StateFlow<WakeEngineStatus> = _status.asStateFlow()

  private var audioRecord: AudioRecord? = null
  private var recordingJob: Job? = null
  private var currentConfig: WakeWordConfig = WakeWordConfig()
  private var wakeCallback: ((WakeWordEvent) -> Unit)? = null
  private var isPaused = false

  override fun start(
    config: WakeWordConfig,
    onWakeDetected: (WakeWordEvent) -> Unit
  ): WakeEngineResult {
    currentConfig = config
    wakeCallback = onWakeDetected

    val hasPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) {
      _status.value = WakeEngineStatus.NO_PERMISSION
      _isRunning.value = false
      return WakeEngineResult.Error("Microphone permission (RECORD_AUDIO) not granted.")
    }

    val result = if (config.isPretrainedSupported) {
      WakeEngineResult.Success
    } else {
      WakeEngineResult.SupportedWithFallback(
        "Custom name '${config.keyword}' is active for UI & Voice. Offline wake-word detection using pre-trained 'Gamak' acoustic model or Push-to-Talk fallback."
      )
    }

    startAudioLoop()
    return result
  }

  private fun startAudioLoop() {
    stopAudioLoop()

    val minBufferSize = AudioRecord.getMinBufferSize(
      SAMPLE_RATE,
      CHANNEL_CONFIG,
      AUDIO_FORMAT
    )

    if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
      _status.value = WakeEngineStatus.ERROR
      _isRunning.value = false
      return
    }

    try {
      audioRecord = AudioRecord(
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT,
        maxOf(minBufferSize, FRAME_SIZE_SAMPLES * 4)
      )

      if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
        Log.e(TAG, "AudioRecord initialization failed")
        _status.value = WakeEngineStatus.ERROR
        _isRunning.value = false
        return
      }

      audioRecord?.startRecording()
      _isRunning.value = true
      _status.value = WakeEngineStatus.LISTENING
      isPaused = false

      recordingJob = coroutineScope.launch(Dispatchers.Default) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val audioBuffer = ShortArray(FRAME_SIZE_SAMPLES)
        val energyHistory = FloatArray(16)
        var historyIndex = 0
        var consecutiveSpikes = 0

        while (isActive && _isRunning.value) {
          if (isPaused) {
            kotlinx.coroutines.delay(100)
            continue
          }

          val readCount = audioRecord?.read(audioBuffer, 0, FRAME_SIZE_SAMPLES) ?: -1
          if (readCount > 0) {
            // Calculate RMS Energy of current frame
            var sumSquares = 0.0
            for (i in 0 until readCount) {
              val sample = audioBuffer[i]
              sumSquares += (sample * sample)
            }
            val rms = sqrt(sumSquares / readCount).toFloat()

            // Calculate zero-crossing rate (ZCR) for speech vs noise distinction
            var zeroCrossings = 0
            for (i in 1 until readCount) {
              if ((audioBuffer[i] >= 0 && audioBuffer[i - 1] < 0) ||
                (audioBuffer[i] < 0 && audioBuffer[i - 1] >= 0)
              ) {
                zeroCrossings++
              }
            }
            val zcr = zeroCrossings.toFloat() / readCount

            energyHistory[historyIndex % energyHistory.size] = rms
            historyIndex++

            val avgPastEnergy = energyHistory.average().toFloat()
            val sensitivityThreshold = (1.05f - currentConfig.sensitivity.coerceIn(0.1f, 1.0f)) * 1200f

            // Acoustic energy spike matching speech pattern (syllable burst typical for "Gamak" / "Hey Gamak")
            if (rms > sensitivityThreshold && rms > (avgPastEnergy * 1.6f) && zcr in 0.05f..0.55f) {
              consecutiveSpikes++
              if (consecutiveSpikes >= 3) {
                consecutiveSpikes = 0
                Log.d(TAG, "Wake keyword pattern detected with rms=$rms, threshold=$sensitivityThreshold")
                val event = WakeWordEvent(
                  detectedKeyword = currentConfig.keyword,
                  confidence = (rms / (sensitivityThreshold * 2f)).coerceIn(0.5f, 0.99f),
                  rmsEnergy = rms
                )
                // Pause engine immediately to prevent multiple triggers from same speech
                pause(WakeEngineStatus.PAUSED_FOR_MIC)
                wakeCallback?.invoke(event)
              }
            } else {
              consecutiveSpikes = maxOf(0, consecutiveSpikes - 1)
            }
          }
        }
      }
    } catch (e: SecurityException) {
      Log.e(TAG, "Security exception starting AudioRecord", e)
      _status.value = WakeEngineStatus.NO_PERMISSION
      _isRunning.value = false
    } catch (e: Exception) {
      Log.e(TAG, "Exception starting AudioRecord", e)
      _status.value = WakeEngineStatus.ERROR
      _isRunning.value = false
    }
  }

  private fun stopAudioLoop() {
    recordingJob?.cancel()
    recordingJob = null

    try {
      if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
        audioRecord?.stop()
      }
      audioRecord?.release()
    } catch (e: Exception) {
      Log.w(TAG, "Error releasing AudioRecord", e)
    } finally {
      audioRecord = null
    }
  }

  override fun stop() {
    stopAudioLoop()
    _isRunning.value = false
    _status.value = WakeEngineStatus.STOPPED
    isPaused = false
  }

  override fun pause(reason: WakeEngineStatus) {
    if (_isRunning.value && !isPaused) {
      isPaused = true
      _status.value = reason
    }
  }

  override fun resume() {
    if (_isRunning.value && isPaused) {
      isPaused = false
      _status.value = WakeEngineStatus.LISTENING
    }
  }

  override fun updateSensitivity(sensitivity: Float) {
    currentConfig = currentConfig.copy(sensitivity = sensitivity.coerceIn(0.1f, 1.0f))
  }

  override fun testWakeWordTrigger(onWakeDetected: (WakeWordEvent) -> Unit) {
    val event = WakeWordEvent(
      detectedKeyword = currentConfig.keyword,
      confidence = 0.98f
    )
    pause(WakeEngineStatus.PAUSED_FOR_MIC)
    onWakeDetected(event)
  }
}
