package com.mckai.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Apple-style color palette
private val AppleBlue = Color(0xFF007AFF)
private val AppleBlueDark = Color(0xFF0A84FF)
private val AppleGreen = Color(0xFF34C759)
private val AppleOrange = Color(0xFFFF9500)
private val AppleRed = Color(0xFFFF3B30)
private val ApplePurple = Color(0xFFAF52DE)
private val ApplePink = Color(0xFFFF2D55)
private val AppleTeal = Color(0xFF5AC8FA)

// Dark theme - true black like Apple
private val AppleDarkBg = Color(0xFF000000)
private val AppleDarkSurface = Color(0xFF1C1C1E)
private val AppleDarkSurfaceHigh = Color(0xFF2C2C2E)
private val AppleDarkSurfaceMid = Color(0xFF3A3A3C)
private val AppleDarkText = Color(0xFFF2F2F7)
private val AppleDarkTextSecondary = Color(0xFF8E8E93)
private val AppleDarkSeparator = Color(0xFF38383A)

// Light theme - Apple white
private val AppleLightBg = Color(0xFFF2F2F7)
private val AppleLightSurface = Color(0xFFFFFFFF)
private val AppleLightSurfaceHigh = Color(0xFFF2F2F7)
private val AppleLightText = Color(0xFF000000)
private val AppleLightTextSecondary = Color(0xFF8E8E93)
private val AppleLightSeparator = Color(0xFFC6C6C8)

private val AppleDarkColors = darkColorScheme(
    primary = AppleBlueDark,
    onPrimary = Color.White,
    primaryContainer = AppleBlue,
    onPrimaryContainer = Color.White,
    secondary = AppleGreen,
    onSecondary = Color.Black,
    tertiary = AppleOrange,
    onTertiary = Color.Black,
    background = AppleDarkBg,
    onBackground = AppleDarkText,
    surface = AppleDarkSurface,
    onSurface = AppleDarkText,
    surfaceVariant = AppleDarkSurfaceHigh,
    onSurfaceVariant = AppleDarkTextSecondary,
    surfaceContainerLow = AppleDarkSurface,
    surfaceContainer = AppleDarkSurfaceHigh,
    surfaceContainerHigh = AppleDarkSurfaceMid,
    error = AppleRed,
    onError = Color.White,
    outline = AppleDarkSeparator,
    outlineVariant = AppleDarkSurfaceMid
)

private val AppleLightColors = lightColorScheme(
    primary = AppleBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = AppleGreen,
    onSecondary = Color.Black,
    tertiary = AppleOrange,
    onTertiary = Color.Black,
    background = AppleLightBg,
    onBackground = AppleLightText,
    surface = AppleLightSurface,
    onSurface = AppleLightText,
    surfaceVariant = AppleLightSurfaceHigh,
    onSurfaceVariant = AppleLightTextSecondary,
    surfaceContainerLow = AppleLightSurface,
    surfaceContainer = AppleLightSurfaceHigh,
    surfaceContainerHigh = Color(0xFFE5E5EA),
    error = AppleRed,
    onError = Color.White,
    outline = AppleLightSeparator,
    outlineVariant = Color(0xFFD1D1D6)
)

@Composable
fun MCKAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) AppleDarkColors else AppleLightColors,
        content = content
    )
}
