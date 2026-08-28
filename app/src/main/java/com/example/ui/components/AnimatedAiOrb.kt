package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedAiOrb(
  state: AssistantState,
  modifier: Modifier = Modifier,
  orbSize: Dp = 190.dp,
  onClick: (() -> Unit)? = null
) {
  val infiniteTransition = rememberInfiniteTransition(label = "orb_infinite")

  // Rotation animation
  val rotationSpeed = when (state) {
    AssistantState.THINKING -> 2400
    AssistantState.LISTENING -> 4500
    AssistantState.SPEAKING -> 3500
    AssistantState.CONFIRMING -> 3000
    AssistantState.CLARIFYING -> 5000
    AssistantState.ERROR -> 7000
    AssistantState.IDLE -> 6500
  }

  val primaryRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = rotationSpeed, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "primary_rotation"
  )

  val reverseRotation by infiniteTransition.animateFloat(
    initialValue = 360f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = (rotationSpeed * 1.4).toInt(), easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "reverse_rotation"
  )

  // Breathing pulse
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.92f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (state == AssistantState.SPEAKING || state == AssistantState.LISTENING) 900 else 2200,
        easing = FastOutSlowInEasing
      ),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  // Core glow intensity
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_alpha"
  )

  // Animated color shifts according to state
  val primaryColor by animateColorAsState(
    targetValue = when (state) {
      AssistantState.IDLE -> GamakCyan
      AssistantState.LISTENING -> GamakBlue
      AssistantState.THINKING -> GamakViolet
      AssistantState.SPEAKING -> GamakCyan
      AssistantState.CONFIRMING -> GamakPink
      AssistantState.CLARIFYING -> GamakAmber
      AssistantState.ERROR -> GamakError
    },
    animationSpec = tween(600),
    label = "primary_color"
  )

  val secondaryColor by animateColorAsState(
    targetValue = when (state) {
      AssistantState.IDLE -> GamakViolet
      AssistantState.LISTENING -> GamakCyan
      AssistantState.THINKING -> GamakPink
      AssistantState.SPEAKING -> GamakBlue
      AssistantState.CONFIRMING -> GamakViolet
      AssistantState.CLARIFYING -> GamakPink
      AssistantState.ERROR -> Color(0xFF8B0029)
    },
    animationSpec = tween(600),
    label = "secondary_color"
  )

  val targetStateScale by animateFloatAsState(
    targetValue = when (state) {
      AssistantState.LISTENING -> 1.12f
      AssistantState.SPEAKING -> 1.06f
      AssistantState.THINKING -> 0.98f
      AssistantState.CONFIRMING -> 1.02f
      AssistantState.CLARIFYING -> 1.0f
      AssistantState.ERROR -> 0.94f
      AssistantState.IDLE -> 1.0f
    },
    animationSpec = tween(500),
    label = "state_scale"
  )

  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier
      .size(orbSize)
      .testTag("animated_ai_orb")
      .then(
        if (onClick != null) {
          Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
          )
        } else Modifier
      ),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val baseRadius = (size.width / 2f) * 0.72f * pulseScale * targetStateScale

      // 1. Outer ambient glow ring
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            primaryColor.copy(alpha = glowAlpha * 0.5f),
            secondaryColor.copy(alpha = glowAlpha * 0.2f),
            Color.Transparent
          ),
          center = center,
          radius = baseRadius * 1.55f
        ),
        radius = baseRadius * 1.55f,
        center = center
      )

      // 2. Rotating orbital segmented ring 1
      rotate(degrees = primaryRotation, pivot = center) {
        drawOrbitalRing(
          center = center,
          radius = baseRadius * 1.22f,
          strokeWidth = 2.5.dp.toPx(),
          color = primaryColor.copy(alpha = 0.75f),
          dashIntervals = floatArrayOf(24f, 16f, 48f, 16f)
        )
      }

      // 3. Rotating orbital segmented ring 2 (Counter-rotation)
      rotate(degrees = reverseRotation, pivot = center) {
        drawOrbitalRing(
          center = center,
          radius = baseRadius * 1.05f,
          strokeWidth = 1.8.dp.toPx(),
          color = secondaryColor.copy(alpha = 0.65f),
          dashIntervals = floatArrayOf(36f, 20f, 18f, 20f)
        )
      }

      // 4. Core energetic sphere with multi-stop radial gradient
      drawCircle(
        brush = Brush.radialGradient(
          colorStops = arrayOf(
            0.0f to Color.White.copy(alpha = 0.95f),
            0.25f to primaryColor.copy(alpha = 0.9f),
            0.7f to secondaryColor.copy(alpha = 0.75f),
            1.0f to primaryColor.copy(alpha = 0.15f)
          ),
          center = center,
          radius = baseRadius * 0.85f
        ),
        radius = baseRadius * 0.85f,
        center = center
      )

      // 5. Dynamic energy core nodes based on state
      drawEnergyNodes(
        center = center,
        radius = baseRadius * 0.6f,
        rotation = primaryRotation,
        color = primaryColor,
        state = state
      )
    }
  }
}

private fun DrawScope.drawOrbitalRing(
  center: Offset,
  radius: Float,
  strokeWidth: Float,
  color: Color,
  dashIntervals: FloatArray
) {
  drawCircle(
    color = color,
    radius = radius,
    center = center,
    style = Stroke(
      width = strokeWidth,
      pathEffect = PathEffect.dashPathEffect(dashIntervals, 0f)
    )
  )
}

private fun DrawScope.drawEnergyNodes(
  center: Offset,
  radius: Float,
  rotation: Float,
  color: Color,
  state: AssistantState
) {
  val nodeCount = if (state == AssistantState.THINKING) 6 else 4
  val radOffset = Math.toRadians(rotation.toDouble())

  for (i in 0 until nodeCount) {
    val angle = radOffset + (i * (2 * Math.PI / nodeCount))
    val x = center.x + (radius * cos(angle)).toFloat()
    val y = center.y + (radius * sin(angle)).toFloat()

    drawCircle(
      color = Color.White.copy(alpha = 0.85f),
      radius = 3.dp.toPx(),
      center = Offset(x, y)
    )

    drawCircle(
      color = color.copy(alpha = 0.4f),
      radius = 6.dp.toPx(),
      center = Offset(x, y)
    )
  }
}
