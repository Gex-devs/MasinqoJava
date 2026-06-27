package gex.com.masinqojava

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import gex.com.masinqojava.dataimport.LyricLine
import gex.com.masinqojava.networkimport.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaybackViewModel : ViewModel() {
    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics = _lyrics.asStateFlow()
    private val _isLyricsLoading = MutableStateFlow(false)
    val isLoading = _isLyricsLoading.asStateFlow()
    private var player: Player? = null
    private val _upcomingItems = MutableStateFlow<List<Pair<Int, MediaItem>>>(emptyList())
    val upcomingItems = _upcomingItems.asStateFlow()
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()
    private var isUserSeeking = false
    private var lastFetchedSongKey: String? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            fetchLyrics(mediaMetadata, player?.duration ?: 0L)
            updateShadowQueue()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            updateShadowQueue()
        }

        override fun onRepeatModeChanged(repeatMode: Int) = updateShadowQueue()
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = updateShadowQueue()

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (oldPosition.mediaItemIndex != newPosition.mediaItemIndex) {
                updateShadowQueue()
            }
        }
    }

    private fun updateShadowQueue() {
        val p = player ?: return
        val timeline = p.currentTimeline
        if (timeline.isEmpty) {
            _upcomingItems.value = emptyList()
            return
        }

        val items = mutableListOf<Pair<Int, MediaItem>>()
        var nextIndex = timeline.getNextWindowIndex(
            p.currentMediaItemIndex,
            p.repeatMode,
            p.shuffleModeEnabled
        )
        val seenIndices = mutableSetOf<Int>()
        seenIndices.add(p.currentMediaItemIndex)
        val window = Timeline.Window()

        while (nextIndex != C.INDEX_UNSET && !seenIndices.contains(nextIndex)) {
            val mediaItem = timeline.getWindow(nextIndex, window).mediaItem
            items.add(nextIndex to mediaItem)

            seenIndices.add(nextIndex)
            nextIndex = timeline.getNextWindowIndex(nextIndex, p.repeatMode, p.shuffleModeEnabled)
            if (items.size >= 50) break
        }
        _upcomingItems.value = items
    }

    fun setPlayer(p: Player) {
        if (this.player === p) return
        this.player?.removeListener(playerListener)
        this.player = p
        p.addListener(playerListener)
        startProgressPolling()
        fetchLyrics(p.mediaMetadata, p.duration)
        updateShadowQueue()
    }
    fun toggleShuffle(){
        player?.let{
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }
    fun toggleRepeat(){
        player?.let {
            it.repeatMode = when(it.repeatMode){
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
                else -> Player.REPEAT_MODE_OFF
            }
        }

    }
    fun setUserSeeking(seeking: Boolean){
        isUserSeeking = seeking
        if (!seeking){
            player?.let { _currentPosition.value = it.currentPosition }
        }
    }
    fun seekTo(positionMs: Long){
        player?.seekTo(positionMs)
            _currentPosition.value = positionMs
    }
    private fun startProgressPolling() {
        viewModelScope.launch {
            while (true) {
                player?.let { p ->
                    // Only update if playing and the user isn't dragging the seekbar
                    if (p.isPlaying && !isUserSeeking) {
                        _currentPosition.value = p.currentPosition
                    }
                }
                // Wait 1 second before the next update
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun fetchLyrics(metadata: MediaMetadata?, currentDuration: Long) {
        val title = metadata?.title?.toString() ?: "Unknown"
        val artist = metadata?.artist?.toString() ?: "Unknown"
        val songKey = "$title-$artist"
        
        if (title == "Unknown" || artist == "Unknown") {
            _lyrics.value = emptyList()
            lastFetchedSongKey = null
            return
        }
        
        if (songKey == lastFetchedSongKey && (_lyrics.value.isNotEmpty() || _isLyricsLoading.value)) {
            return
        }
        
        lastFetchedSongKey = songKey
        _lyrics.value = emptyList()
        _isLyricsLoading.value = true
        
        val cleanArtist = artist.split(",", "/", "&", " - ")[0].trim()
        Log.d("Lyrics", "Fetching: $title by $cleanArtist")

        viewModelScope.launch {
            try {
                val durationSec = if (currentDuration > 0) (currentDuration / 1000).toInt() else null
                val response = RetrofitInstance.lyricApi.getLyrics(cleanArtist, title, null, durationSec)
                
                val parsedLyrics = when {
                    response.syncedLyrics != null -> {
                        val parsed = parseLrc(response.syncedLyrics)
                        Log.d("Lyrics", "Parsed ${parsed.size} synced lines for $title")
                        parsed
                    }
                    response.plainLyrics != null -> {
                        Log.d("Lyrics", "Found plain lyrics for $title")
                        listOf(LyricLine(0, response.plainLyrics))
                    }
                    else -> {
                        Log.d("Lyrics", "No lyrics found in response for $title")
                        emptyList()
                    }
                }
                _lyrics.value = parsedLyrics
            } catch (e: Exception) {
                Log.e("Lyrics", "Fetch error for $title: ${e.message}")
                _lyrics.value = emptyList()
            } finally {
                _isLyricsLoading.value = false
            }
        }
    }

    private fun parseLrc(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val timeTagRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{2,3}))?\\]")
        
        lrcContent.lines().forEach { line ->
            val trimmedLine = line.trim()
            val matches = timeTagRegex.findAll(trimmedLine).toList()
            if (matches.isEmpty()) return@forEach
            
            val text = trimmedLine.replace(timeTagRegex, "").trim()
            if (text.isNotEmpty()) {
                matches.forEach { match ->
                    try {
                        val min = match.groupValues[1].toLong()
                        val sec = match.groupValues[2].toLong()
                        val msPart = if (match.groupValues.size > 3) match.groupValues[3] else ""
                        val ms = when (msPart.length) {
                            2 -> msPart.toLong() * 10
                            3 -> msPart.toLong()
                            else -> 0L
                        }
                        val totalTime = (min * 60 * 1000) + (sec * 1000) + ms
                        lines.add(LyricLine(totalTime, text))
                    } catch (e: Exception) {
                        Log.e("Lyrics", "Parsing error in line: $trimmedLine")
                    }
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    override fun onCleared() {
        super.onCleared()
        player?.removeListener(playerListener)
        player = null
    }
}
