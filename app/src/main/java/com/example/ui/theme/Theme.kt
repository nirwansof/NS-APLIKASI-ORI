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
    primary = SophisticatedPrimary,
    secondary = SophisticatedSecondary,
    tertiary = SophisticatedTertiary,
    background = SophisticatedBackgroundDark,
    surface = SophisticatedSurfaceDark,
    onPrimary = SophisticatedOnPrimaryDark,
    onSecondary = SophisticatedOnPrimaryDark,
    onBackground = SophisticatedOnSurfaceDark,
    onSurface = SophisticatedOnSurfaceDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SophisticatedPrimaryLight,
    secondary = SophisticatedSecondaryLight,
    tertiary = SophisticatedTertiaryLight,
    background = SophisticatedBackgroundLight,
    surface = SophisticatedSurfaceLight,
    onPrimary = SophisticatedOnPrimaryLight,
    onSecondary = SophisticatedOnPrimaryLight,
    onBackground = SophisticatedOnSurfaceLight,
    onSurface = SophisticatedOnSurfaceLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
