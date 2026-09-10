package com.blockforge.installer.model

/** Which content source the user is browsing. */
enum class Source { MODRINTH, CURSEFORGE }

/** Which content category the user is browsing/installing. */
enum class Category(val label: String, val targetSubfolder: String) {
    MODS("Mods", "mods"),
    RESOURCE_PACKS("Resource Packs", "resourcepacks"),
    WORLDS("Worlds", "saves"),
    SHADERS("Shaders", "shaderpacks");
}

/** A search result, normalized from either Modrinth or CurseForge so the UI is source-agnostic. */
data class ProjectResult(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val iconUrl: String?,
    val downloads: Long,
    val source: Source
)

/** A specific downloadable file/version for a project. */
data class FileResult(
    val fileId: String,
    val displayName: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long
)


/** A Minecraft Java instance discovered from PojavLauncher's profiles.json. */
data class PojavInstance(
    val id: String,
    val name: String,
    val version: String?,
    val gameDir: String?
)
