package gex.com.masinqojava.networkimport

import retrofit2.http.GET
import retrofit2.http.Query

interface LyricApiService {
    @GET("get")
    suspend fun getLyrics(
        @Query("artist_name") artist: String,
        @Query("track_name") title: String,
        @Query("album_name") album: String?,
        @Query("duration") duration: Int?,
    ) : LyricResponse
}

data class LyricResponse(
    val syncedLyrics: String?,
    val plainLyrics: String?,
    val instrumental: Boolean
)
