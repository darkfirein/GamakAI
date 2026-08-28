package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.model.ChatMessage
import com.example.tools.ConfirmationDetails
import com.example.tools.PickerOption
import com.example.tools.TaskPlan
import com.example.tools.TaskStep
import com.example.tools.TaskStepStatus
import com.example.ui.theme.GamakAmber
import com.example.ui.theme.GamakBlue
import com.example.ui.theme.GamakCyan
import com.example.ui.theme.GamakError
import com.example.ui.theme.GamakGreen
import com.example.ui.theme.GamakPink
import com.example.ui.theme.GamakViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationArea(
  messages: List<ChatMessage>,
  assistantName: String,
  onConfirmAction: (Boolean) -> Unit,
  onClarifyReply: (String) -> Unit,
  onOptionSelected: (PickerOption) -> Unit = {},
  onPermissionRequest: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  LazyColumn(
    state = listState,
    modifier = modifier
      .fillMaxWidth()
      .testTag("conversation_area"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(messages, key = { it.id }) { message ->
      AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { it / 2 }
      ) {
        if (message.isUser) {
          UserMessageItem(message = message)
        } else {
          AssistantMessageItem(
            message = message,
            assistantName = assistantName,
            onConfirmAction = onConfirmAction,
            onClarifyReply = onClarifyReply,
            onOptionSelected = onOptionSelected,
            onPermissionRequest = onPermissionRequest
          )
        }
      }
    }
  }
}

