package gex.com.masinqojava.dataimport

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: PlaylistEntitiy)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntitiy)

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntitiy>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun removeSongFromPlaylist(playlistId: Long, mediaId: Long)

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Query("SELECT mediaId FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    fun getSongsForPlaylist(playlistId: Long): Flow<List<Long>>
}
