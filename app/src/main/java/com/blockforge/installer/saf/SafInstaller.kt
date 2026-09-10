package com.blockforge.installer.saf

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.blockforge.installer.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.util.zip.ZipInputStream

sealed class InstallResult {
    data class Success(val message: String) : InstallResult()
    data class Failure(val message: String) : InstallResult()
}

/**
 * Handles: downloading a mod/pack/world/shader file, then writing it into a folder the
 * user picked via the Storage Access Framework (e.g. their Amethyst launcher's game
 * data folder). SAF is required on modern Android to write into another app's storage
 * without root, since apps can no longer freely read/write each other's private files.
 */
object SafInstaller {

    /**
     * Downloads [downloadUrl] and installs it under the tree rooted at [treeUri].
     *
     * @param extractIfArchive if true and the file is a .zip/.mcpack/.mcworld/.mcaddon,
     *   it is unpacked into a folder named after the file (minus extension) instead of
     *   being copied as a single archive. Useful for behavior/resource packs that a
     *   launcher expects as loose folders rather than zips.
     */
    suspend fun downloadAndInstall(
        context: Context,
        treeUri: Uri,
        downloadUrl: String,
        fileName: String,
        extractIfArchive: Boolean
    ): InstallResult = withContext(Dispatchers.IO) {
        try {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext InstallResult.Failure("Selected folder is no longer accessible. Please choose it again.")
            if (!root.canWrite()) {
                return@withContext InstallResult.Failure("No write permission on the selected folder.")
            }

            // 1. Download to a temp cache file first (never stream straight into SAF while
            //    also parsing it, so a network hiccup can't leave a half-written pack).
            val tempFile = File(context.cacheDir, "dl_${System.currentTimeMillis()}_$fileName")
            val request = Request.Builder().url(downloadUrl).build()
            NetworkModule.downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext InstallResult.Failure("Download failed: HTTP ${response.code}")
                }
                val body = response.body ?: return@withContext InstallResult.Failure("Empty response body.")
                tempFile.outputStream().use { out -> body.byteStream().copyTo(out) }
            }

            val isArchive = fileName.endsWith(".zip", true) ||
                fileName.endsWith(".mcpack", true) ||
                fileName.endsWith(".mcworld", true) ||
                fileName.endsWith(".mcaddon", true)

            if (extractIfArchive && isArchive) {
                val folderName = fileName.substringBeforeLast('.')
                val destFolder = root.findFile(folderName)?.takeIf { it.isDirectory }
                    ?: root.createDirectory(folderName)
                    ?: return@withContext InstallResult.Failure("Could not create destination folder.")
                extractZipInto(context, tempFile, destFolder)
            } else {
                // Replace any existing file of the same name so re-installs/updates work cleanly.
                root.findFile(fileName)?.delete()
                val mime = "application/octet-stream"
                val newFile = root.createFile(mime, fileName)
                    ?: return@withContext InstallResult.Failure("Could not create file in destination folder.")
                context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                    tempFile.inputStream().use { it.copyTo(out) }
                } ?: return@withContext InstallResult.Failure("Could not open output stream for destination file.")
            }

            tempFile.delete()
            InstallResult.Success("Installed \"$fileName\" successfully.")
        } catch (t: Throwable) {
            InstallResult.Failure("Install failed: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    private fun extractZipInto(context: Context, zipFile: File, destFolder: DocumentFile) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val parts = entry.name.split("/").filter { it.isNotBlank() }
                    var currentFolder = destFolder
                    for (i in 0 until parts.size - 1) {
                        currentFolder = currentFolder.findFile(parts[i])?.takeIf { it.isDirectory }
                            ?: currentFolder.createDirectory(parts[i])
                            ?: currentFolder
                    }
                    val leafName = parts.last()
                    currentFolder.findFile(leafName)?.delete()
                    val newFile = currentFolder.createFile("application/octet-stream", leafName)
                    if (newFile != null) {
                        context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                            zis.copyTo(out)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
