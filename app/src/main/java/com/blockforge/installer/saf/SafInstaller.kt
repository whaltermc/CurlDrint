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

object SafInstaller {
    suspend fun downloadAndInstall(
        context: Context,
        treeUri: Uri,
        downloadUrl: String,
        fileName: String,
        extractIfArchive: Boolean,
        targetSubfolder: String? = null
    ): InstallResult = withContext(Dispatchers.IO) {
        try {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext InstallResult.Failure("Selected folder is no longer accessible.")
            if (!root.canWrite()) {
                return@withContext InstallResult.Failure("No write permission on the selected folder.")
            }
            installDownloaded(
                context = context,
                root = root,
                downloadUrl = downloadUrl,
                fileName = fileName,
                extractIfArchive = extractIfArchive,
                targetSubfolder = targetSubfolder
            )
        } catch (t: Throwable) {
            InstallResult.Failure("Install failed: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    suspend fun downloadAndInstallToDirectory(
        context: Context,
        directory: DocumentFile,
        downloadUrl: String,
        fileName: String,
        extractIfArchive: Boolean,
        targetSubfolder: String
    ): InstallResult = withContext(Dispatchers.IO) {
        try {
            if (!directory.isDirectory || !directory.canWrite()) {
                return@withContext InstallResult.Failure("The selected instance directory is not writable.")
            }
            installDownloaded(
                context = context,
                root = directory,
                downloadUrl = downloadUrl,
                fileName = fileName,
                extractIfArchive = extractIfArchive,
                targetSubfolder = targetSubfolder
            )
        } catch (t: Throwable) {
            InstallResult.Failure("Install failed: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    private fun installDownloaded(
        context: Context,
        root: DocumentFile,
        downloadUrl: String,
        fileName: String,
        extractIfArchive: Boolean,
        targetSubfolder: String?
    ): InstallResult {
        val tempFile = File(context.cacheDir, "dl_${System.currentTimeMillis()}_${fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")}")
        try {
            val request = Request.Builder().url(downloadUrl).build()
            NetworkModule.downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return InstallResult.Failure("Download failed: HTTP ${response.code}")
                val body = response.body ?: return InstallResult.Failure("Empty response body.")
                tempFile.outputStream().use { out -> body.byteStream().copyTo(out) }
            }

            val destination = if (targetSubfolder != null) {
                root.findFile(targetSubfolder)?.takeIf { it.isDirectory }
                    ?: root.createDirectory(targetSubfolder)
                    ?: return InstallResult.Failure("Could not create $targetSubfolder folder.")
            } else root

            val isArchive = fileName.endsWith(".zip", true) ||
                fileName.endsWith(".mcpack", true) ||
                fileName.endsWith(".mcworld", true) ||
                fileName.endsWith(".mcaddon", true)

            if (extractIfArchive && isArchive) {
                val folderName = fileName.substringBeforeLast('.')
                val destFolder = destination.findFile(folderName)?.takeIf { it.isDirectory }
                    ?: destination.createDirectory(folderName)
                    ?: return InstallResult.Failure("Could not create destination folder.")
                extractZipInto(context, tempFile, destFolder)
            } else {
                destination.findFile(fileName)?.delete()
                val newFile = destination.createFile("application/octet-stream", fileName)
                    ?: return InstallResult.Failure("Could not create file in destination folder.")
                context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                    tempFile.inputStream().use { it.copyTo(out) }
                } ?: return InstallResult.Failure("Could not open destination file.")
            }
            return InstallResult.Success("Installed \"$fileName\" successfully.")
        } finally {
            tempFile.delete()
        }
    }

    private fun extractZipInto(context: Context, zipFile: File, destFolder: DocumentFile) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val parts = entry.name.split('/').filter { it.isNotBlank() && it != "." && it != ".." }
                    if (parts.isNotEmpty()) {
                        var currentFolder = destFolder
                        for (i in 0 until parts.size - 1) {
                            currentFolder = currentFolder.findFile(parts[i])?.takeIf { it.isDirectory }
                                ?: currentFolder.createDirectory(parts[i])
                                ?: break
                        }
                        val leafName = parts.last()
                        currentFolder.findFile(leafName)?.delete()
                        val newFile = currentFolder.createFile("application/octet-stream", leafName)
                        if (newFile != null) {
                            context.contentResolver.openOutputStream(newFile.uri)?.use { out -> zis.copyTo(out) }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
