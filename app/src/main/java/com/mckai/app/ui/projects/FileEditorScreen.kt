package com.mckai.app.ui.projects

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileEditorScreen(
    fileId: Long,
    onBack: () -> Unit,
    viewModel: FileEditorViewModel = viewModel()
) {
    LaunchedEffect(fileId) { viewModel.load(fileId) }
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.file?.fileName ?: "编辑") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                actions = {
                    if (!state.saved) {
                        IconButton(onClick = { viewModel.save() }) { Icon(Icons.Filled.Save, "保存") }
                    }
                }
            )
        }
    ) { padding ->
        OutlinedTextField(
            value = state.content,
            onValueChange = { viewModel.updateContent(it) },
            modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            singleLine = false
        )
    }
}
