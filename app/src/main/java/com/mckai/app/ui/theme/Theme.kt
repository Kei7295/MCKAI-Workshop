package com.mckai.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Grass = Color(0xFF4CAF50)
private val GrassDark = Color(0xFF2E7D32)
private val GrassLight = Color(0xFF81C784)
private val DarkBg = Color(0xFF1A1C1A)
private val DarkSurface = Color(0xFF1A1C1A)
private val DarkSurfaceHigh = Color(0xFF2A2D2A)
private val LightBg = Color(0xFFF8FAF5)
private val LightSurface = Color(0xFFF8FAF5)

private val DarkColors = darkColorScheme(
    primary = GrassLight,
    onPrimary = Color.Black,
    primaryContainer = GrassDark,
    onPrimaryContainer = Color.White,
    secondary = Grass,
    onSecondary = Color.Black,
    background = DarkBg,
    onBackground = Color(0xFFE8EDE4),
    surface = DarkSurface,
    onSurface = Color(0xFFE8EDE4),
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = Color(0xFFB7C0AE),
    error = Color(0xFFEF9A9A),
    outline = Color(0xFF4A5447)
)

private val LightColors = lightColorScheme(
    primary = Grass,
    onPrimary = Color.White,
    primaryContainer = GrassLight,
    onPrimaryContainer = Color(0xFF0B2E10),
    secondary = GrassDark,
    onSecondary = Color.White,
    background = LightBg,
    onBackground = Color(0xFF1C231B),
    surface = LightSurface,
    onSurface = Color(0xFF1C231B),
    surfaceVariant = Color(0xFFE7EDE2),
    onSurfaceVariant = Color(0xFF4A544A),
    error = Color(0xFFBA1A1A),
    outline = Color(0xFF7A8578)
)

@Composable
fun MCKAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
