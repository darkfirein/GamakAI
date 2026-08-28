package com.example.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class AssistantAudioManager(private val context: Context) {

  companion object {
    private const val TAG = "AssistantAudioManager"
  }

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private var audioFocusRequest: AudioFocusRequest? = null

  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vibratorManager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  fun triggerWakeHaptic() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 35, 45, 60), -1)
        vibrator?.vibrate(effect)
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(70)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Haptic vibration failed", e)
    }
  }

  fun triggerTapHaptic() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(25)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Tap haptic failed", e)
    }
  }

  fun playActivationChime() {
    try {
      val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
      toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
      // Release tone generator after short playback
      android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        try {
          toneGen.release()
        } catch (_: Exception) {}
      }, 250)
    } catch (e: Exception) {
      Log.w(TAG, "Activation chime failed", e)
    }
  }

  fun requestAudioFocus(): Boolean {
    if (audioManager == null) return true

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val playbackAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

      val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(playbackAttributes)
        .setAcceptsDelayedFocusGain(false)
        .setOnAudioFocusChangeListener { focusChange ->
          Log.d(TAG, "Audio focus changed: $focusChange")
        }
        .build()

      audioFocusRequest = focusRequest
      audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    } else {
      @Suppress("DEPRECATION")
      audioManager.requestAudioFocus(
        null,
        AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
      ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
  }

  fun abandonAudioFocus() {
    if (audioManager == null) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
      audioFocusRequest = null
    } else {
      @Suppress("DEPRECATION")
      audioManager.abandonAudioFocus(null)
    }
  }
}
