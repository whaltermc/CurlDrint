package com.blockforge.installer.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Modrinth's REST API (https://docs.modrinth.com) is public and requires no API key.
 */
interface ModrinthApi {

    @GET("v2/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("facets") facets: String, // e.g. [["project_type:mod"]]
        @Query("limit") limit: Int = 20
    ): ModrinthSearchResponse

    @GET("v2/project/{id}/version")
    suspend fun versions(@Path("id") projectId: String): List<ModrinthVersion>
}

data class ModrinthSearchResponse(
    @SerializedName("hits") val hits: List<ModrinthHit>
)

data class ModrinthHit(
    @SerializedName("project_id") val projectId: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("icon_url") val iconUrl: String?,
    @SerializedName("downloads") val downloads: Long
)

data class ModrinthVersion(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("files") val files: List<ModrinthFile>
)

data class ModrinthFile(
    @SerializedName("url") val url: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("size") val size: Long,
    @SerializedName("primary") val primary: Boolean
)

/** Maps our generic Category to the Modrinth "project_type" facet. */
fun modrinthFacetFor(category: com.blockforge.installer.model.Category): String = when (category) {
    com.blockforge.installer.model.Category.MODS -> "[[\"project_type:mod\"]]"
    com.blockforge.installer.model.Category.RESOURCE_PACKS -> "[[\"project_type:resourcepack\"]]"
    com.blockforge.installer.model.Category.WORLDS -> "[[\"project_type:modpack\"]]" // Modrinth has no dedicated "world" type
    com.blockforge.installer.model.Category.SHADERS -> "[[\"project_type:shader\"]]"
}
