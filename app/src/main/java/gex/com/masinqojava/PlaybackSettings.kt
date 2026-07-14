package gex.com.masinqojava

import android.content.Context
import androidx.core.content.edit

object PlaybackSettings {
    private const val PREFS_NAME = "playback_prefs"
    private const val KEY_LAST_MEDIA_ID = "last_media_id"
    private const val KEY_LAST_PLAYBACK_POSITION = "last_playback_position"

    fun saveLastTrack(context: android.content.Context, mediaId: String, position: Long) {
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit {
            putString(KEY_LAST_MEDIA_ID, mediaId)
            putLong(KEY_LAST_PLAYBACK_POSITION, position)
        }
    }

    fun getLastMediaId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getString(KEY_LAST_MEDIA_ID, null)
    }

    fun getLastPosition(context: android.content.Context): Long {
        return context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getLong(KEY_LAST_PLAYBACK_POSITION, 0L)
    }
}