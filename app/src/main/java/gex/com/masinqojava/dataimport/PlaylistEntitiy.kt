package gex.com.masinqojava.dataimport

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntitiy(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_song_cross_ref", primaryKeys = ["playlistId", "mediaId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val mediaId: Long
)
