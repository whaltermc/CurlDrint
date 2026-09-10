package com.blockforge.installer.saf

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.blockforge.installer.model.PojavInstance

class PojavInstanceRepository(private val context: Context) {

    fun canAccessLauncherFolder(rootUri: Uri): Boolean {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return false
        if (!root.canRead() || !root.canWrite()) return false
        val minecraft = root.findFile(".minecraft") ?: return false
        if (!minecraft.isDirectory || !minecraft.canRead()) return false
        val profiles = minecraft.findFile("launcher_profiles.json") ?: return false
        return profiles.isFile && profiles.canRead()
    }

    fun readProfiles(rootUri: Uri): List<PojavInstance> {
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: error("The selected launcher folder is no longer accessible.")
        val minecraft = root.findFile(".minecraft")
            ?: error(".minecraft was not found in the selected launcher folder.")
        val profilesFile = minecraft.findFile("launcher_profiles.json")
            ?: error(".minecraft/launcher_profiles.json was not found.")

        val json = context.contentResolver.openInputStream(profilesFile.uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Could not read launcher_profiles.json.")

        return PojavProfilesParser.parse(json)
    }

    /**
     * Resolves the profile's gameDir relative to the SAF root.
     * Examples:
     *   missing gameDir -> .minecraft
     *   ./custom_instances/Foo -> custom_instances/Foo
     *   custom_instances/Foo -> custom_instances/Foo
     */
    fun resolveGameDir(rootUri: Uri, instance: PojavInstance): DocumentFile? {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return null
        val raw = instance.gameDir?.trim().orEmpty()
        val normalized = raw.replace('\\', '/')
        val relative = when {
            raw.isBlank() -> ".minecraft"
            normalized.startsWith("./") -> normalized.removePrefix("./")
            normalized.contains("/custom_instances/") -> normalized.substringAfter("/custom_instances/").let { "custom_instances/$it" }
            normalized.contains("/.minecraft/") -> normalized.substringAfter("/").let { ".minecraft/$it" }
            normalized.endsWith("/.minecraft") -> ".minecraft"
            else -> normalized.trimStart('/')
        }

        // launcher_profiles.json commonly stores gameDir as ./custom_instances/...
        // Keep traversal inside the user-selected SAF tree.
        val parts = relative.split('/').filter { it.isNotBlank() && it != "." && it != ".." }
        var current = root
        for (part in parts) {
            current = current.findFile(part)?.takeIf { it.isDirectory } ?: return null
        }
        return current
    }
}
