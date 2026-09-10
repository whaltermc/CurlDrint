package com.blockforge.installer.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * CurseForge's "Eternal" API (https://docs.curseforge.com) requires a personal API key,
 * issued for free from the CurseForge Console. Per CurseForge's Studio ToS, keys are
 * per-developer and must NOT be hardcoded/redistributed in a shipped app, so this app
 * asks the user to paste their own key in Settings and sends it as the x-api-key header.
 *
 * gameId 432 = Minecraft.
 * classId values below are current at time of writing but CurseForge can renumber
 * categories; verify against GET /v1/games/432/categories if results look empty.
 */
interface CurseForgeApi {

    @GET("v1/mods/search")
    suspend fun search(
        @Header("x-api-key") apiKey: String,
        @Query("gameId") gameId: Int = 432,
        @Query("classId") classId: Int,
        @Query("searchFilter") searchFilter: String,
        @Query("pageSize") pageSize: Int = 20
    ): CurseForgeSearchResponse

    @GET("v1/mods/{modId}/files")
    suspend fun files(
        @Header("x-api-key") apiKey: String,
        @Path("modId") modId: Int
    ): CurseForgeFilesResponse
}

object CurseForgeClassIds {
    const val MODS = 6
    const val RESOURCE_PACKS = 12
    const val WORLDS = 17
    const val SHADERS = 6552
}

fun curseForgeClassIdFor(category: com.blockforge.installer.model.Category): Int = when (category) {
    com.blockforge.installer.model.Category.MODS -> CurseForgeClassIds.MODS
    com.blockforge.installer.model.Category.RESOURCE_PACKS -> CurseForgeClassIds.RESOURCE_PACKS
    com.blockforge.installer.model.Category.WORLDS -> CurseForgeClassIds.WORLDS
    com.blockforge.installer.model.Category.SHADERS -> CurseForgeClassIds.SHADERS
}

data class CurseForgeSearchResponse(@SerializedName("data") val data: List<CurseForgeMod>)

data class CurseForgeMod(
    @SerializedName("id") val id: Int,
    @SerializedName("slug") val slug: String,
    @SerializedName("name") val name: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("downloadCount") val downloadCount: Double,
    @SerializedName("logo") val logo: CurseForgeLogo?
)

data class CurseForgeLogo(@SerializedName("thumbnailUrl") val thumbnailUrl: String?)

data class CurseForgeFilesResponse(@SerializedName("data") val data: List<CurseForgeFile>)

data class CurseForgeFile(
    @SerializedName("id") val id: Int,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("fileName") val fileName: String,
    @SerializedName("downloadUrl") val downloadUrl: String?,
    @SerializedName("fileLength") val fileLength: Long
)
