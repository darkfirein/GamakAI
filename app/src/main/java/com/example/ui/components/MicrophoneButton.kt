package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.GamakAmber
import com.example.ui.theme.GamakBlue
import com.example.ui.theme.GamakCyan
import com.example.ui.theme.GamakError
import com.example.ui.theme.GamakPink
import com.example.ui.theme.GamakViolet

@Composable
fun MicrophoneButton(
  state: AssistantState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val pressScale by animateFloatAsState(
    targetValue = if (isPressed) 0.92f else 1.0f,
    animationSpec = tween(120),
    label = "press_scale"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = if (state == AssistantState.LISTENING) 1.32f else 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (state == AssistantState.LISTENING) 800 else 1800,
        easing = FastOutSlowInEasing
      ),
      repeatMode = RepeatMode.Reverse
    ),
    label = "halo_pulse"
  )

  val haloAlpha by infiniteTransition.animateFloat(
    initialValue = 0.5f,
    targetValue = 0.1f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (state == AssistantState.LISTENING) 800 else 1800,
        easing = FastOutSlowInEasing
      ),
      repeatMode = RepeatMode.Reverse
    ),
    label = "halo_alpha"
  )

  val buttonColor by animateColorAsState(
    targetValue = when (state) {
      AssistantState.IDLE -> GamakCyan
      AssistantState.LISTENING -> GamakBlue
      AssistantState.THINKING -> GamakViolet
      AssistantState.SPEAKING -> GamakCyan
      AssistantState.CONFIRMING -> GamakPink
      AssistantState.CLARIFYING -> GamakAmber
      AssistantState.ERROR -> GamakError
    },
    animationSpec = tween(400),
    label = "mic_button_color"
  )

  val iconVector = when (state) {
    AssistantState.IDLE -> Icons.Default.Mic
    AssistantState.LISTENING -> Icons.Default.Mic
    AssistantState.THINKING -> Icons.Default.Psychology
    AssistantState.SPEAKING -> Icons.Default.Close
    AssistantState.CONFIRMING -> Icons.Default.Close
    AssistantState.CLARIFYING -> Icons.Default.Close
    AssistantState.ERROR -> Icons.Default.Refresh
  }

  val contentDesc = when (state) {
    AssistantState.IDLE -> "Start listening with Gamak AI"
    AssistantState.LISTENING -> "Listening, tap to process"
    AssistantState.THINKING -> "Thinking, tap to cancel"
    AssistantState.SPEAKING -> "Speaking, tap to stop"
    AssistantState.CONFIRMING -> "Tap to dismiss"
    AssistantState.CLARIFYING -> "Tap to dismiss"
    AssistantState.ERROR -> "Error, tap to retry"
  }

  Box(
    modifier = modifier
      .defaultMinSize(minWidth = 72.dp, minHeight = 72.dp)
      .scale(pressScale),
    contentAlignment = Alignment.Center
  ) {
    // Outer Pulsing Halo Ring
    Box(
      modifier = Modifier
        .size(68.dp * pulseScale)
        .clip(CircleShape)
        .background(buttonColor.copy(alpha = haloAlpha))
    )

    // Inner Button
    Box(
      modifier = Modifier
        .size(64.dp)
        .shadow(
          elevation = if (state == AssistantState.LISTENING) 16.dp else 8.dp,
          shape = CircleShape,
          spotColor = buttonColor
        )
        .clip(CircleShape)
        .background(
          brush = Brush.linearGradient(
            colors = listOf(
              buttonColor,
              buttonColor.copy(alpha = 0.85f),
              Color(0xFF0F172A)
            )
          )
        )
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          role = Role.Button,
          onClick = onClick
        )
        .testTag("mic_button"),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = iconVector,
        contentDescription = contentDesc,
        tint = if (state == AssistantState.IDLE || state == AssistantState.SPEAKING) Color(0xFF070B14) else Color.White,
        modifier = Modifier.size(30.dp)
      )
    }
  }
}
