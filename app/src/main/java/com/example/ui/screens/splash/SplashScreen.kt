package com.example.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GamakBlue
import com.example.ui.theme.GamakCyan
import com.example.ui.theme.GamakViolet
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onSplashFinished: () -> Unit,
  modifier: Modifier = Modifier
) {
  val logoScale = remember { Animatable(0.4f) }
  val logoAlpha = remember { Animatable(0f) }
  val textAlpha = remember { Animatable(0f) }
  val badgeAlpha = remember { Animatable(0f) }

  val infiniteTransition = rememberInfiniteTransition(label = "splash_ring")
  val ringPulse by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "splash_ring_pulse"
  )

  LaunchedEffect(Unit) {
    // 1. Entrance animation
    logoAlpha.animateTo(1f, animationSpec = tween(600))
    logoScale.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    textAlpha.animateTo(1f, animationSpec = tween(500))
    badgeAlpha.animateTo(1f, animationSpec = tween(400))

    // 2. Hold for brand impression
    delay(1400)

    // 3. Smooth transition out
    onSplashFinished()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFF0F1B38),
            Color(0xFF070B14),
            Color(0xFF03050A)
          )
        )
      )
      .testTag("splash_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Animated Logo with Outer Glowing Holographic Ring
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(180.dp)
      ) {
        // Outer pulsing ring
        Box(
          modifier = Modifier
            .size(160.dp * ringPulse)
            .clip(CircleShape)
            .background(GamakCyan.copy(alpha = 0.12f))
        )

        // Mid rotating border frame
        Box(
          modifier = Modifier
            .size(130.dp)
            .scale(logoScale.value)
            .alpha(logoAlpha.value)
            .clip(CircleShape)
            .border(
              width = 2.5.dp,
              brush = Brush.sweepGradient(
                listOf(GamakCyan, GamakBlue, GamakViolet, GamakCyan)
              ),
              shape = CircleShape
            )
            .background(Color(0xFF0A0E1A)),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_gamak_logo),
            contentDescription = "Gamak AI Logo",
            modifier = Modifier.size(116.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Brand Title
      Text(
        text = "GAMAK AI",
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 4.sp,
        color = Color.White,
        modifier = Modifier
          .alpha(textAlpha.value)
          .testTag("splash_title")
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Next-Generation Intelligence Core",
        style = MaterialTheme.typography.bodyLarge,
        color = GamakCyan,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.alpha(textAlpha.value)
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Phase Badge
      Box(
        modifier = Modifier
          .alpha(badgeAlpha.value)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF1E293B))
          .border(1.dp, GamakViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          .padding(horizontal = 16.dp, vertical = 6.dp)
      ) {
        Text(
          text = "PHASE 1 • FOUNDATION & STATE SYSTEM",
          style = MaterialTheme.typography.labelSmall,
          color = GamakViolet,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      }
    }
  }
}
