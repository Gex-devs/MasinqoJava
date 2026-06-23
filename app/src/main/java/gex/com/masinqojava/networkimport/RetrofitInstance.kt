package gex.com.masinqojava.networkimport


object RetrofitInstance {
    private const val BASE_URL = "https://api.deezer.com/"

    val api: ArtistApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(ArtistApiService::class.java)
    }
}
