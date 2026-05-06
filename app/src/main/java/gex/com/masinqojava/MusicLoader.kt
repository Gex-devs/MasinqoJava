package gex.com.masinqojava.gex.com.masinqojava

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem

class MusicLoader {

    fun getSongs(context: Context): ArrayList<MediaItem?> {
        val tempAudioList = ArrayList<MediaItem?>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val title = cursor.getString(0)
                val artist = cursor.getString(1)
                val duration = cursor.getLong(2)
                val path = cursor.getString(3)
                val albumId = cursor.getLong(5)
                val artUri = ContentUris.withAppendedId(
                    "content://media/external/audio/albumart".toUri(),
                    albumId
                )

                val mediaItem = MediaItem.Builder()
                    .setMediaId(path)
                    .setUri(path)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(artist)
                            .setDurationMs(duration)
                            .setArtworkUri(artUri)
                            .build()
                    )
                    .build()
                tempAudioList.add(mediaItem)
                Log.d("De Song list", "path: $path artist: $artist")
            }
        }
        return tempAudioList
    }

    fun getAlbums(context: Context): ArrayList<MediaItem?> {
        val tempAlbumList = ArrayList<MediaItem?>()
        val uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val title = cursor.getString(0) ?: "Unknown album"
                val artist = cursor.getString(1) ?: "Unknown artist"
                val albumId = cursor.getLong(2)
                val numberOfSongs = cursor.getInt(3)
                val artUri = ContentUris.withAppendedId(
                    "content://media/external/audio/albumart".toUri(),
                    albumId
                )
                val mediaItem = MediaItem.Builder()
                    .setMediaId(albumId.toString())
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(artist)
                            .setArtworkUri(artUri)
                            .setExtras(android.os.Bundle().apply {
                                putInt("track_count", numberOfSongs)
                            })
                            .build()
                    )
                    .build()
                tempAlbumList.add(mediaItem)
                Log.d("De Album list", "Album title: $title Artist: $artist")


            }
        }
        return tempAlbumList
    }
}