@Composable
private fun UserMessageItem(message: ChatMessage) {
  val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 48.dp),
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.Bottom
  ) {
    Column(horizontalAlignment = Alignment.End) {
      Box(
        modifier = Modifier
          .clip(
            RoundedCornerShape(
              topStart = 16.dp,
              topEnd = 16.dp,
              bottomStart = 16.dp,
              bottomEnd = 4.dp
            )
          )
          .background(
            brush = Brush.linearGradient(
              colors = listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.secondaryContainer
              )
            )
          )
          .border(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            RoundedCornerShape(
              topStart = 16.dp,
              topEnd = 16.dp,
              bottomStart = 16.dp,
              bottomEnd = 4.dp
            )
          )
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Text(
          text = message.text,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontSize = 14.sp
        )
      }

      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = timeStr,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        fontSize = 10.sp
      )
    }

    Spacer(modifier = Modifier.width(6.dp))

    Box(
      modifier = Modifier
        .size(24.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Person,
        contentDescription = "User",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(14.dp)
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantMessageItem(
  message: ChatMessage,
  assistantName: String,
  onConfirmAction: (Boolean) -> Unit,
  onClarifyReply: (String) -> Unit,
  onOptionSelected: (PickerOption) -> Unit,
  onPermissionRequest: (String) -> Unit
) {
  val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
  val accentColor = when (message.stateTag) {
    AssistantState.IDLE -> GamakCyan
    AssistantState.LISTENING -> GamakBlue
    AssistantState.THINKING -> GamakViolet
    AssistantState.SPEAKING -> GamakCyan
    AssistantState.CONFIRMING -> GamakPink
    AssistantState.CLARIFYING -> GamakAmber
    AssistantState.ERROR -> GamakError
    null -> GamakCyan
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(end = 32.dp),
    horizontalArrangement = Arrangement.Start,
    verticalAlignment = Alignment.Top
  ) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(accentColor.copy(alpha = 0.2f))
        .border(1.dp, accentColor, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.AutoAwesome,
        contentDescription = assistantName,
        tint = accentColor,
        modifier = Modifier.size(16.dp)
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    Column(modifier = Modifier.widthIn(max = 330.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = assistantName,
          style = MaterialTheme.typography.labelLarge,
          color = accentColor,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = timeStr,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          fontSize = 10.sp
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Surface(
        shape = RoundedCornerShape(
          topStart = 4.dp,
          topEnd = 16.dp,
          bottomStart = 16.dp,
          bottomEnd = 16.dp
        ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        tonalElevation = 2.dp
      ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            lineHeight = 20.sp
          )

          // 1. Task Plan Stepper Card (Multi-Step Engine)
          if (message.taskPlan != null) {
            Spacer(modifier = Modifier.height(10.dp))
            TaskPlanCard(taskPlan = message.taskPlan)
          }

          // 2. Confirmation Details Card
          if (message.confirmationDetails != null) {
            Spacer(modifier = Modifier.height(10.dp))
            ConfirmationDetailsCard(details = message.confirmationDetails)
          }

          // 3. Multiple Contact / Item Picker Options List
          if (message.pickerOptions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              message.pickerOptions.forEach { option ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = androidx.compose.foundation.BorderStroke(1.dp, GamakAmber.copy(alpha = 0.4f)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOptionSelected(option) }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Call,
                      contentDescription = null,
                      tint = GamakAmber,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = option.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                      )
                      Text(
                        text = option.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                      )
                    }
                  }
                }
              }
            }
          }

          // 4. Permission Request Prompt Button
          if (!message.permissionRequired.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            ElevatedButton(
              onClick = { onPermissionRequest(message.permissionRequired) },
              colors = ButtonDefaults.elevatedButtonColors(
                containerColor = GamakPink,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("अनुमति दें (Grant Permission)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          // 5. Confirmation Buttons
          if (message.stateTag == AssistantState.CONFIRMING && message.permissionRequired.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              ElevatedButton(
                onClick = { onConfirmAction(true) },
                colors = ButtonDefaults.elevatedButtonColors(
                  containerColor = GamakPink,
                  contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Confirm", fontSize = 12.sp)
              }

              OutlinedButton(
                onClick = { onConfirmAction(false) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cancel", fontSize = 12.sp)
              }
            }
          }

          // 6. Quick Clarification Reply Chips
          if (message.quickReplies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              message.quickReplies.forEach { reply ->
                FilledTonalButton(
                  onClick = { onClarifyReply(reply) },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text(text = reply, fontSize = 11.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TaskPlanCard(taskPlan: TaskPlan) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
    border = androidx.compose.foundation.BorderStroke(1.dp, GamakViolet.copy(alpha = 0.4f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Sequential Plan",
          style = MaterialTheme.typography.labelMedium,
          color = GamakViolet,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        )
        Text(
          text = "${taskPlan.steps.count { it.status == TaskStepStatus.COMPLETED }}/${taskPlan.steps.size} done",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 10.sp
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      taskPlan.steps.forEachIndexed { idx, step ->
        TaskStepItem(index = idx + 1, step = step)
        if (idx < taskPlan.steps.size - 1) {
          Spacer(modifier = Modifier.height(6.dp))
        }
      }
    }
  }
}

@Composable
private fun TaskStepItem(index: Int, step: TaskStep) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    when (step.status) {
      TaskStepStatus.PENDING -> {
        Icon(
          imageVector = Icons.Default.RadioButtonUnchecked,
          contentDescription = "Pending",
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.size(16.dp)
        )
      }
      TaskStepStatus.RUNNING -> {
        CircularProgressIndicator(
          strokeWidth = 2.dp,
          color = GamakCyan,
          modifier = Modifier.size(16.dp)
        )
      }
      TaskStepStatus.COMPLETED -> {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Completed",
          tint = GamakGreen,
          modifier = Modifier.size(16.dp)
        )
      }
      TaskStepStatus.FAILED -> {
        Icon(
          imageVector = Icons.Default.Error,
          contentDescription = "Failed",
          tint = GamakError,
          modifier = Modifier.size(16.dp)
        )
      }
      TaskStepStatus.CANCELLED -> {
        Icon(
          imageVector = Icons.Default.Cancel,
          contentDescription = "Cancelled",
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier.size(16.dp)
        )
      }
      TaskStepStatus.WAITING_FOR_CONFIRMATION,
      TaskStepStatus.WAITING_FOR_CLARIFICATION,
      TaskStepStatus.WAITING_FOR_PICKER,
      TaskStepStatus.WAITING_FOR_PERMISSION -> {
        Icon(
          imageVector = Icons.Default.HourglassEmpty,
          contentDescription = "Waiting",
          tint = GamakAmber,
          modifier = Modifier.size(16.dp)
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "$index. ${step.description.ifBlank { step.actionRequest.toolName.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() } }}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (step.status == TaskStepStatus.RUNNING) FontWeight.Bold else FontWeight.Normal,
        color = when (step.status) {
          TaskStepStatus.COMPLETED -> GamakGreen
          TaskStepStatus.FAILED -> GamakError
          TaskStepStatus.RUNNING -> GamakCyan
          TaskStepStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
          else -> MaterialTheme.colorScheme.onSurface
        },
        fontSize = 12.sp
      )
      if (!step.resultMessage.isNullOrBlank() && step.status != TaskStepStatus.PENDING) {
        Text(
          text = step.resultMessage,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 10.sp
        )
      }
    }
  }
}

@Composable
private fun ConfirmationDetailsCard(details: ConfirmationDetails) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(1.dp, GamakPink.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = GamakPink,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = details.actionTitle,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = GamakPink,
          fontSize = 12.sp
        )
      }

      if (!details.targetRecipient.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Recipient: ${details.targetRecipient}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 11.sp
        )
      }

      if (!details.contentSummary.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "\"${details.contentSummary}\"",
          style = MaterialTheme.typography.bodySmall,
          fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.sp
        )
      }
    }
  }
}
