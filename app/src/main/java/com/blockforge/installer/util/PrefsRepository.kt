package com.blockforge.installer.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.blockforge.installer.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "blockforge_prefs")

class PrefsRepository(private val context: Context) {

    private fun folderKey(category: Category) = stringPreferencesKey("folder_uri_${category.name}")
    private val curseForgeKeyPref = stringPreferencesKey("curseforge_api_key")
    private val pojavRootUriPref = stringPreferencesKey("pojav_root_uri")
    private val selectedPojavInstancePref = stringPreferencesKey("selected_pojav_instance")

    fun folderUriFlow(category: Category): Flow<String?> =
        context.dataStore.data.map { it[folderKey(category)] }

    suspend fun setFolderUri(category: Category, uriString: String) {
        context.dataStore.edit { it[folderKey(category)] = uriString }
    }

    val curseForgeApiKeyFlow: Flow<String?> =
        context.dataStore.data.map { it[curseForgeKeyPref] }

    suspend fun setCurseForgeApiKey(key: String) {
        context.dataStore.edit { it[curseForgeKeyPref] = key }
    }

    val pojavRootUriFlow: Flow<String?> =
        context.dataStore.data.map { it[pojavRootUriPref] }

    suspend fun setPojavRootUri(uri: String) {
        context.dataStore.edit { it[pojavRootUriPref] = uri }
    }

    val selectedPojavInstanceFlow: Flow<String?> =
        context.dataStore.data.map { it[selectedPojavInstancePref] }

    suspend fun setSelectedPojavInstance(id: String) {
        context.dataStore.edit { it[selectedPojavInstancePref] = id }
    }
}
