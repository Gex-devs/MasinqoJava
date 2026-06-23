package gex.com.masinqojava.dataimport

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists WHERE artistName = :name LIMIT 1")
    suspend fun getArtistByName(name: String): ArtistImage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: ArtistImage)

    @Update
    suspend fun updateArtist(artist: ArtistImage)
}