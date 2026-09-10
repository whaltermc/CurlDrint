package com.blockforge.installer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SetupScreen(
    errorMessage: String?,
    onChooseFolder: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Set up BlockForge", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Select the Amethyst, Mojo, or PojavLauncher folder that contains .minecraft and custom_instances. BlockForge will read .minecraft/launcher_profiles.json to find your instances.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onChooseFolder) {
                Text("Select launcher folder")
            }
        }
    }
}
