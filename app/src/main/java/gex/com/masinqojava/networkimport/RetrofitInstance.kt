package gex.com.masinqojava.networkimport

import gex.com.masinqojava.networkimport.LyricApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val DEEZER_BASE_URL = "https://api.deezer.com/"
    private const val LRCLIB_BASE_URL = "https://lrclib.net/api/"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply{
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    val artistApi: ArtistApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(DEEZER_BASE_URL)
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(ArtistApiService::class.java)
    }

    val lyricApi: LyricApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(LRCLIB_BASE_URL)
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(LyricApiService::class.java)
    }
}
