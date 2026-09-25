package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = LibrusDarkPrimary,
    onPrimary = LibrusDarkOnPrimary,
    primaryContainer = LibrusDarkPrimaryContainer,
    onPrimaryContainer = LibrusDarkOnPrimaryContainer,
    secondary = LibrusDarkSecondary,
    onSecondary = LibrusDarkOnSecondary,
    secondaryContainer = LibrusDarkSecondaryContainer,
    onSecondaryContainer = LibrusDarkOnSecondaryContainer,
    tertiary = LibrusDarkTertiary,
    onTertiary = LibrusDarkOnTertiary,
    background = LibrusDarkBackground,
    onBackground = LibrusDarkOnBackground,
    surface = LibrusDarkSurface,
    onSurface = LibrusDarkOnSurface,
    surfaceVariant = LibrusDarkSurfaceVariant,
    outline = LibrusDarkOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LibrusNavyPrimary,
    onPrimary = LibrusNavyOnPrimary,
    primaryContainer = LibrusNavyContainer,
    onPrimaryContainer = LibrusNavyOnContainer,
    secondary = LibrusAmberSecondary,
    onSecondary = LibrusAmberOnSecondary,
    secondaryContainer = LibrusAmberContainer,
    onSecondaryContainer = LibrusAmberOnContainer,
    tertiary = LibrusTealTertiary,
    onTertiary = LibrusTealOnTertiary,
    background = LibrusLightBackground,
    onBackground = LibrusLightOnBackground,
    surface = LibrusLightSurface,
    onSurface = LibrusLightOnSurface,
    surfaceVariant = LibrusLightSurfaceVariant,
    outline = LibrusLightOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Set to false to keep rich Librus branding consistent
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

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
