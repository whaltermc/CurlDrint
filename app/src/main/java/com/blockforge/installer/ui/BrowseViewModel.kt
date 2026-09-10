package com.blockforge.installer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blockforge.installer.model.Category
import com.blockforge.installer.model.FileResult
import com.blockforge.installer.model.PojavInstance
import com.blockforge.installer.model.ProjectResult
import com.blockforge.installer.model.Source
import com.blockforge.installer.network.ContentRepository
import com.blockforge.installer.saf.InstallResult
import com.blockforge.installer.saf.PojavInstanceRepository
import com.blockforge.installer.saf.SafInstaller
import com.blockforge.installer.util.PrefsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class LoadState { IDLE, LOADING, ERROR }

data class BrowseUiState(
    val source: Source = Source.MODRINTH,
    val category: Category = Category.MODS,
    val query: String = "",
    val results: List<ProjectResult> = emptyList(),
    val loadState: LoadState = LoadState.IDLE,
    val errorMessage: String? = null,
    val installMessage: String? = null,
    val curseForgeApiKey: String = "",
    val launcherFolderConfigured: Boolean = false,
    val launcherLoading: Boolean = false,
    val instances: List<PojavInstance> = emptyList(),
    val selectedInstance: PojavInstance? = null
)

class BrowseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContentRepository()
    private val prefs = PrefsRepository(application)
    private val instanceRepository = PojavInstanceRepository(application)

    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state

    var pendingProject: ProjectResult? = null
        private set
    var pendingFiles: List<FileResult> = emptyList()
        private set
    var pendingFile: FileResult? = null
        private set

    init {
        viewModelScope.launch {
            prefs.curseForgeApiKeyFlow.first()?.let { key ->
                _state.value = _state.value.copy(curseForgeApiKey = key)
            }
            loadSavedLauncherFolder()
        }
    }

    private suspend fun loadSavedLauncherFolder() {
        val saved = prefs.launcherTreeUriFlow.first() ?: return
        val uri = Uri.parse(saved)
        if (!instanceRepository.canAccessLauncherFolder(uri)) {
            _state.value = _state.value.copy(launcherFolderConfigured = false)
            return
        }
        loadLauncherFolder(uri, save = false)
    }

    fun loadLauncherFolder(uri: Uri, save: Boolean = true) {
        viewModelScope.launch {
            _state.value = _state.value.copy(launcherLoading = true, errorMessage = null)
            try {
                if (!instanceRepository.canAccessLauncherFolder(uri)) {
                    throw IllegalArgumentException(
                        "That folder does not contain .minecraft/launcher_profiles.json. Select the Amethyst, Mojo, or PojavLauncher folder that contains .minecraft and custom_instances."
                    )
                }
                val instances = instanceRepository.readProfiles(uri)
                val selectedId = prefs.selectedInstanceIdFlow.first()
                val selected = instances.firstOrNull { it.id == selectedId }
                if (save) {
                    prefs.setLauncherTreeUri(uri.toString())
                }
                _state.value = _state.value.copy(
                    launcherFolderConfigured = true,
                    launcherLoading = false,
                    instances = instances,
                    selectedInstance = selected ?: instances.firstOrNull()
                )
                if (selected == null && instances.isNotEmpty()) {
                    prefs.setSelectedInstanceId(instances.first().id)
                }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    launcherLoading = false,
                    launcherFolderConfigured = false,
                    errorMessage = t.message ?: "Could not read launcher_profiles.json."
                )
            }
        }
    }

    fun selectInstance(instance: PojavInstance) {
        _state.value = _state.value.copy(selectedInstance = instance)
        viewModelScope.launch { prefs.setSelectedInstanceId(instance.id) }
    }

    fun setSource(source: Source) {
        _state.value = _state.value.copy(source = source)
        search()
    }

    fun setCategory(category: Category) {
        _state.value = _state.value.copy(category = category, results = emptyList())
        if (_state.value.query.isNotBlank()) search()
    }

    fun setQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun saveCurseForgeApiKey(key: String) {
        _state.value = _state.value.copy(curseForgeApiKey = key)
        viewModelScope.launch { prefs.setCurseForgeApiKey(key) }
    }

    fun search() {
        val s = _state.value
        viewModelScope.launch {
            _state.value = s.copy(loadState = LoadState.LOADING, errorMessage = null)
            try {
                val results = repository.search(s.source, s.category, s.query, s.curseForgeApiKey)
                _state.value = _state.value.copy(results = results, loadState = LoadState.IDLE)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    loadState = LoadState.ERROR,
                    errorMessage = t.message ?: "Something went wrong while searching."
                )
            }
        }
    }

    suspend fun loadFiles(project: ProjectResult): List<FileResult> {
        pendingProject = project
        val files = repository.files(project, _state.value.curseForgeApiKey)
        pendingFiles = files
        return files
    }

    fun choosePendingFile(file: FileResult) {
        pendingFile = file
    }

    fun installToSelectedInstance() {
        val file = pendingFile ?: return
        val instance = _state.value.selectedInstance ?: run {
            _state.value = _state.value.copy(installMessage = "Select a Minecraft instance first.")
            return
        }
        viewModelScope.launch {
            val rootUriString = prefs.launcherTreeUriFlow.first()
            if (rootUriString == null) {
                _state.value = _state.value.copy(installMessage = "Launcher folder permission is missing. Please select it again.")
                return@launch
            }
            val rootUri = Uri.parse(rootUriString)
            val gameDir = instanceRepository.resolveGameDir(rootUri, instance)
            if (gameDir == null) {
                _state.value = _state.value.copy(
                    installMessage = "Could not find the instance game folder: ${instance.gameDir ?: ".minecraft"}"
                )
                return@launch
            }

            _state.value = _state.value.copy(installMessage = "Downloading \"${file.fileName}\"...")
            val result = SafInstaller.downloadAndInstallToDirectory(
                context = getApplication(),
                directory = gameDir,
                downloadUrl = file.downloadUrl,
                fileName = file.fileName,
                extractIfArchive = _state.value.category == Category.WORLDS,
                targetSubfolder = _state.value.category.targetSubfolder
            )
            _state.value = _state.value.copy(
                installMessage = when (result) {
                    is InstallResult.Success -> result.message
                    is InstallResult.Failure -> "❌ ${result.message}"
                }
            )
        }
    }

    fun clearInstallMessage() {
        _state.value = _state.value.copy(installMessage = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun clearPending() {
        pendingProject = null
        pendingFiles = emptyList()
        pendingFile = null
    }
}
