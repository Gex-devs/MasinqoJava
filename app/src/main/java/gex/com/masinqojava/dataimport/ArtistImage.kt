package gex.com.masinqojava.dataimport

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class ArtistImage(
    @PrimaryKey val Id: Long,
    val artistName: String,
    val imageUrl: String? = null,
)
