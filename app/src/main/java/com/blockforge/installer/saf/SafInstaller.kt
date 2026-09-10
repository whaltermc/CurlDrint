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

/** Downloads content and writes it into a SAF directory. */
object SafInstaller {

    suspend fun downloadAndInstall(
        context: Context,
        treeUri: Uri,
        downloadUrl: String,
        fileName: String,
        extractIfArchive: Boolean,
        targetSubfolder: String? = null
    ): InstallResult = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext InstallResult.Failure("Selected folder is no longer accessible. Please choose it again.")
        installIntoRoot(context, root, downloadUrl, fileName, extractIfArchive, targetSubfolder)
    }

    /** Same installer, but accepts an already-resolved DocumentFile (used for Pojav instances). */
    suspend fun downloadAndInstallInto(
        context: Context,
        root: DocumentFile,
        downloadUrl: String,
        fileName: String,
        extractIfArchive: Boolean,
        targetSubfolder: String? = null
    ): InstallResult = withContext(Dispatchers.IO) {
        installIntoRoot(context, root, downloadUrl, fileName, extractIfArchive, targetSubfolder)
    }

    private fun installIntoRoot(
        context: Context,
        root: DocumentFile,
        downloadUrl: String,
        fileName: String,
        extractIfArchive: Boolean,
        targetSubfolder: String?
    ): InstallResult {
        try {
            if (!root.canWrite()) {
                return InstallResult.Failure("No write permission on the selected folder.")
            }

            val destinationRoot = if (targetSubfolder.isNullOrBlank()) {
                root
            } else {
                root.findFile(targetSubfolder)?.takeIf { it.isDirectory }
                    ?: root.createDirectory(targetSubfolder)
                    ?: return InstallResult.Failure("Could not create $targetSubfolder folder.")
            }

            val safeName = File(fileName).name
            val tempFile = File(context.cacheDir, "dl_${System.currentTimeMillis()}_$safeName")
            try {
                val request = Request.Builder().url(downloadUrl).build()
                NetworkModule.downloadClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return InstallResult.Failure("Download failed: HTTP ${response.code}")
                    }
                    val body = response.body ?: return InstallResult.Failure("Empty response body.")
                    tempFile.outputStream().use { out -> body.byteStream().copyTo(out) }
                }

                val isArchive = safeName.endsWith(".zip", true) ||
                    safeName.endsWith(".mcpack", true) ||
                    safeName.endsWith(".mcworld", true) ||
                    safeName.endsWith(".mcaddon", true)

                if (extractIfArchive && isArchive) {
                    val folderName = safeName.substringBeforeLast('.')
                    val destFolder = destinationRoot.findFile(folderName)?.takeIf { it.isDirectory }
                        ?: destinationRoot.createDirectory(folderName)
                        ?: return InstallResult.Failure("Could not create destination folder.")
                    extractZipInto(context, tempFile, destFolder)
                } else {
                    destinationRoot.findFile(safeName)?.delete()
                    val newFile = destinationRoot.createFile("application/octet-stream", safeName)
                        ?: return InstallResult.Failure("Could not create file in destination folder.")
                    context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                        tempFile.inputStream().use { it.copyTo(out) }
                    } ?: return InstallResult.Failure("Could not open output stream for destination file.")
                }

                return InstallResult.Success("Installed \"$safeName\" successfully.")
            } finally {
                tempFile.delete()
            }
        } catch (t: Throwable) {
            return InstallResult.Failure("Install failed: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    private fun extractZipInto(context: Context, zipFile: File, destFolder: DocumentFile) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                // Prevent zip-slip paths such as ../../outside.txt.
                val parts = entry.name.replace('\\', '/').split('/').filter { it.isNotBlank() && it != "." && it != ".." }
                if (!entry.isDirectory && parts.isNotEmpty()) {
                    var currentFolder = destFolder
                    for (i in 0 until parts.size - 1) {
                        currentFolder = currentFolder.findFile(parts[i])?.takeIf { it.isDirectory }
                            ?: currentFolder.createDirectory(parts[i])
                            ?: break
                    }
                    val leafName = parts.last()
                    currentFolder.findFile(leafName)?.delete()
                    currentFolder.createFile("application/octet-stream", leafName)?.let { newFile ->
                        context.contentResolver.openOutputStream(newFile.uri)?.use { out -> zis.copyTo(out) }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
