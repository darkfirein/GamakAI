package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.model.UserSettings
import com.example.navigation.GamakNavHost
import com.example.ui.theme.GamakTheme
import com.example.voice.wakeword.WakeWordService

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val app = application as GamakApplication
    val container = app.container

    handleWakeIntent(intent)

    setContent {
      val settings by container.settingsRepository.settingsFlow.collectAsStateWithLifecycle(
        initialValue = UserSettings()
      )

      LaunchedEffect(settings.wakeWordEnabled, settings.assistantName, settings.wakeWordSensitivity) {
        val hasMicPermission = ContextCompat.checkSelfPermission(
          this@MainActivity,
          Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (settings.wakeWordEnabled && hasMicPermission) {
          WakeWordService.startService(
            this@MainActivity,
            keyword = settings.activeDisplayName,
            sensitivity = settings.wakeWordSensitivity
          )
        } else {
          WakeWordService.stopService(this@MainActivity)
        }
      }

      GamakTheme(themeMode = settings.themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
          val navController = rememberNavController()
          GamakNavHost(
            navController = navController,
            container = container
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleWakeIntent(intent)
  }

  private fun handleWakeIntent(intent: Intent?) {
    if (intent?.action == WakeWordService.ACTION_WAKE_WORD_TRIGGERED) {
      val app = application as? GamakApplication
      app?.container?.let { container ->
        container.assistantAudioManager.triggerWakeHaptic()
        container.assistantAudioManager.playActivationChime()
      }
    }
  }
}


