package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AssistantPersona
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.GamakAmber
import com.example.ui.theme.GamakCyan
import com.example.ui.theme.GamakGreen
import com.example.ui.theme.GamakViolet
import com.example.voice.wakeword.WakeEngineStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val settings = uiState.settings

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("settings_screen"),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Voice & Assistant Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.onBackground
        )
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Feedback Toast Banner
      if (uiState.feedbackMessage != null) {
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth().testTag("feedback_banner")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = uiState.feedbackMessage ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      // 1. Active Persona Summary Card
      item {
        ActivePersonaSummaryCard(
          activeName = settings.activeDisplayName,
          persona = settings.persona
        )
      }

      // 2. Persona / Custom Name Section
      item {
        SectionCard(title = "Assistant Identity & Persona", icon = Icons.Default.Psychology) {
          Text(
            text = "Select a predefined persona or give your AI a unique name. All responses, greetings, and UI headers update dynamically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Persona Selector Chips
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistantPersona.entries.forEach { persona ->
              PersonaSelectionRow(
                persona = persona,
                isSelected = settings.persona == persona,
                onSelected = { viewModel.onPersonaSelected(persona) }
              )
            }
          }

          // Custom Name Input Field when CUSTOM is selected
          AnimatedVisibility(
            visible = settings.persona == AssistantPersona.CUSTOM,
            enter = fadeIn(),
            exit = fadeOut()
          ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
              Text(
                text = "Enter Custom Assistant Name:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = uiState.customNameDraft,
                  onValueChange = { viewModel.onCustomNameDraftChanged(it) },
                  placeholder = { Text("e.g. Jarvis, Nova, Aria...") },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("custom_name_input"),
                  shape = RoundedCornerShape(14.dp),
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                  keyboardActions = KeyboardActions(onDone = { viewModel.onSaveCustomName() })
                )

                Button(
                  onClick = { viewModel.onSaveCustomName() },
                  shape = RoundedCornerShape(14.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier.testTag("save_custom_name_button")
                ) {
                  Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Apply")
                }
              }
            }
          }
        }
      }

      // 3. Wake Word & Voice Core Section
      item {
        SectionCard(title = "Wake-Word Voice Activation", icon = Icons.Default.Mic) {
          SettingToggleRow(
            title = "Wake-Word Voice Activation",
            subtitle = "Say '${settings.activeDisplayName}' to wake the assistant hands-free",
            icon = Icons.Default.Mic,
            checked = settings.wakeWordEnabled,
            onCheckedChange = { viewModel.onWakeWordToggled(it) },
            testTag = "wake_word_switch"
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Live Engine Status Indicator
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Engine Status:",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium
            )
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = when (uiState.wakeEngineStatus) {
                WakeEngineStatus.LISTENING -> GamakGreen.copy(alpha = 0.2f)
                WakeEngineStatus.PAUSED_FOR_MIC, WakeEngineStatus.PAUSED_FOR_TTS -> GamakAmber.copy(alpha = 0.2f)
                WakeEngineStatus.NO_PERMISSION, WakeEngineStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
              }
            ) {
              Text(
                text = uiState.wakeEngineStatus.displayLabel,
                style = MaterialTheme.typography.labelSmall,
                color = when (uiState.wakeEngineStatus) {
                  WakeEngineStatus.LISTENING -> GamakGreen
                  WakeEngineStatus.PAUSED_FOR_MIC, WakeEngineStatus.PAUSED_FOR_TTS -> GamakAmber
                  WakeEngineStatus.NO_PERMISSION, WakeEngineStatus.ERROR -> MaterialTheme.colorScheme.error
                  else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("wake_status_badge")
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Wake Word Sensitivity Slider
          Text(
            text = "Wake-Word Sensitivity: ${(settings.wakeWordSensitivity * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
          )
          Slider(
            value = settings.wakeWordSensitivity,
            onValueChange = { viewModel.onWakeWordSensitivityChanged(it) },
            valueRange = 0.2f..1.0f,
            steps = 8,
            modifier = Modifier.testTag("wake_sensitivity_slider"),
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Test Wake Word Button
          Button(
            onClick = { viewModel.onTestWakeWordTrigger() },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("test_wake_word_button")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Test Wake-Word Activation (${settings.activeDisplayName})")
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Privacy note
          Text(
            text = "🔒 Privacy Guarantee: Audio frames for wake-word detection are processed entirely on-device and never saved or transmitted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
          )
        }
      }

      // 4. Voice Language Section
      item {
        SectionCard(title = "Voice Language & Speech Recognition", icon = Icons.Default.Language) {
          Text(
            text = "Select primary speech recognition & TTS target language:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
          )

          Spacer(modifier = Modifier.height(10.dp))

          val languages = listOf(
            "auto" to "Auto Multi-Language",
            "hi-IN" to "Hindi (हिन्दी)",
            "ne-NP" to "Nepali (नेपाली)",
            "en-IN" to "English (India)"
          )

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            languages.forEach { (code, label) ->
              val isSelected = settings.voiceLanguage == code
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { viewModel.onVoiceLanguageSelected(code) }
                  .testTag("lang_option_$code")
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                  )
                  if (isSelected) {
                    Icon(
                      Icons.Default.Check,
                      contentDescription = "Selected",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 5. Theme Mode Section
      item {
        SectionCard(title = "Appearance & Theme", icon = Icons.Default.Palette) {
          Text(
            text = "Select visual presentation mode:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            AppThemeMode.entries.forEach { mode ->
              val isSelected = settings.themeMode == mode
              FilterChip(
                selected = isSelected,
                onClick = { viewModel.onThemeModeSelected(mode) },
                label = { Text(mode.title, fontSize = 12.sp) },
                leadingIcon = {
                  Icon(
                    imageVector = when (mode) {
                      AppThemeMode.SYSTEM -> Icons.Default.Palette
                      AppThemeMode.DARK -> Icons.Default.DarkMode
                      AppThemeMode.LIGHT -> Icons.Default.LightMode
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("theme_chip_${mode.name.lowercase()}"),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
              )
            }
          }
        }
      }

      // 6. Memory & Conversation Controls
      item {
        SectionCard(title = "Long-Term Memory & Data Management", icon = Icons.Default.Memory) {
          SettingToggleRow(
            title = "Personal Preferences Memory",
            subtitle = "Allow ${settings.activeDisplayName} to remember language preferences & music choices on-device",
            icon = Icons.Default.Security,
            checked = settings.memoryEnabled,
            onCheckedChange = { viewModel.onMemoryToggled(it) },
            testTag = "memory_switch"
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { viewModel.onClearConversationHistory() },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("clear_history_button")
            ) {
              Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Clear Chat", fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = { viewModel.onClearLongTermMemory() },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("clear_memory_button")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Clear Memory", fontSize = 12.sp)
            }
          }
        }
      }

      // 7. Tactile & Response Feedback
      item {
        SectionCard(title = "Experience & Feedback", icon = Icons.Default.Speed) {
          SettingToggleRow(
            title = "Haptic Tactile Feedback",
            subtitle = "Vibrate on mic gestures and wake detections",
            icon = Icons.Default.Vibration,
            checked = settings.hapticFeedbackEnabled,
            onCheckedChange = { viewModel.onHapticFeedbackToggled(it) },
            testTag = "haptic_switch"
          )

          Spacer(modifier = Modifier.height(10.dp))

          SettingToggleRow(
            title = "Auditory Wave Effects",
            subtitle = "Enable dynamic chime on wake detection",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            checked = settings.soundEffectsEnabled,
            onCheckedChange = { viewModel.onSoundEffectsToggled(it) },
            testTag = "sound_switch"
          )

          Spacer(modifier = Modifier.height(10.dp))

          SettingToggleRow(
            title = "Fast Response Engine",
            subtitle = "Optimize animation latency for instant responses",
            icon = Icons.Default.Speed,
            checked = settings.fastResponseMode,
            onCheckedChange = { viewModel.onFastResponseToggled(it) },
            testTag = "fast_response_switch"
          )
        }
      }

      // 8. Reset Defaults
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Button(
            onClick = { viewModel.onResetDefaults() },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("reset_defaults_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset All Settings to Default")
          }
        }
      }
    }
  }
}

@Composable
private fun ActivePersonaSummaryCard(
  activeName: String,
  persona: AssistantPersona
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      Brush.horizontalGradient(listOf(GamakCyan, GamakViolet))
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column {
        Text(
          text = "Active Persona: $activeName",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = persona.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 12.sp
        )
      }
    }
  }
}

@Composable
private fun SectionCard(
  title: String,
  icon: ImageVector,
  content: @Composable () -> Unit
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}

@Composable
private fun PersonaSelectionRow(
  persona: AssistantPersona,
  isSelected: Boolean,
  onSelected: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onSelected)
      .testTag("persona_option_${persona.name.lowercase()}")
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(
          text = persona.defaultName,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = persona.tagline,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.sp
        )
      }

      if (isSelected) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Selected",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

@Composable
private fun SettingToggleRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          fontSize = 14.sp
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.sp
        )
      }
    }

    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
      colors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.primary,
        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
      )
    )
  }
}
