package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QuickAction

@Composable
fun QuickActionArea(
  actions: List<QuickAction>,
  onActionSelected: (QuickAction) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyRow(
    modifier = modifier
      .fillMaxWidth()
      .testTag("quick_action_area"),
    contentPadding = PaddingValues(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    items(actions, key = { it.id }) { action ->
      QuickActionChip(
        action = action,
        onClick = { onActionSelected(action) }
      )
    }
  }
}

@Composable
fun QuickActionChip(
  action: QuickAction,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .background(
        brush = Brush.horizontalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
          )
        )
      )
      .border(
        width = 1.dp,
        brush = Brush.horizontalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
          )
        ),
        shape = RoundedCornerShape(20.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 9.dp)
      .testTag("quick_action_${action.id}")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = action.icon,
        contentDescription = action.title,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = action.title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}
