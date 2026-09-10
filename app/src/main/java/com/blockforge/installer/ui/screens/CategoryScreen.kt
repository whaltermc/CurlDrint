package com.blockforge.installer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockforge.installer.model.Category
import com.blockforge.installer.model.PojavInstance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    instance: PojavInstance,
    onBack: () -> Unit,
    onCategory: (Category) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(instance.name) },
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
            Text("What do you want to install?", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Category.values().forEach { category ->
                Button(
                    onClick = { onCategory(category) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(category.label)
                }
            }
        }
    }
}
