package com.blockforge.installer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.blockforge.installer.model.Category
import com.blockforge.installer.model.ProjectResult
import com.blockforge.installer.model.PojavInstance
import com.blockforge.installer.ui.BrowseViewModel
import com.blockforge.installer.ui.screens.BrowseScreen
import com.blockforge.installer.ui.screens.CategoryScreen
import com.blockforge.installer.ui.screens.HomeScreen
import com.blockforge.installer.ui.screens.InstallScreen
import com.blockforge.installer.ui.screens.SettingsScreen
import com.blockforge.installer.ui.screens.SetupScreen
import com.blockforge.installer.ui.theme.BlockForgeTheme

private sealed class Screen {
    data object Home : Screen()
    data class Categories(val instance: PojavInstance) : Screen()
    data class Browse(val category: Category) : Screen()
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
                    val state by viewModel.state.collectAsState()
                    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                    var pickerShown by remember { mutableStateOf(false) }

                    val launcherPicker = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocumentTree()
                    ) { uri ->
                        pickerShown = false
                        if (uri != null) {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                            viewModel.loadLauncherFolder(uri)
                        }
                    }

                    // First launch, or after the previously granted SAF permission disappears.
                    LaunchedEffect(state.launcherFolderConfigured, state.launcherLoading) {
                        if (!state.launcherFolderConfigured && !state.launcherLoading && !pickerShown) {
                            pickerShown = true
                            launcherPicker.launch(null)
                        }
                    }

                    if (state.launcherFolderConfigured) {
                        when (val current = screen) {
                            Screen.Home -> HomeScreen(
                                viewModel = viewModel,
                                onSelectInstance = {
                                    viewModel.selectInstance(it)
                                    screen = Screen.Categories(it)
                                },
                                onOpenSettings = { screen = Screen.Settings }
                            )
                            is Screen.Categories -> CategoryScreen(
                                instance = current.instance,
                                onBack = { screen = Screen.Home },
                                onCategory = {
                                    viewModel.setCategory(it)
                                    screen = Screen.Browse(it)
                                }
                            )
                            is Screen.Browse -> BrowseScreen(
                                viewModel = viewModel,
                                category = current.category,
                                onBack = {
                                    val instance = state.selectedInstance
                                    screen = if (instance != null) Screen.Categories(instance) else Screen.Home
                                },
                                onOpenProject = { screen = Screen.Install(it) },
                                onOpenSettings = { screen = Screen.Settings }
                            )
                            is Screen.Install -> InstallScreen(
                                viewModel = viewModel,
                                project = current.project,
                                onBack = {
                                    viewModel.clearPending()
                                    screen = Screen.Browse(state.category)
                                }
                            )
                            Screen.Settings -> SettingsScreen(
                                viewModel = viewModel,
                                onBack = { screen = Screen.Home },
                                onChooseLauncherFolder = {
                                    pickerShown = true
                                    launcherPicker.launch(null)
                                }
                            )
                        }
                    } else {
                        SetupScreen(
                            errorMessage = state.errorMessage,
                            onChooseFolder = {
                                pickerShown = true
                                launcherPicker.launch(null)
                            }
                        )
                    }
                }
            }
        }
    }
}
