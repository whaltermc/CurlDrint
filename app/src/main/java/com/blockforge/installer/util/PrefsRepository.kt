package com.blockforge.installer.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "blockforge_prefs")

class PrefsRepository(private val context: Context) {
    private val launcherTreeKey = stringPreferencesKey("launcher_tree_uri")
    private val selectedInstanceKey = stringPreferencesKey("selected_instance_id")
    private val curseForgeKeyPref = stringPreferencesKey("curseforge_api_key")

    val launcherTreeUriFlow: Flow<String?> =
        context.dataStore.data.map { it[launcherTreeKey] }

    val selectedInstanceIdFlow: Flow<String?> =
        context.dataStore.data.map { it[selectedInstanceKey] }

    val curseForgeApiKeyFlow: Flow<String?> =
        context.dataStore.data.map { it[curseForgeKeyPref] }

    suspend fun setLauncherTreeUri(uri: String) {
        context.dataStore.edit { it[launcherTreeKey] = uri }
    }

    suspend fun setSelectedInstanceId(id: String) {
        context.dataStore.edit { it[selectedInstanceKey] = id }
    }

    suspend fun setCurseForgeApiKey(key: String) {
        context.dataStore.edit { it[curseForgeKeyPref] = key }
    }
}
