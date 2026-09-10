package com.blockforge.installer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.blockforge.installer.model.FileResult
import com.blockforge.installer.model.ProjectResult
import com.blockforge.installer.ui.BrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallScreen(
    viewModel: BrowseViewModel,
    project: ProjectResult,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val category = state.category
    val instance = state.selectedInstance

    var files by remember(project) { mutableStateOf<List<FileResult>>(emptyList()) }
    var loading by remember(project) { mutableStateOf(true) }
    var error by remember(project) { mutableStateOf<String?>(null) }
    var selectedFile by remember(project) { mutableStateOf<FileResult?>(null) }

    LaunchedEffect(project) {
        loading = true
        error = null
        try {
            files = viewModel.loadFiles(project)
        } catch (t: Throwable) {
            error = t.message ?: "Could not load files for this project."
        }
        loading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(project.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text(
                "${category.label} • ${instance?.name ?: "No instance selected"}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                project.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text("Choose a version to install:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            when {
                loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                files.isEmpty() -> Text(
                    "No downloadable files found for this project.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files, key = { it.fileId }) { file ->
                        FileRow(
                            file = file,
                            selected = selectedFile == file,
                            onSelect = {
                                selectedFile = file
                                viewModel.choosePendingFile(file)
                            }
                        )
                    }
                }
            }

            if (files.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Install into ${category.targetSubfolder}/ automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.installToSelectedInstance() },
                    enabled = selectedFile != null && instance != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Install to ${instance?.name ?: "instance"}")
                }
            }
        }
    }
}

@Composable
private fun FileRow(file: FileResult, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(file.displayName, color = MaterialTheme.colorScheme.onSurface)
                Text(file.fileName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatSize(file.sizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
    bytes >= 1_024 -> String.format("%.1f KB", bytes / 1_024.0)
    else -> "$bytes B"
}
