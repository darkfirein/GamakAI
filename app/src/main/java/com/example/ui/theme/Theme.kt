package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val GamakDarkColorScheme =
  darkColorScheme(
    primary = GamakCyan,
    onPrimary = Color(0xFF003830),
    primaryContainer = Color(0xFF005146),
    onPrimaryContainer = GamakCyan,
    secondary = GamakBlue,
    onSecondary = Color(0xFF00344D),
    secondaryContainer = Color(0xFF004C6E),
    onSecondaryContainer = Color(0xFFC7E7FF),
    tertiary = GamakViolet,
    onTertiary = Color(0xFF38006B),
    tertiaryContainer = Color(0xFF5A1E96),
    onTertiaryContainer = Color(0xFFECD8FF),
    background = GamakDarkBackground,
    onBackground = GamakDarkTextPrimary,
    surface = GamakDarkSurface,
    onSurface = GamakDarkTextPrimary,
    surfaceVariant = GamakDarkSurfaceVariant,
    onSurfaceVariant = GamakDarkTextSecondary,
    outline = GamakDarkBorder,
    outlineVariant = Color(0xFF1E293B),
    error = GamakError,
    onError = Color.White,
  )

val GamakLightColorScheme =
  lightColorScheme(
    primary = Color(0xFF007E6F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF4E5),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF0277BD),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCBE6FF),
    onSecondaryContainer = Color(0xFF001E30),
    tertiary = Color(0xFF7E3ABF),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE9D8FD),
    onTertiaryContainer = Color(0xFF280053),
    background = GamakLightBackground,
    onBackground = GamakLightTextPrimary,
    surface = GamakLightSurface,
    onSurface = GamakLightTextPrimary,
    surfaceVariant = GamakLightSurfaceVariant,
    onSurfaceVariant = GamakLightTextSecondary,
    outline = GamakLightBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
  )

enum class AppThemeMode(val title: String) {
  SYSTEM("System Default"),
  DARK("Futuristic Dark"),
  LIGHT("Modern Light")
}

@Composable
fun GamakTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    AppThemeMode.DARK -> true
    AppThemeMode.LIGHT -> false
  }

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      isDark -> GamakDarkColorScheme
      else -> GamakLightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

