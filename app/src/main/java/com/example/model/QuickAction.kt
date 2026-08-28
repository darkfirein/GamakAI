package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

data class QuickAction(
  val id: String,
  val title: String,
  val icon: ImageVector,
  val prompt: String,
  val targetState: AssistantState = AssistantState.THINKING
) {
  companion object {
    val defaultActions = listOf(
      QuickAction(
        id = "action_multistep",
        title = "Multi-Step Flow",
        icon = Icons.Default.DynamicFeed,
        prompt = "सुबह 7 बजे का अलार्म लगाओ और फिर YouTube खोलो"
      ),
      QuickAction(
        id = "action_youtube",
        title = "YouTube",
        icon = Icons.Default.PlayArrow,
        prompt = "YouTube खोलो"
      ),
      QuickAction(
        id = "action_weather",
        title = "Kathmandu मौसम",
        icon = Icons.Default.Cloud,
        prompt = "Kathmandu में मौसम कैसा है?"
      ),
      QuickAction(
        id = "action_alarm",
        title = "Alarm 7 AM",
        icon = Icons.Default.Alarm,
        prompt = "कल सुबह 7 बजे alarm लगा देना"
      ),
      QuickAction(
        id = "action_camera",
        title = "Camera",
        icon = Icons.Default.CameraAlt,
        prompt = "Camera खोलो"
      ),
      QuickAction(
        id = "action_whatsapp_msg",
        title = "WhatsApp Rahul",
        icon = Icons.AutoMirrored.Filled.Chat,
        prompt = "WhatsApp पर Rahul को message लिखो"
      ),
      QuickAction(
        id = "action_navigation",
        title = "घर का रास्ता",
        icon = Icons.Default.Navigation,
        prompt = "मुझे घर का रास्ता दिखाओ"
      ),
      QuickAction(
        id = "intro",
        title = "Self Introduction",
        icon = Icons.Default.Bolt,
        prompt = "नमस्ते! तुम कौन हो और क्या कर सकते हो?"
      ),
      QuickAction(
        id = "voice_test",
        title = "Voice Cycle",
        icon = Icons.Default.Mic,
        prompt = "Gamak voice engine test karo"
      )
    )
  }
}
