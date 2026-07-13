package gex.com.masinqojava.dataimport

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ArtistImage::class, PlaylistEntitiy::class, PlaylistSongCrossRef::class], version = 2)
abstract class AppDatabase : RoomDatabase(){
    abstract fun artistDao(): ArtistDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                context.applicationContext,
                    AppDatabase::class.java,
                    "masinqo_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
