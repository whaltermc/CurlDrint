package com.blockforge.installer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.blockforge.installer.model.ProjectResult
import com.blockforge.installer.ui.BrowseViewModel
import com.blockforge.installer.ui.screens.BrowseScreen
import com.blockforge.installer.ui.screens.InstallScreen
import com.blockforge.installer.ui.screens.SettingsScreen
import com.blockforge.installer.ui.theme.BlockForgeTheme

private sealed class Screen {
    data object Browse : Screen()
    data class Install(val project: ProjectResult) : Screen()
    data object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: BrowseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlockForgeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf<Screen>(Screen.Browse) }

                    when (val current = screen) {
                        is Screen.Browse -> BrowseScreen(
                            viewModel = viewModel,
                            onOpenProject = { screen = Screen.Install(it) },
                            onOpenSettings = { screen = Screen.Settings }
                        )
                        is Screen.Install -> InstallScreen(
                            viewModel = viewModel,
                            project = current.project,
                            onBack = {
                                viewModel.clearPending()
                                screen = Screen.Browse
                            }
                        )
                        is Screen.Settings -> SettingsScreen(
                            viewModel = viewModel,
                            onBack = { screen = Screen.Browse }
                        )
                    }
                }
            }
        }
    }
}
