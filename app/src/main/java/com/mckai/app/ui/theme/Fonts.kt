package com.mckai.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mckai.app.R

/**
 * Pinguo/Apple 字体体系：
 *   --font-sans: DM Sans  （界面 + 标题）
 *   --font-mono: JetBrains Mono （技术细节 / 等宽代码块）
 */
object AppleFonts {
    val Sans = FontFamily(
        Font(R.font.dm_sans_regular, FontWeight.Normal),
        Font(R.font.dm_sans_medium, FontWeight.Medium),
        Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
        Font(R.font.dm_sans_bold, FontWeight.Bold)
    )

    val Mono = FontFamily(Font(R.font.jetbrains_mono_regular, FontWeight.Normal))
}