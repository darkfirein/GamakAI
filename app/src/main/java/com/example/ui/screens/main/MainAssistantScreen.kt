package com.example.ui.screens.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AssistantState
import com.example.ui.components.AnimatedAiOrb
import com.example.ui.components.AssistantStatusBadge
import com.example.ui.components.ConversationArea
import com.example.ui.components.GamakHeader
import com.example.ui.components.MicrophoneButton
import com.example.ui.components.QuickActionArea
import com.example.ui.components.WaveformVisualizer

@Composable
fun MainAssistantScreen(
  viewModel: MainViewModel,
  onNavigateToSettings: () -> Unit,
  onNavigateToAbout: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val activeAssistantName = uiState.userSettings.activeDisplayName

  val singlePermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      viewModel.onMicTapped()
    }
  }

  val dynamicPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) {
    // Permission granted by user; user can re-trigger action
  }

  val handleMicClick = {
    val hasPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    if (hasPermission) {
      viewModel.onMicTapped()
    } else {
      singlePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
      .testTag("main_assistant_screen"),
    topBar = {
      GamakHeader(
        assistantName = activeAssistantName,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToAbout = onNavigateToAbout,
        onForceStateTest = { viewModel.onForceStateTest(it) },
        onClearChat = { viewModel.onClearChat() }
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // 1. AI Assistant Visualizer Stage
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
      ) {
        Column(
          modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Animated AI Orb (Centerpiece)
          AnimatedAiOrb(
            state = uiState.assistantState,
            orbSize = 135.dp,
            onClick = { handleMicClick() }
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Assistant Status Badge
          AssistantStatusBadge(
            state = uiState.assistantState,
            assistantName = activeAssistantName
          )

          Spacer(modifier = Modifier.height(6.dp))

          // Live Equalizer / Waveform
          WaveformVisualizer(
            audioLevels = uiState.audioLevels,
            state = uiState.assistantState,
            modifier = Modifier.padding(horizontal = 24.dp),
            height = 24.dp
          )
        }
      }

      // 2. Quick Action Area
      Spacer(modifier = Modifier.height(6.dp))
      QuickActionArea(
        actions = uiState.quickActions,
        onActionSelected = { viewModel.onQuickActionClicked(it) }
      )

      // 3. Conversation Area
      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        ConversationArea(
          messages = uiState.messages,
          assistantName = activeAssistantName,
          onConfirmAction = { viewModel.onConfirmAction(it) },
          onClarifyReply = { viewModel.onClarifyReply(it) },
          onOptionSelected = { viewModel.onOptionSelected(it) },
          onPermissionRequest = { requestedPerm ->
            dynamicPermissionLauncher.launch(requestedPerm)
          }
        )
      }

      // 4. Input & Microphone Control Deck
      BottomControlDeck(
        inputText = uiState.inputText,
        assistantState = uiState.assistantState,
        assistantName = activeAssistantName,
        onInputTextChanged = { viewModel.onInputTextChanged(it) },
        onSendClicked = { viewModel.onSendPrompt() },
        onMicClicked = { handleMicClick() }
      )
    }
  }
}

@Composable
private fun BottomControlDeck(
  inputText: String,
  assistantState: AssistantState,
  assistantName: String,
  onInputTextChanged: (String) -> Unit,
  onSendClicked: () -> Unit,
  onMicClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp,
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Text Input Field
      OutlinedTextField(
        value = inputText,
        onValueChange = onInputTextChanged,
        placeholder = {
          Text(
            text = "Ask $assistantName in Hindi, Nepali, English...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontSize = 13.sp
          )
        },
        modifier = Modifier
          .weight(1f)
          .testTag("prompt_input_field"),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onSendClicked() })
      )

      // Send Button (Visible when text present)
      AnimatedVisibility(
        visible = inputText.isNotBlank(),
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        IconButton(
          onClick = onSendClicked,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .testTag("send_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send prompt",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      // Tactile Glowing Microphone Button
      MicrophoneButton(
        state = assistantState,
        onClick = onMicClicked
      )
    }
  }
}
