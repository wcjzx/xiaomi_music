package com.example.music_yaoyuhang.Request

import com.example.music_yaoyuhang.Dao.HomePageResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("/music/homePage")
    suspend fun getHomePageData(
        @Query("current") current: Int = 2,
        @Query("size") size: Int = 5
    ): HomePageResponse

    @GET("/music/lyrics")
    suspend fun getLyrics(@Query("lyricUrl") lyricUrl: String): List<String>
}
