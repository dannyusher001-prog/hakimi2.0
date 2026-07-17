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

private val DarkColorScheme =
  darkColorScheme(
    primary = SolarAmber,
    onPrimary = SolarAmberMuted,
    secondary = SoftSand,
    onSecondary = ObsidianBlack,
    tertiary = ElectricOrange,
    background = ObsidianBlack,
    surface = NordicCharcoal,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = HairlineBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryDeep,
    onPrimary = Color(0xFF381E72),
    secondary = TextMutedDark,
    onSecondary = SoftWhite,
    tertiary = AccentDeep,
    background = SoftWhite,
    surface = PaperCard,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = SoftWhite,
    onSurfaceVariant = TextMutedDark,
    outline = BorderLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to preserve the brand identity
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
