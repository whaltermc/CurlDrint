package com.blockforge.installer.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    val modrinth: ModrinthApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.modrinth.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ModrinthApi::class.java)
    }

    val curseForge: CurseForgeApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.curseforge.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurseForgeApi::class.java)
    }

    /** Shared client for plain file downloads (not JSON). */
    val downloadClient: OkHttpClient get() = client
}
