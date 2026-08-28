package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState

@Composable
fun AssistantStatusBadge(
  state: AssistantState,
  assistantName: String,
  modifier: Modifier = Modifier
) {
  val accentColor by animateColorAsState(
    targetValue = state.accentColor,
    label = "badge_color"
  )

  Column(
    modifier = modifier.testTag("assistant_status_badge"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Status Pill
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(24.dp))
        .background(accentColor.copy(alpha = 0.12f))
        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
        .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        // Glowing dot
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))

        AnimatedContent(
          targetState = state,
          transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
          },
          label = "status_text_anim"
        ) { targetState ->
          val label = when (targetState) {
            AssistantState.IDLE -> "$assistantName is Standby"
            AssistantState.LISTENING -> "$assistantName is Listening..."
            AssistantState.THINKING -> "$assistantName is Synthesizing..."
            AssistantState.SPEAKING -> "$assistantName is Responding"
            AssistantState.CONFIRMING -> "$assistantName requires Confirmation"
            AssistantState.CLARIFYING -> "$assistantName asks Clarification"
            AssistantState.ERROR -> "$assistantName Engine Alert"
          }

          Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = accentColor,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    AnimatedContent(
      targetState = state,
      transitionSpec = {
        fadeIn() togetherWith fadeOut()
      },
      label = "status_desc_anim"
    ) { targetState ->
      Text(
        text = targetState.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 24.dp)
      )
    }
  }
}
