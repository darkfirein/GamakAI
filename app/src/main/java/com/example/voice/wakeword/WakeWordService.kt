package com.example.voice.wakeword

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.GamakApplication
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WakeWordService : Service() {

  companion object {
    private const val TAG = "WakeWordService"
    const val NOTIFICATION_ID = 1001
    const val CHANNEL_ID = "gamak_wake_word_channel"

    const val ACTION_START_LISTENING = "com.example.ACTION_START_WAKE_WORD"
    const val ACTION_STOP_LISTENING = "com.example.ACTION_STOP_WAKE_WORD"
    const val ACTION_START_WAKE_SERVICE = "com.example.action.START_WAKE_SERVICE"
    const val ACTION_STOP_WAKE_SERVICE = "com.example.action.STOP_WAKE_SERVICE"
    const val ACTION_UPDATE_CONFIG = "com.example.action.UPDATE_WAKE_CONFIG"
    const val ACTION_WAKE_WORD_TRIGGERED = "com.example.ACTION_WAKE_WORD_TRIGGERED"

    const val EXTRA_KEYWORD = "extra_keyword"
    const val EXTRA_SENSITIVITY = "extra_sensitivity"

    fun startService(context: Context, keyword: String = "Gamak", sensitivity: Float = 0.8f) {
      val intent = Intent(context, WakeWordService::class.java).apply {
        action = ACTION_START_WAKE_SERVICE
        putExtra(EXTRA_KEYWORD, keyword)
        putExtra(EXTRA_SENSITIVITY, sensitivity)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stopService(context: Context) {
      val intent = Intent(context, WakeWordService::class.java).apply {
        action = ACTION_STOP_WAKE_SERVICE
      }
      context.startService(intent)
    }
  }

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var wakeWordEngine: WakeWordEngine? = null
  private var currentKeyword = "Gamak"
  private var currentSensitivity = 0.8f

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    val app = application as? GamakApplication
    wakeWordEngine = app?.container?.wakeWordEngine ?: KeywordSpotterWakeWordEngine(this, serviceScope)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP_WAKE_SERVICE, ACTION_STOP_LISTENING -> {
        stopWakeListening()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_START_WAKE_SERVICE, ACTION_START_LISTENING, ACTION_UPDATE_CONFIG, null -> {
        currentKeyword = intent?.getStringExtra(EXTRA_KEYWORD) ?: currentKeyword
        currentSensitivity = intent?.getFloatExtra(EXTRA_SENSITIVITY, currentSensitivity) ?: currentSensitivity

        val notification = buildForegroundNotification(currentKeyword)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
          )
        } else {
          startForeground(NOTIFICATION_ID, notification)
        }

        startWakeListening()
      }
    }
    return START_STICKY
  }

  private fun startWakeListening() {
    val config = WakeWordConfig(
      keyword = currentKeyword,
      sensitivity = currentSensitivity,
      isBackgroundServiceEnabled = true
    )

    wakeWordEngine?.start(config) { event ->
      Log.d(TAG, "Wake word detected in background service: ${event.detectedKeyword}")
      handleWakeDetected(event)
    }
  }

  private fun stopWakeListening() {
    wakeWordEngine?.stop()
  }

  private fun handleWakeDetected(event: WakeWordEvent) {
    val app = application as? GamakApplication
    app?.container?.assistantAudioManager?.triggerWakeHaptic()
    app?.container?.assistantAudioManager?.playActivationChime()

    // Trigger Assistant Engine to active listening state
    serviceScope.launch {
      val settings = app?.container?.settingsRepository?.settingsFlow?.firstOrNull()
      val assistantName = settings?.activeDisplayName ?: currentKeyword
      app?.container?.assistantEngine?.onWakeWordDetected(assistantName)
    }

    // Launch or bring MainActivity to front
    val mainIntent = Intent(this, MainActivity::class.java).apply {
      action = ACTION_WAKE_WORD_TRIGGERED
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_KEYWORD, event.detectedKeyword)
    }
    startActivity(mainIntent)
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Gamak Voice Activation",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Monitors wake word activation for Gamak AI assistant"
        setShowBadge(false)
        enableVibration(false)
      }
      val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  private fun buildForegroundNotification(keyword: String): Notification {
    val openAppIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val openAppPendingIntent = PendingIntent.getActivity(
      this,
      0,
      openAppIntent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val stopIntent = Intent(this, WakeWordService::class.java).apply {
      action = ACTION_STOP_WAKE_SERVICE
    }
    val stopPendingIntent = PendingIntent.getService(
      this,
      1,
      stopIntent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("$keyword AI Voice Core")
      .setContentText("Listening for wake word • Tap to open")
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentIntent(openAppPendingIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .addAction(
        android.R.drawable.ic_menu_close_clear_cancel,
        "Stop",
        stopPendingIntent
      )
      .build()
  }

  override fun onDestroy() {
    stopWakeListening()
    serviceScope.cancel()
    super.onDestroy()
  }
}
