package com.blockforge.installer.network

import com.blockforge.installer.model.Category
import com.blockforge.installer.model.FileResult
import com.blockforge.installer.model.ProjectResult
import com.blockforge.installer.model.Source

class ContentRepository {

    suspend fun search(source: Source, category: Category, query: String, curseForgeApiKey: String?): List<ProjectResult> {
        return when (source) {
            Source.MODRINTH -> {
                val q = query.ifBlank { " " } // Modrinth wants a non-empty query for browse-style search
                val response = NetworkModule.modrinth.search(q, modrinthFacetFor(category))
                response.hits.map {
                    ProjectResult(
                        id = it.projectId,
                        slug = it.slug,
                        title = it.title,
                        description = it.description,
                        iconUrl = it.iconUrl,
                        downloads = it.downloads,
                        source = Source.MODRINTH
                    )
                }
            }
            Source.CURSEFORGE -> {
                val key = curseForgeApiKey
                if (key.isNullOrBlank()) {
                    throw IllegalStateException("Add your CurseForge API key in Settings first.")
                }
                val response = NetworkModule.curseForge.search(
                    apiKey = key,
                    classId = curseForgeClassIdFor(category),
                    searchFilter = query
                )
                response.data.map {
                    ProjectResult(
                        id = it.id.toString(),
                        slug = it.slug,
                        title = it.name,
                        description = it.summary,
                        iconUrl = it.logo?.thumbnailUrl,
                        downloads = it.downloadCount.toLong(),
                        source = Source.CURSEFORGE
                    )
                }
            }
        }
    }

    suspend fun files(project: ProjectResult, curseForgeApiKey: String?): List<FileResult> {
        return when (project.source) {
            Source.MODRINTH -> {
                val versions = NetworkModule.modrinth.versions(project.id)
                versions.flatMap { version ->
                    version.files.map { file ->
                        FileResult(
                            fileId = version.id,
                            displayName = version.name,
                            fileName = file.filename,
                            downloadUrl = file.url,
                            sizeBytes = file.size
                        )
                    }
                }
            }
            Source.CURSEFORGE -> {
                val key = curseForgeApiKey
                    ?: throw IllegalStateException("Add your CurseForge API key in Settings first.")
                val response = NetworkModule.curseForge.files(key, project.id.toInt())
                response.data.mapNotNull { file ->
                    val url = file.downloadUrl ?: return@mapNotNull null
                    FileResult(
                        fileId = file.id.toString(),
                        displayName = file.displayName,
                        fileName = file.fileName,
                        downloadUrl = url,
                        sizeBytes = file.fileLength
                    )
                }
            }
        }
    }
}
