package gex.com.masinqojava.dataimport

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ArtistImage::class], version = 1)
abstract class AppDatabase : RoomDatabase(){
    abstract fun artistDao(): ArtistDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                context.applicationContext,
                    AppDatabase::class.java,
                    "masinqo_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
