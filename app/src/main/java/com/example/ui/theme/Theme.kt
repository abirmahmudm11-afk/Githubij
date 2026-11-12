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

private val LightColorScheme = lightColorScheme(
    primary = CloukeEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F7F2),
    onPrimaryContainer = Color(0xFF00382B),
    secondary = CloukeCobalt,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF6FF),
    onSecondaryContainer = Color(0xFF1E3A8A),
    background = CloukeWhiteBg,
    onBackground = CloukeInkPrimary,
    surface = CloukeWhiteBg,
    onSurface = CloukeInkPrimary,
    surfaceVariant = CloukeOffWhite,
    onSurfaceVariant = CloukeInkSecondary,
    surfaceContainer = CloukeSurfaceElevated,
    outline = CloukeBorder,
    outlineVariant = CloukeBorderSubtle,
    error = CloukeRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = CloukeEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF00513F),
    onPrimaryContainer = Color(0xFF75F8D1),
    secondary = CloukeCobalt,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1D4ED8),
    onSecondaryContainer = Color(0xFFDBEAFE),
    background = CloukeDarkBg,
    onBackground = CloukeDarkInkPrimary,
    surface = CloukeDarkSurface,
    onSurface = CloukeDarkInkPrimary,
    surfaceVariant = CloukeDarkSurfaceElevated,
    onSurfaceVariant = CloukeDarkInkSecondary,
    surfaceContainer = CloukeDarkSurfaceElevated,
    outline = CloukeDarkBorder,
    outlineVariant = Color(0xFF3F3F46),
    error = CloukeRed,
    onError = Color.White
)

@Composable
fun CloukeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep CLOUKE branded white/dark aesthetic by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
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
