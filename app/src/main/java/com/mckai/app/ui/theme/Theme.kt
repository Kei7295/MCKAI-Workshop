package com.mckai.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

// ================================================================
// Pinguo design tokens —— Apple HIG 锚点色板
// 来源：Apple Copy-1/colors_and_type.css（brand / background / text / state）
// ================================================================

// --brand-
private val Brand50 = Color(0xFFE8F2FF)
private val Brand100 = Color(0xFFCFE5FF)
private val Brand400 = Color(0xFF2E8DFF)  // dark primary
private val Brand500 = Color(0xFF007AFF)  // light primary

// --background-
private val Bg50 = Color(0xFFFFFFFF)
private val Bg100 = Color(0xFFF7F7FA)
private val Bg200 = Color(0xFFF2F2F7)     // light grouped bg
private val Bg300 = Color(0xFFE5E5EA)     // light border / separator
private val Bg400 = Color(0xFFD1D1D6)     // light input
private val Bg700 = Color(0xFF3A3A3C)     // dark border / input
private val Bg800 = Color(0xFF1C1C1E)     // dark card
private val Bg900 = Color(0xFF000000)     // dark bg

// --text-
private val Text50 = Color(0xFFF5F5F7)    // dark foreground
private val Text400 = Color(0xFF8E8E93)   // secondary, both themes
private val Text800 = Color(0xFF1D1D1F)   // light foreground

// --state-
private val Success = Color(0xFF34C759)
private val SuccessDark = Color(0xFF30D158)
private val Error = Color(0xFFFF3B30)
private val ErrorDark = Color(0xFFFF453A)
private val ErrorSurface = Color(0xFFFFECEA)

/** 徽章/图表辅助色（保留，克制使用） */
private val ChartOrange = Color(0xFFFF9500)
private val ChartPurple = Color(0xFF5856D6)

// ================================================================
// Typography —— DM Sans，数字/技术场景用 JetBrains Mono
// 规范：body 14–16px / 400 / 1.55；hero 72–96px/600/-0.04em（App 内不达此量级）
// ================================================================

private val AppleTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = (-0.4).sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.3).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 25.sp, letterSpacing = (-0.1).sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 23.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppleFonts.Sans, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp
    )
)

// ================================================================
// 形状 —— 规范 --radius: 1.2rem ≈ 19dp；按钮全 pill
// ================================================================

private val AppleShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(19.dp),
    extraLarge = RoundedCornerShape(27.dp)
)

// ================================================================
// 色板（语义 token 对齐规范 :root 与 .dark）
// ================================================================

private val AppleDarkColors = darkColorScheme(
    primary = Brand400,
    onPrimary = Bg900,
    primaryContainer = Color(0xFF0A3D6B),
    onPrimaryContainer = Brand100,
    secondary = SuccessDark,
    onSecondary = Bg900,
    tertiary = ChartOrange,
    onTertiary = Bg900,
    background = Bg900,
    onBackground = Text50,
    surface = Bg800,
    onSurface = Text50,
    surfaceVariant = Bg800,
    onSurfaceVariant = Text400,
    surfaceContainerLow = Bg800,
    surfaceContainer = Bg800,
    surfaceContainerHigh = Bg700,
    surfaceContainerHighest = Bg700,
    error = ErrorDark,
    onError = Bg900,
    errorContainer = Color(0xFF4A1F1C),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Bg700,
    outlineVariant = Bg700,
    scrim = Bg900
)

private val AppleLightColors = lightColorScheme(
    primary = Brand500,
    onPrimary = Bg50,
    primaryContainer = Brand100,
    onPrimaryContainer = Color(0xFF00275A),
    secondary = Success,
    onSecondary = Bg50,
    tertiary = ChartOrange,
    onTertiary = Bg50,
    background = Bg200,
    onBackground = Text800,
    surface = Bg50,
    onSurface = Text800,
    surfaceVariant = Bg100,
    onSurfaceVariant = Text400,
    surfaceContainerLowest = Bg50,
    surfaceContainerLow = Bg50,
    surfaceContainer = Bg100,
    surfaceContainerHigh = Bg100,
    surfaceContainerHighest = Bg100,
    error = Error,
    onError = Bg50,
    errorContainer = ErrorSurface,
    onErrorContainer = Color(0xFFD70015),
    outline = Bg300,
    outlineVariant = Bg300,
    scrim = Bg900
)

@Composable
fun MCKAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) AppleDarkColors else AppleLightColors,
        typography = AppleTypography,
        shapes = AppleShapes,
        content = content
    )
}