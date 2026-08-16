package com.mckai.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mckai.app.ui.theme.AppleFonts
import kotlinx.coroutines.launch

/**
 * Shared Apple-inspired (Pinguo) component kit.
 * 规范对齐（Apple Copy-1）：
 *  - --radius: 1.2rem ≈ 19dp，按钮全 pill
 *  - 卡片：1px 边框即边界，阴影仅浮层用
 *  - 输入：44dp 高、静默背景、聚焦靠 ring（1.5dp 蓝环）
 *  - 触控目标 ≥44dp
 *  - 动效：cubic-bezier(0.32, 0.72, 0, 1)，150/250/350ms
 *  - 字体：DM Sans（AppleFonts.Sans），技术细节 JetBrains Mono
 */

/** 规范动效曲线 */
val AppleEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/* ---------- 导航 ---------- */

/** iOS 风格导航栏：半透明背景、hairline 分隔线、居中标题。 */
@Composable
fun AppleNavBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.background.copy(alpha = 0.88f)
    val separator = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

    Column(modifier = modifier.fillMaxWidth().background(bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        "返回",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppleFonts.Sans,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        fontFamily = AppleFonts.Sans,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Row(
                modifier = Modifier,
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions()
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(separator)
        )
    }
}

/** 根标签页大标题（iOS large-title 风格）。 */
@Composable
fun AppleLargeTitle(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    fontFamily = AppleFonts.Sans,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        fontSize = 13.sp,
                        fontFamily = AppleFonts.Sans,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            actions()
        }
    }
}

/* ---------- 表面 ---------- */

/** 静默卡片：1px 边框定义边界，无装饰（规范 --radius: 1.2rem）。 */
@Composable
fun AppleCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(19.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
fun AppleSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = AppleFonts.Sans,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
fun AppleRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, modifier = Modifier.size(17.dp), tint = iconTint)
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontFamily = AppleFonts.Sans, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        fontSize = 13.sp,
                        fontFamily = AppleFonts.Sans,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            } else if (onClick != null) {
                Icon(
                    Icons.Filled.ChevronRight,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = if (icon != null) 58.dp else 16.dp)
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )
        }
    }
}

/* ---------- 按钮（规范尺寸 40/48/56，全 pill） ---------- */

/** 主 CTA：品牌蓝实心胶囊（唯一主角动作）。 */
@Composable
fun ApplePrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    size: ButtonSize = ButtonSize.LARGE
) {
    val minH = when (size) {
        ButtonSize.SMALL -> 40.dp
        ButtonSize.MEDIUM -> 48.dp
        ButtonSize.LARGE -> 56.dp
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minH)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = AppleFonts.Sans,
                fontSize = when (size) {
                    ButtonSize.SMALL -> 15.sp
                    ButtonSize.MEDIUM, ButtonSize.LARGE -> 16.sp
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** 次级胶囊：静默填充，视觉克制。 */
@Composable
fun AppleSecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    size: ButtonSize = ButtonSize.LARGE
) {
    val minH = when (size) {
        ButtonSize.SMALL -> 40.dp
        ButtonSize.MEDIUM -> 48.dp
        ButtonSize.LARGE -> 56.dp
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minH)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = AppleFonts.Sans,
                fontSize = when (size) {
                    ButtonSize.SMALL -> 15.sp
                    ButtonSize.MEDIUM, ButtonSize.LARGE -> 16.sp
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** 文字导向次级动作：无描边，尾随 chevron。 */
@Composable
fun AppleTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    chevron: Boolean = true
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = MaterialTheme.colorScheme.primary, fontFamily = AppleFonts.Sans, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        if (chevron) {
            Spacer(Modifier.width(2.dp))
            Icon(
                Icons.Filled.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

enum class ButtonSize { SMALL, MEDIUM, LARGE }

/** 破坏性文字行（红、居中），用于 sheet/dialog。 */
@Composable
fun AppleDestructiveRow(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(19.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(19.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.error, fontFamily = AppleFonts.Sans, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

/* ---------- 输入（44dp，聚焦靠 ring） ---------- */

/** 圆角中性输入：静默背景 + 聚焦 1.5dp 蓝环（规范 input.json）。 */
@Composable
fun AppleField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    textStyle: TextStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 16.sp,
        fontFamily = AppleFonts.Sans
    ),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent
    val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val fieldShape = RoundedCornerShape(12.dp)

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AppleFonts.Sans,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(fieldShape)
                .background(bg)
                .border(1.5.dp, borderColor, fieldShape)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .onFocusChanged { focused = it.isFocused }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                textStyle = textStyle,
                singleLine = singleLine,
                minLines = minLines,
                keyboardOptions = keyboardOptions,
                visualTransformation = visualTransformation,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty() && placeholder != null) {
                                Text(
                                    placeholder,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    style = textStyle
                                )
                            }
                            innerTextField()
                        }
                        if (trailingIcon != null) trailingIcon()
                    }
                }
            )
        }
    }
}

