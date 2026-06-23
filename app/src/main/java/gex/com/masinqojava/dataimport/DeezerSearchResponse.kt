package gex.com.masinqojava.dataimport

data class DeezerSearchResponse(
    val data: List<DeezerArtist>
)

data class DeezerArtist(
    val id: Long,
    val name: String,
    val picture_medium: String, // 250x250px
    val picture_big: String,    // 500x500px
    val picture_xl: String      // 1000x1000px
)
