package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CloukeCobalt,
    onPrimary = Color.White,
    primaryContainer = CloukeCobaltContainer,
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = CloukeSlateSteel,
    onSecondary = Color.White,
    secondaryContainer = CloukeLightElevated,
    onSecondaryContainer = CloukeLightTextPrimary,
    background = CloukeLightBg,
    onBackground = CloukeLightTextPrimary,
    surface = CloukeLightBg,
    onSurface = CloukeLightTextPrimary,
    surfaceVariant = CloukeLightSecondary,
    onSurfaceVariant = CloukeLightTextSecondary,
    surfaceContainer = CloukeLightElevated,
    outline = CloukeLightBorder,
    outlineVariant = CloukeLightBorderSubtle,
    error = CloukeError,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = CloukeCobaltLight,
    onPrimary = Color.White,
    primaryContainer = CloukeCobaltDarkContainer,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = CloukeSlateSteel,
    onSecondary = Color.White,
    secondaryContainer = CloukeDarkElevated,
    onSecondaryContainer = CloukeDarkTextPrimary,
    background = CloukeDarkBg,
    onBackground = CloukeDarkTextPrimary,
    surface = CloukeDarkBg,
    onSurface = CloukeDarkTextPrimary,
    surfaceVariant = CloukeDarkSecondary,
    onSurfaceVariant = CloukeDarkTextSecondary,
    surfaceContainer = CloukeDarkElevated,
    outline = CloukeDarkBorder,
    outlineVariant = CloukeDarkBorderSubtle,
    error = CloukeError,
    onError = Color.White
)

@Composable
fun CloukeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
