package com.blockforge.installer.saf

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.blockforge.installer.model.PojavInstance

/** Reads Pojav's profiles.json and resolves an instance's gameDir inside the SAF tree. */
class PojavInstanceRepository(private val context: Context) {

    fun readProfiles(pojavRootUri: Uri): List<PojavInstance> {
        val root = DocumentFile.fromTreeUri(context, pojavRootUri)
            ?: return emptyList()
        val profilesFile = root.findFile("profiles.json")
            ?: return emptyList()
        val input = context.contentResolver.openInputStream(profilesFile.uri)
            ?: return emptyList()
        val text = input.bufferedReader().use { it.readText() }
        return PojavProfilesParser.parse(text)
    }

    /**
     * Resolves profile.gameDir when it is inside the tree the user granted us.
     * A profile without gameDir uses the selected Pojav root itself.
     */
    fun resolveGameDirectory(rootUri: Uri, instance: PojavInstance): DocumentFile? {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return null
        val gameDir = instance.gameDir?.trim().orEmpty()
        if (gameDir.isEmpty()) return root

        val treeId = DocumentsContract.getTreeDocumentId(rootUri)
        val targetId = absolutePathToDocumentId(gameDir) ?: return null

        if (targetId == treeId) return root
        val prefix = if (treeId.endsWith('/')) treeId else "$treeId/"
        if (!targetId.startsWith(prefix)) return null

        val relative = targetId.removePrefix(prefix)
            .split('/')
            .filter { it.isNotBlank() }

        var current = root
        for (part in relative) {
            current = current.findFile(part)?.takeIf { it.isDirectory } ?: return null
        }
        return current
    }

    private fun absolutePathToDocumentId(path: String): String? {
        val normalized = path.replace('\\', '/').trimEnd('/')
        val primary = "/storage/emulated/0"
        return when {
            normalized == primary -> "primary:"
            normalized.startsWith("$primary/") ->
                "primary:" + normalized.removePrefix("$primary/")
            else -> null
        }
    }
}
