package com.blockforge.installer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blockforge.installer.model.Category
import com.blockforge.installer.model.FileResult
import com.blockforge.installer.model.ProjectResult
import com.blockforge.installer.model.PojavInstance
import com.blockforge.installer.model.Source
import com.blockforge.installer.network.ContentRepository
import com.blockforge.installer.saf.InstallResult
import com.blockforge.installer.saf.SafInstaller
import com.blockforge.installer.saf.PojavInstanceRepository
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
    val pojavInstances: List<PojavInstance> = emptyList(),
    val selectedPojavInstance: PojavInstance? = null,
    val pojavRootConfigured: Boolean = false
)

class BrowseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContentRepository()
    private val prefs = PrefsRepository(application)
    private val pojavRepository = PojavInstanceRepository(application)

    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state

    // Project awaiting a file choice / folder pick, kept outside state to avoid re-triggering recompositions unnecessarily.
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
            prefs.pojavRootUriFlow.first()?.let { savedUri ->
                loadPojavInstances(Uri.parse(savedUri))
            }
        }
    }


    fun loadPojavInstances(uri: Uri) {
        viewModelScope.launch {
            try {
                val instances = pojavRepository.readProfiles(uri)
                prefs.setPojavRootUri(uri.toString())
                val selectedId = prefs.selectedPojavInstanceFlow.first()
                val selected = instances.firstOrNull { it.id == selectedId }
                    ?: instances.firstOrNull()
                if (selected != null && selected.id != selectedId) {
                    prefs.setSelectedPojavInstance(selected.id)
                }
                _state.value = _state.value.copy(
                    pojavInstances = instances,
                    selectedPojavInstance = selected,
                    pojavRootConfigured = true,
                    errorMessage = null
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    pojavRootConfigured = false,
                    errorMessage = t.message ?: "Could not read PojavLauncher profiles.json."
                )
            }
        }
    }

    fun selectPojavInstance(instance: PojavInstance) {
        _state.value = _state.value.copy(selectedPojavInstance = instance)
        viewModelScope.launch { prefs.setSelectedPojavInstance(instance.id) }
    }

    fun installToPojavInstance(instance: PojavInstance, extractIfArchive: Boolean) {
        val file = pendingFile ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(installMessage = "Downloading \"${file.fileName}\"...")
            val rootString = prefs.pojavRootUriFlow.first()
            if (rootString == null) {
                _state.value = _state.value.copy(installMessage = "PojavLauncher folder is not configured.")
                return@launch
            }
            val rootUri = Uri.parse(rootString)
            val gameDir = pojavRepository.resolveGameDirectory(rootUri, instance)
            if (gameDir == null) {
                _state.value = _state.value.copy(
                    installMessage = "The selected instance folder is outside the folder you granted to BlockForge. Re-select the PojavLauncher .minecraft folder that contains the instance."
                )
                return@launch
            }
            val result = SafInstaller.downloadAndInstallInto(
                context = getApplication(),
                root = gameDir,
                downloadUrl = file.downloadUrl,
                fileName = file.fileName,
                extractIfArchive = extractIfArchive,
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


    fun setSource(source: Source) {
        _state.value = _state.value.copy(source = source)
        search()
    }

    fun setCategory(category: Category) {
        _state.value = _state.value.copy(category = category)
        search()
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

    /** Step 1 of install: fetch the list of downloadable files/versions for a project. */
    suspend fun loadFiles(project: ProjectResult): List<FileResult> {
        pendingProject = project
        val s = _state.value
        val files = repository.files(project, s.curseForgeApiKey)
        pendingFiles = files
        return files
    }

    fun choosePendingFile(file: FileResult) {
        pendingFile = file
    }

    suspend fun rememberedFolderUri(category: Category): String? =
        prefs.folderUriFlow(category).first()

    suspend fun rememberFolderUri(category: Category, uri: Uri) {
        prefs.setFolderUri(category, uri.toString())
    }

    /** Step 2 of install: given a user-chosen SAF folder, download + place the file. */
    fun installTo(folderUri: Uri, extractIfArchive: Boolean) {
        val file = pendingFile ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(installMessage = "Downloading \"${file.fileName}\"...")
            val result = SafInstaller.downloadAndInstall(
                context = getApplication(),
                treeUri = folderUri,
                downloadUrl = file.downloadUrl,
                fileName = file.fileName,
                extractIfArchive = extractIfArchive
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

    fun clearPending() {
        pendingProject = null
        pendingFiles = emptyList()
        pendingFile = null
    }
}
