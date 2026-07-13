package gex.com.masinqojava

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

object MusicLoader {

    private val SONG_PROJECTION = arrayOf(
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media._ID
    )

    fun getSongs(context: Context): ArrayList<MediaItem?> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        return resolver(context, uri, SONG_PROJECTION, null, null)
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
                        MediaMetadata.Builder()
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
            }
        }
        return tempAlbumList
    }

    fun getSongsByAlbum(context: Context, albumId: Long): ArrayList<MediaItem?> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())
        return resolver(context, uri, SONG_PROJECTION, selection, selectionArgs)
    }

    fun getArtists(context: Context): ArrayList<MediaItem?> {
        val tempArtistList = ArrayList<MediaItem?>()
        val uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
        val trackUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
        )
        val sortOrder = "${MediaStore.Audio.Artists.ARTIST} COLLATE NOCASE ASC"

        context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            while (cursor.moveToNext()) {
                val artistName = cursor.getString(0) ?: "Unknown artist"
                val artistId = cursor.getLong(1)
                val numberOfAlbums = cursor.getInt(2)
                val numberOfSongs = cursor.getInt(3)

                val trackProjection = arrayOf(MediaStore.Audio.Media.ALBUM_ID)
                val trackSelection = "${MediaStore.Audio.Media.ARTIST_ID} = ?"
                val trackSelectionArgs = arrayOf(artistId.toString())

                var representativeAlbumId: Long = -1
                context.contentResolver.query(
                    trackUri,
                    trackProjection,
                    trackSelection,
                    trackSelectionArgs,
                    "${MediaStore.Audio.Media.DATE_ADDED} DESC"
                )?.use { trackCursor ->
                    if (trackCursor.moveToFirst()) {
                        representativeAlbumId = trackCursor.getLong(0)
                    }
                }
                val artUri = if (representativeAlbumId != -1L) {
                    ContentUris.withAppendedId(
                        "content://media/external/audio/albumart".toUri(),
                        representativeAlbumId
                    )
                } else {
                    null
                }

                val mediaItem = MediaItem.Builder()
                    .setMediaId(artistId.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setArtist(artistName)
                            .setArtworkUri(artUri)
                            .setExtras(android.os.Bundle().apply {
                                putInt("album_count", numberOfAlbums)
                                putInt("track_count", numberOfSongs)
                            })
                            .build()
                    )
                    .build()
                tempArtistList.add(mediaItem)
            }
        }
        return tempArtistList
    }

    fun getGenres(context: Context): ArrayList<MediaItem?> {
        val tempGenreList = ArrayList<MediaItem?>()
        val uri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Genres.NAME,
            MediaStore.Audio.Genres._ID,
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val genreIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
            val genreNameIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
            while (cursor.moveToNext()) {
                val genreId = cursor.getLong(genreIdIdx)
                val genreName = cursor.getString(genreNameIdx)

                val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                val membersProjection = arrayOf(MediaStore.Audio.Media.ALBUM_ID)
                var representativeAlbumId: Long = -1
                var trackCount = 0

                context.contentResolver.query(membersUri, membersProjection, null, null, null)?.use { membersCursor ->
                    trackCount = membersCursor.count
                    if (membersCursor.moveToFirst()) {
                        representativeAlbumId = membersCursor.getLong(membersCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                    }
                }

                val artUri = if (representativeAlbumId != -1L) {
                    ContentUris.withAppendedId(
                        "content://media/external/audio/albumart".toUri(),
                        representativeAlbumId
                    )
                } else {
                    null
                }

                val mediaItem = MediaItem.Builder()
                    .setMediaId(genreId.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(genreName)
                            .setArtworkUri(artUri)
                            .setExtras(android.os.Bundle().apply {
                                putInt("track_count", trackCount)
                            })
                            .build()
                    )
                    .build()
                tempGenreList.add(mediaItem)
            }
        }
        return tempGenreList
    }

    fun getSongsByGenre(context: Context, genreId: Long): ArrayList<MediaItem?> {
        val uri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
        return resolver(context, uri, SONG_PROJECTION, null, null)
    }

    fun getSongsByArtist(context: Context, artistId: Long): ArrayList<MediaItem?> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Media.ARTIST_ID} = ?"
        val selectionArgs = arrayOf(artistId.toString())
        return resolver(context, uri, SONG_PROJECTION, selection, selectionArgs)
    }

    fun getSongsByIds(context: Context, ids: List<Long>): ArrayList<MediaItem?> {
        if (ids.isEmpty()) return ArrayList()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Media._ID} IN (${ids.joinToString(",")})"
        // Selection args not used with IN clause for simplicity here, but be careful with large lists
        return resolver(context, uri, SONG_PROJECTION, selection, null)
    }

    fun resolver(
        context: Context,
        uri: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?
    ): ArrayList<MediaItem?> {
        val content = ArrayList<MediaItem?>()
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                while (cursor.moveToNext()) {
                    val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val pathIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val albumIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

                    val title = cursor.getString(titleIdx)
                    val artist = cursor.getString(artistIdx)
                    val duration = cursor.getLong(durationIdx)
                    val path = cursor.getString(pathIdx)
                    val albumId = cursor.getLong(albumIdIdx)
                    val id = cursor.getLong(idIdx)
                    
                    val artUri = ContentUris.withAppendedId(
                        "content://media/external/audio/albumart".toUri(),
                        albumId
                    )
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(id.toString())
                        .setUri(path)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(title)
                                .setArtist(artist)
                                .setDurationMs(duration)
                                .setArtworkUri(artUri)
                                .build()
                        )
                        .build()
                    content.add(mediaItem)
                }
            }
        return content
    }
}
