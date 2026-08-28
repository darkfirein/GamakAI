package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.GamakAmber
import com.example.ui.theme.GamakBlue
import com.example.ui.theme.GamakCyan
import com.example.ui.theme.GamakError
import com.example.ui.theme.GamakPink
import com.example.ui.theme.GamakViolet

enum class AssistantState(
  val label: String,
  val description: String,
  val icon: ImageVector,
  val accentColor: Color
) {
  IDLE(
    label = "Ready",
    description = "Tap microphone or choose a quick prompt to start",
    icon = Icons.Default.AutoAwesome,
    accentColor = GamakCyan
  ),
  LISTENING(
    label = "Listening...",
    description = "Capturing audio input & intent...",
    icon = Icons.Default.Mic,
    accentColor = GamakBlue
  ),
  THINKING(
    label = "Synthesizing...",
    description = "Processing neural reasoning engine...",
    icon = Icons.Default.Psychology,
    accentColor = GamakViolet
  ),
  SPEAKING(
    label = "Responding",
    description = "Delivering vocal and contextual answer...",
    icon = Icons.Default.GraphicEq,
    accentColor = GamakCyan
  ),
  CONFIRMING(
    label = "Confirmation Required",
    description = "Verifying parameters before execution...",
    icon = Icons.Default.CheckCircle,
    accentColor = GamakPink
  ),
  CLARIFYING(
    label = "Clarification Needed",
    description = "Please specify additional context...",
    icon = Icons.AutoMirrored.Filled.Help,
    accentColor = GamakAmber
  ),
  ERROR(
    label = "Attention Needed",
    description = "A temporary connection or processing interruption occurred",
    icon = Icons.Default.ErrorOutline,
    accentColor = GamakError
  );

  val isActionable: Boolean
    get() = this == IDLE || this == ERROR || this == CONFIRMING || this == CLARIFYING
}
