package com.example.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GamakCyan
import com.example.ui.theme.GamakViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("about_screen"),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "About Gamak AI",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("about_back_button")
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
      // 1. Hero Brand Header
      item {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(GamakCyan, GamakViolet))
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(2.dp, GamakCyan, CircleShape)
                .background(Color(0xFF0F172A)),
              contentAlignment = Alignment.Center
            ) {
              Image(
                painter = painterResource(id = R.drawable.ic_gamak_logo),
                contentDescription = "Gamak AI Logo",
                modifier = Modifier.size(64.dp)
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "GAMAK AI",
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 2.sp
            )

            Text(
              text = "Version 1.0.0 (Phase 1 Foundation)",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "Gamak AI is a futuristic, next-generation personal AI assistant engine engineered with reactive Jetpack Compose state architecture, dynamic voice micro-interactions, and customizable persona identities.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 20.sp
            )
          }
        }
      }

      // 2. Privacy & Data Ethics Policy (Crucial requirement)
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
          ),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Privacy & Local-First Commitment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            PrivacyBullet(
              icon = Icons.Default.Lock,
              title = "Zero Hardcoded Secrets",
              description = "No credentials, tokens, or API keys are hardcoded in the codebase."
            )

            Spacer(modifier = Modifier.height(8.dp))

            PrivacyBullet(
              icon = Icons.Default.Security,
              title = "Local Preferences Storage",
              description = "Custom assistant names, themes, and sound toggles are securely stored exclusively on-device via DataStore."
            )

            Spacer(modifier = Modifier.height(8.dp))

            PrivacyBullet(
              icon = Icons.Default.Info,
              title = "No Unsolicited Tracking",
              description = "Zero telemetry trackers or background profile data harvesting."
            )
          }
        }
      }

      // 3. Phase 1 Capabilities Checklist
      item {
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
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Phase 1 Implemented Architecture",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ArchFeatureItem("7-State UI Finite State Machine", "Idle, Listening, Thinking, Speaking, Confirming, Clarifying, Error")
            ArchFeatureItem("Animated AI Energy Orb", "Multi-layered particle physics, reactive rotating orbital rings")
            ArchFeatureItem("Live Equalizer Visualizer", "Dynamic audio frequency synthesis synced with state transitions")
            ArchFeatureItem("Custom Assistant Personas", "Gamak, Maya, Sathi, Mitra, plus arbitrary custom names")
            ArchFeatureItem("Material 3 Theme Engine", "Futuristic Dark, Modern Light, and System Default support")
            ArchFeatureItem("Battery-Conscious Loop", "Hardware-accelerated DrawScope animations without cpu spinning")
          }
        }
      }
    }
  }
}

@Composable
private fun PrivacyBullet(
  icon: ImageVector,
  title: String,
  description: String
) {
  Row(modifier = Modifier.fillMaxWidth()) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier
        .size(16.dp)
        .padding(top = 2.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
      )
    }
  }
}

@Composable
private fun ArchFeatureItem(
  title: String,
  detail: String
) {
  Column(modifier = Modifier.padding(vertical = 4.dp)) {
    Text(
      text = "• $title",
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.SemiBold,
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "   $detail",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 12.sp
    )
  }
}
