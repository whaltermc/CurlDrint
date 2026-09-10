package com.blockforge.installer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockforge.installer.model.PojavInstance
import com.blockforge.installer.ui.BrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BrowseViewModel,
    onSelectInstance: (PojavInstance) -> Unit,
    onOpenSettings: () -> Unit
) {
    val state = viewModel.state.collectAsState().value

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("BlockForge") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp)
        ) {
            Text("Minecraft Instances", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Choose an instance to install mods, shaders, resource packs, or worlds.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            if (state.instances.isEmpty()) {
                Text(
                    "No launcher profiles were found in .minecraft/launcher_profiles.json.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.instances, key = { it.id }) { instance ->
                        InstanceCard(
                            instance = instance,
                            selected = state.selectedInstance?.id == instance.id,
                            onClick = { onSelectInstance(instance) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstanceCard(
    instance: PojavInstance,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(instance.name, style = MaterialTheme.typography.titleLarge)
            instance.version?.let {
                Spacer(Modifier.height(3.dp))
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            instance.gameDir?.let {
                Spacer(Modifier.height(5.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
