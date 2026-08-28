package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AssistantState
import com.example.ui.theme.GamakCyan
import com.example.ui.theme.GamakViolet

@Composable
fun GamakHeader(
  assistantName: String,
  onNavigateToSettings: () -> Unit,
  onNavigateToAbout: () -> Unit,
  onForceStateTest: (AssistantState) -> Unit,
  onClearChat: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp)
      .testTag("gamak_header"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Left: Brand Logo + Identity
    Row(
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .border(
            width = 1.5.dp,
            brush = Brush.linearGradient(listOf(GamakCyan, GamakViolet)),
            shape = CircleShape
          )
          .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_gamak_logo),
          contentDescription = "Gamak AI Logo",
          modifier = Modifier.size(36.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "GAMAK AI",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(6.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
              .padding(horizontal = 5.dp, vertical = 2.dp)
          ) {
            Text(
              text = "PHASE 1",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Text(
          text = "Persona: $assistantName",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.sp
        )
      }
    }

    // Right: Actions (About, Settings, Context Menu)
    Row(
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onNavigateToAbout,
        modifier = Modifier.testTag("about_button")
      ) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = "About Gamak AI",
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      IconButton(
        onClick = onNavigateToSettings,
        modifier = Modifier.testTag("settings_button")
      ) {
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = "Settings",
          tint = MaterialTheme.colorScheme.primary
        )
      }

      Box {
        IconButton(
          onClick = { showMenu = true },
          modifier = Modifier.testTag("more_options_button")
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "More Options",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          modifier = Modifier.testTag("header_dropdown_menu")
        ) {
          DropdownMenuItem(
            text = { Text("Clear Chat History") },
            onClick = {
              showMenu = false
              onClearChat()
            }
          )

          DropdownMenuItem(
            text = { Text("Simulate: Listening") },
            onClick = {
              showMenu = false
              onForceStateTest(AssistantState.LISTENING)
            }
          )

          DropdownMenuItem(
            text = { Text("Simulate: Thinking") },
            onClick = {
              showMenu = false
              onForceStateTest(AssistantState.THINKING)
            }
          )

          DropdownMenuItem(
            text = { Text("Simulate: Speaking") },
            onClick = {
              showMenu = false
              onForceStateTest(AssistantState.SPEAKING)
            }
          )

          DropdownMenuItem(
            text = { Text("Simulate: Confirming") },
            onClick = {
              showMenu = false
              onForceStateTest(AssistantState.CONFIRMING)
            }
          )

          DropdownMenuItem(
            text = { Text("Simulate: Clarifying") },
            onClick = {
              showMenu = false
              onForceStateTest(AssistantState.CLARIFYING)
            }
          )

          DropdownMenuItem(
            text = { Text("Simulate: Error State") },
            onClick = {
              showMenu = false
              onForceStateTest(AssistantState.ERROR)
            }
          )
        }
      }
    }
  }
}