/** iOS 分段控制器。 */
@Composable
fun AppleSegmented(
    options: List<Pair<String, String>>, // (value, label)
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .background(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            RoundedCornerShape(11.dp)
        )
        .padding(2.dp)
        .let { if (scrollable) it.horizontalScroll(rememberScrollState()) else it }

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = selected == value
            val elevation by animateDpAsState(
                targetValue = if (isSelected) 1.dp else 0.dp,
                animationSpec = androidx.compose.animation.core.tween(150, easing = AppleEasing),
                label = "segElev"
            )
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                shadowElevation = elevation,
                modifier = if (scrollable) {
                    Modifier
                        .heightIn(min = 36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onSelect(value) }
                } else {
                    Modifier
                        .weight(1f)
                        .heightIn(min = 36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onSelect(value) }
                }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = AppleFonts.Sans,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/* ---------- 浮层 ---------- */

/** iOS 风格 alert dialog（圆角 19dp，蓝色/红色动作）。 */
@Composable
fun AppleAlertDialog(
    title: String,
    message: String? = null,
    confirmText: String = "确定",
    destructive: Boolean = false,
    dismissText: String? = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(19.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(270.dp)
        ) {
            Column {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AppleFonts.Sans,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    if (message != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            message,
                            fontSize = 13.sp,
                            fontFamily = AppleFonts.Sans,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                    if (content != null) {
                        Spacer(Modifier.height(4.dp))
                        content()
                    }
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                if (dismissText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(dismissText, color = Color(0xFF007AFF), fontSize = 17.sp, fontFamily = AppleFonts.Sans)
                    }
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        confirmText,
                        color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AppleFonts.Sans
                    )
                }
            }
        }
    }
}

data class AppleSheetOption(
    val label: String,
    val destructive: Boolean = false,
    val bold: Boolean = false,
    val onClick: () -> Unit
)

/** iOS 风格底部动作面板（圆角 19dp 分组）。 */
@Composable
fun AppleActionSheet(
    title: String? = null,
    options: List<AppleSheetOption>,
    dismissText: String = "取消",
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            if (title != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(19.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(19.dp))
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        title,
                        fontSize = 13.sp,
                        fontFamily = AppleFonts.Sans,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(19.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(19.dp))
            ) {
                options.forEachIndexed { index, option ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .clickable {
                                option.onClick()
                                scope.launch { onDismiss() }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option.label,
                            fontSize = 17.sp,
                            fontWeight = if (option.bold) FontWeight.SemiBold else FontWeight.Normal,
                            fontFamily = AppleFonts.Sans,
                            color = if (option.destructive) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (index < options.size - 1) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(19.dp))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dismissText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppleFonts.Sans,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/* ---------- 其他 ---------- */

@Composable
fun AppleEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppleFonts.Sans,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                fontFamily = AppleFonts.Sans,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

/** 数字/技术信息专用等宽（JetBrains Mono）。 */
@Composable
fun MonoText(text: String, modifier: Modifier = Modifier, fontSize: androidx.compose.ui.unit.TextUnit = 13.sp, color: Color? = null) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize,
        color = color ?: MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

private val EMOJI_PATTERN = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]")

/**
 * 头像：字符或首字母圆形底。
 * 规范：product UI avoids emoji —— emoji 一律过滤，回退到名称首字母。
 */
@Composable
fun AppleAvatar(
    name: String,
    avatar: String?,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 17.sp,
    modifier: Modifier = Modifier
) {
    val display = avatar
        ?.takeIf { it.isNotBlank() && !EMOJI_PATTERN.containsMatchIn(it) }
        ?.take(1)
        ?: name.take(1).ifBlank { "A" }.uppercase()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            display,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun AppleListSpacing() = Spacer(Modifier.height(12.dp))