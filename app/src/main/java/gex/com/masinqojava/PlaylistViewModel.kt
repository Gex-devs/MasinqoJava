package gex.com.masinqojava

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import gex.com.masinqojava.dataimport.AppDatabase
import gex.com.masinqojava.dataimport.PlaylistEntitiy
import gex.com.masinqojava.dataimport.PlaylistSongCrossRef
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val playlistDao = AppDatabase.getDatabase(application).playlistDao()

    private val _playlistItems = MutableLiveData<List<PlaylistEntitiy>>()
    val playlistItems: LiveData<List<PlaylistEntitiy>> get() = _playlistItems

    private val _currentPlaylistSongs = MutableLiveData<List<MediaItem>>()
    val currentPlaylistSongs: LiveData<List<MediaItem>> get() = _currentPlaylistSongs

    private var songsJob: Job? = null

    init {
        observePlaylists()
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            playlistDao.getAllPlaylists().collectLatest { playlists ->
                _playlistItems.postValue(playlists)
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistDao.createPlaylist(PlaylistEntitiy(name = name))
        }
    }

    fun deletePlaylist(playlist: PlaylistEntitiy) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlist)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            playlistDao.renamePlaylist(playlistId, newName)
        }
    }

    fun addSongToPlaylist(song: MediaItem, playlistId: Long) {
        viewModelScope.launch {
            val mediaId = song.mediaId.toLongOrNull()
            if (mediaId != null) {
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, mediaId))
            }
        }
    }

    fun removeFromPlaylist(song: MediaItem, playlistId: Long) {
        viewModelScope.launch {
            val mediaId = song.mediaId.toLongOrNull()
            if (mediaId != null) {
                playlistDao.removeSongFromPlaylist(playlistId, mediaId)
            }
        }
    }

    fun loadSongsForPlaylist(playlistId: Long) {
        songsJob?.cancel()
        songsJob = viewModelScope.launch {
            playlistDao.getSongsForPlaylist(playlistId).collectLatest { ids ->
                val songs = MusicLoader.getSongsByIds(getApplication(), ids)
                _currentPlaylistSongs.postValue(songs.filterNotNull())
            }
        }
    }


}
