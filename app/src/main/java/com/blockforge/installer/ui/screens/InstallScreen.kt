package com.blockforge.installer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.blockforge.installer.model.FileResult
import com.blockforge.installer.model.ProjectResult
import com.blockforge.installer.ui.BrowseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallScreen(
    viewModel: BrowseViewModel,
    project: ProjectResult,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val category = viewModel.state.collectAsState().value.category

    var files by remember { mutableStateOf<List<FileResult>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedFile by remember { mutableStateOf<FileResult?>(null) }
    var extractArchive by remember {
        mutableStateOf(category.name == "RESOURCE_PACKS" || category.name == "SHADERS")
    }
    var rememberedFolder by remember { mutableStateOf<Uri?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            scope.launch { viewModel.rememberFolderUri(category, uri) }
            viewModel.installTo(uri, extractArchive)
        }
    }

    LaunchedEffect(project) {
        loading = true
        error = null
        try {
            files = viewModel.loadFiles(project)
        } catch (t: Throwable) {
            error = t.message ?: "Could not load files for this project."
        }
        loading = false
        rememberedFolder = viewModel.rememberedFolderUri(category)?.let { Uri.parse(it) }
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
                files.isEmpty() -> Text("No downloadable files found for this project.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files) { file ->
                        FileRow(
                            file = file,
                            selected = selectedFile == file,
                            onSelect = { selectedFile = file; viewModel.choosePendingFile(file) }
                        )
                    }
                }
            }

            if (files.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = extractArchive,
                        onCheckedChange = { extractArchive = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        "Extract into a folder (recommended for resource packs / shaders)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))

                rememberedFolder?.let {
                    Text(
                        "Last used folder is remembered for ${category.label}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        if (selectedFile == null) return@Button
                        val remembered = rememberedFolder
                        if (remembered != null) {
                            viewModel.installTo(remembered, extractArchive)
                        } else {
                            folderPicker.launch(null)
                        }
                    },
                    enabled = selectedFile != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Install to selected folder")
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { folderPicker.launch(null) }) {
                    Text("Choose a different folder…", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .then(Modifier.clickableRow(onSelect)),
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

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
