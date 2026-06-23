package gex.com.masinqojava.networkimport

import gex.com.masinqojava.dataimport.DeezerSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ArtistApiService {
    @GET("search/artist")
    suspend fun searchArtist(
        @Query("q") artistName: String
    ): DeezerSearchResponse
}
