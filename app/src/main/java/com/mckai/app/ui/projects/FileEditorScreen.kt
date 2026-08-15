package com.mckai.app.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mckai.app.ui.components.AppleNavBar
import com.mckai.app.ui.theme.AppleFonts

@Composable
fun FileEditorScreen(
    fileId: Long,
    onBack: () -> Unit,
    viewModel: FileEditorViewModel = viewModel()
) {
    LaunchedEffect(fileId) { viewModel.load(fileId) }
    val state by viewModel.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleNavBar(
            title = state.file?.fileName ?: "编辑",
            onBack = onBack,
            actions = {
                if (!state.saved) {
                    IconButton(onClick = { viewModel.save() }) {
                        Icon(
                            Icons.Filled.Save,
                            "保存",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        )
        // 多行编辑器：external scrolling（BasicTextField 支持光标随滚动同步）
        BasicTextField(
            value = state.content,
            onValueChange = { viewModel.updateContent(it) },
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = AppleFonts.Mono,
                fontSize = 13.sp,
                lineHeight = 19.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )
    }
}