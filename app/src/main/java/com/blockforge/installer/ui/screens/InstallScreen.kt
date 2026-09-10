package com.blockforge.installer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import com.blockforge.installer.model.PojavInstance
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
    val state by viewModel.state.collectAsState()
    val category = state.category

    var files by remember { mutableStateOf<List<FileResult>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedFile by remember { mutableStateOf<FileResult?>(null) }
    var extractArchive by remember { mutableStateOf(category == com.blockforge.installer.model.Category.WORLDS) }
    var showInstances by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.loadPojavInstances(uri)
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
            Text(project.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            Text("Minecraft instance", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (!state.pojavRootConfigured || state.pojavInstances.isEmpty()) {
                OutlinedButton(
                    onClick = { folderPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Select PojavLauncher folder")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Select the folder containing Pojav's profiles.json. BlockForge will remember it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Box {
                    OutlinedButton(
                        onClick = { showInstances = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(state.selectedPojavInstance?.name ?: "Select instance", modifier = Modifier.weight(1f))
                        Text("▼")
                    }
                    DropdownMenu(
                        expanded = showInstances,
                        onDismissRequest = { showInstances = false }
                    ) {
                        state.pojavInstances.forEach { instance ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(instance.name)
                                        instance.version?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectPojavInstance(instance)
                                    showInstances = false
                                }
                            )
                        }
                    }
                }
            }

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
                        FileRow(file, selectedFile == file) {
                            selectedFile = file
                            viewModel.choosePendingFile(file)
                        }
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
                        "Extract archive",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        state.selectedPojavInstance?.let {
                            viewModel.installToPojavInstance(it, extractArchive)
                        }
                    },
                    enabled = selectedFile != null && state.selectedPojavInstance != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        state.selectedPojavInstance?.let { "Install to ${it.name}" }
                            ?: "Select an instance first"
                    )
                }
            }

            state.installMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(msg, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Modifier.fillMaxWidth().padding(12.dp).clickable(onClick = onSelect),
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
