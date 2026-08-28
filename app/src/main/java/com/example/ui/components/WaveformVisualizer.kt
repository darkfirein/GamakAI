package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.GamakAmber
import com.example.ui.theme.GamakBlue
import com.example.ui.theme.GamakCyan
import com.example.ui.theme.GamakError
import com.example.ui.theme.GamakPink
import com.example.ui.theme.GamakViolet

@Composable
fun WaveformVisualizer(
  audioLevels: List<Float>,
  state: AssistantState,
  modifier: Modifier = Modifier,
  height: Dp = 44.dp
) {
  val primaryColor by animateColorAsState(
    targetValue = when (state) {
      AssistantState.IDLE -> GamakCyan.copy(alpha = 0.45f)
      AssistantState.LISTENING -> GamakBlue
      AssistantState.THINKING -> GamakViolet
      AssistantState.SPEAKING -> GamakCyan
      AssistantState.CONFIRMING -> GamakPink
      AssistantState.CLARIFYING -> GamakAmber
      AssistantState.ERROR -> GamakError
    },
    animationSpec = tween(400),
    label = "waveform_color"
  )

  val secondaryColor by animateColorAsState(
    targetValue = when (state) {
      AssistantState.IDLE -> GamakViolet.copy(alpha = 0.35f)
      AssistantState.LISTENING -> GamakCyan
      AssistantState.THINKING -> GamakPink
      AssistantState.SPEAKING -> GamakBlue
      AssistantState.CONFIRMING -> GamakViolet
      AssistantState.CLARIFYING -> GamakPink
      AssistantState.ERROR -> Color(0xFF8B0029)
    },
    animationSpec = tween(400),
    label = "waveform_sec_color"
  )

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .testTag("waveform_visualizer")
  ) {
    if (audioLevels.isEmpty()) return@Canvas

    val barCount = audioLevels.size
    val totalWidth = size.width
    val canvasHeight = size.height
    val centerY = canvasHeight / 2f

    val totalSpacing = totalWidth * 0.4f
    val barSpacing = totalSpacing / (barCount - 1).coerceAtLeast(1)
    val barWidth = (totalWidth - totalSpacing) / barCount

    val brush = Brush.verticalGradient(
      colors = listOf(primaryColor, secondaryColor, primaryColor),
      startY = 0f,
      endY = canvasHeight
    )

    for (i in 0 until barCount) {
      val level = audioLevels[i].coerceIn(0.08f, 1.0f)
      val barHeight = (canvasHeight * level).coerceAtLeast(4.dp.toPx())
      val left = i * (barWidth + barSpacing)
      val top = centerY - (barHeight / 2f)

      drawRoundRect(
        brush = brush,
        topLeft = Offset(left, top),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
      )
    }
  }
}
