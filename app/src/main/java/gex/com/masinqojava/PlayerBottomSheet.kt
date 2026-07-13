package gex.com.masinqojava

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import gex.com.masinqojava.databinding.ExpandedPlaybackControlsBinding
import gex.com.masinqojava.dataimport.LyricLine
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.launch
import java.util.Locale
import com.google.android.material.R as MaterialR

class PlayerBottomSheet(private val player: Player) : BottomSheetDialogFragment() {
    private var _binding: ExpandedPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var queueAdapter: SongAdapter
    private lateinit var lyricAdapter: LyricAdapter
    private val playbackViewModel: PlaybackViewModel by activityViewModels()
    private val playlistViewModel: PlaylistViewModel by activityViewModels()

    private val lyricsUpdateAction = object : Runnable {
        override fun run() {
            val binding = _binding ?: return
            if (player.isPlaying && binding.lyricsRecyclerView.isVisible) {
                val activeIndex = lyricAdapter.updateActiveLine(player.currentPosition)
                if (activeIndex != -1) {
                    centerLyricLine(activeIndex)
                }
            }
            handler.postDelayed(this, 100)
        }
    }

    private val updateProgressAction = object : Runnable {
        override fun run() {
            val binding = _binding ?: return
            if (player.isPlaying) {
                val currentPos = player.currentPosition.toInt()
                binding.seekbar.progress = currentPos
                binding.currentTime.text = formatDuration(currentPos)
            }
            handler.postDelayed(this, 1000)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val binding = _binding ?: return
            if (playbackState == Player.STATE_READY) {
                val duration = player.duration.toInt()
                binding.seekbar.max = duration
                binding.totalTime.text = formatDuration(duration)
                handler.post(updateProgressAction)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val binding = _binding ?: return
            updatePlayPauseIcon(isPlaying)
            if (isPlaying) {
                handler.post(updateProgressAction)
                if (binding.lyricsRecyclerView.isVisible) startLyricsSync()
            } else {
                handler.removeCallbacks(updateProgressAction)
                handler.removeCallbacks(lyricsUpdateAction)
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            if (_binding == null) return
            updateMetadataUI(mediaMetadata)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            // Use the alias you created (MaterialR) to find the ID
            val bottomSheet = dialog.findViewById<FrameLayout>(MaterialR.id.design_bottom_sheet)

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ExpandedPlaybackControlsBinding.inflate(inflater, container, false)
        player.addListener(playerListener)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playbackViewModel.setPlayer(player)
        lyricAdapter = LyricAdapter()
        binding.lyricsRecyclerView.adapter = lyricAdapter
        binding.lyricsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.lyricsShimmerView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                LyricShimmer()
            }
        }

        binding.expandCollapse.setOnClickListener {
            val lyricSheet = LyricsFragment(player)
            lyricSheet.show(parentFragmentManager, "LyricsFragment")
        }

        queueAdapter = SongAdapter(
            ArrayList(), requireContext(), playbackViewModel,
            onItemClick = { pos ->
                val upcoming = playbackViewModel.upcomingItems.value
                if (pos in upcoming.indices) {
                    val targetIndex = upcoming[pos].first
                    player.seekTo(targetIndex, 0L)
                    player.play()
                }
            }, onMoreOptionClick = { song ->
                val optionSheet = SongOptionBottomSheet(song, playbackViewModel, playlistViewModel)
                optionSheet.show(childFragmentManager, "SongOptions")

            })
        binding.currentQueue.layoutManager = LinearLayoutManager(context)
        binding.currentQueue.adapter = queueAdapter

        binding.btnShuffle.setOnClickListener {
            playbackViewModel.toggleShuffle()
            updateShuffleRepeatUI()
        }
        binding.btnRepeat.setOnClickListener {
            playbackViewModel.toggleRepeat()
            updateShuffleRepeatUI()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            playbackViewModel.lyrics.collect { lyrics ->
                lyricAdapter.setData(lyrics)
                updateLyricsUIState(playbackViewModel.isLoading.value)
                if (lyrics.isNotEmpty()) {
                    lyricAdapter.updateActiveLine(player.currentPosition)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            playbackViewModel.isLoading.collect { loading ->
                updateLyricsUIState(loading)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            playbackViewModel.upcomingItems.collect { pairs ->
                val displayList = pairs.map { it.second }
                queueAdapter.updateList(displayList)
            }
        }

        syncUI()

        binding.btnPrevious.setOnClickListener { player.seekToPreviousMediaItem() }
        binding.btnNext.setOnClickListener { player.seekToNextMediaItem() }
        binding.btnPlayPause.setOnClickListener { if (player.isPlaying) player.pause() else player.play() }

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Tell ViewModel to seek and update the internal StateFlow
                    playbackViewModel.seekTo(p.toLong())

                    // Update the text immediately for a smooth experience
                    binding.currentTime.text = formatDuration(p)

                    // If lyrics are visible, sync them while dragging
                    if (binding.lyricsRecyclerView.isVisible) {
                        lyricAdapter.updateActiveLine(p.toLong())
                    }
                }
            }

            override fun onStartTrackingTouch(s: SeekBar?) {
                // Stop the ViewModel from updating the progress flow while the user is dragging
                playbackViewModel.setUserSeeking(true)
            }

            override fun onStopTrackingTouch(s: SeekBar?) {
                // Resume normal progress updates
                playbackViewModel.setUserSeeking(false)
            }
        })
    }

    private fun startLyricsSync() {
        handler.removeCallbacks(lyricsUpdateAction)
        handler.post(lyricsUpdateAction)
    }

    private fun toggleTab(showQueue: Boolean, showLyrics: Boolean) {
        val binding = _binding ?: return
        binding.nestedQueueContainer.visibility =
            if (showQueue || showLyrics) View.VISIBLE else View.GONE
        binding.currentQueue.visibility = if (showQueue) View.VISIBLE else View.GONE
        binding.queueHeader.visibility = if (showQueue) View.GONE else View.VISIBLE

        if (showLyrics) {
            updateLyricsUIState(playbackViewModel.isLoading.value)
        } else {
            binding.lyricsRecyclerView.visibility = View.GONE
            binding.lyricsShimmerView.visibility = View.GONE
            binding.lyricsEmptyText.visibility = View.GONE
            handler.removeCallbacks(lyricsUpdateAction)
        }

        binding.artWork.visibility = if (showQueue || showLyrics) View.GONE else View.VISIBLE
        binding.appBarLayout.updateLayoutParams {
            height =
                if (showQueue || showLyrics) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    private fun updateLyricsUIState(isLoading: Boolean) {
        val binding = _binding ?: return
        val hasLyrics = lyricAdapter.itemCount > 0

        if (isLoading) {
            binding.lyricsShimmerView.visibility = View.VISIBLE
            binding.lyricsRecyclerView.visibility = View.GONE
            binding.lyricsEmptyText.visibility = View.GONE
            handler.removeCallbacks(lyricsUpdateAction)
        } else if (hasLyrics) {
            binding.lyricsShimmerView.visibility = View.GONE
            binding.lyricsRecyclerView.visibility = View.VISIBLE
            binding.lyricsEmptyText.visibility = View.GONE
            if (player.isPlaying) startLyricsSync()
        } else {
            binding.lyricsShimmerView.visibility = View.GONE
            binding.lyricsRecyclerView.visibility = View.GONE
            binding.lyricsEmptyText.visibility = View.VISIBLE
            handler.removeCallbacks(lyricsUpdateAction)
        }
    }

    private fun centerLyricLine(position: Int) {
        val binding = _binding ?: return
        val scroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START
            override fun calculateDtToFit(vS: Int, vE: Int, bS: Int, bE: Int, sP: Int): Int =
                (bS + (bE - bS) / 2) - (vS + (vE - vS) / 2)
        }
        scroller.targetPosition = position
        binding.lyricsRecyclerView.layoutManager?.startSmoothScroll(scroller)
    }

    private fun syncUI() {
        val binding = _binding ?: return
        updateMetadataUI(player.mediaMetadata)
        updatePlayPauseIcon(player.isPlaying)
        val duration = player.duration.toInt()
        if (duration > 0) {
            binding.seekbar.max = duration
            binding.totalTime.text = formatDuration(duration)
        }
        val currentPos = player.currentPosition.toInt()
        binding.seekbar.progress = currentPos
        binding.currentTime.text = formatDuration(currentPos)

        lyricAdapter.updateActiveLine(player.currentPosition)

        if (player.isPlaying) {
            handler.post(updateProgressAction)
            if (binding.lyricsRecyclerView.isVisible) startLyricsSync()
        }
    }

    private fun updateMetadataUI(metadata: MediaMetadata) {
        val binding = _binding ?: return
        binding.songTitle.text = metadata.title ?: "Unknown Title"
        binding.artistName.text = metadata.artist ?: "Unknown Artist"
        binding.songTitle.isSelected = true
        binding.artistName.isSelected = true

        Glide.with(this)
            .load(metadata.artworkUri ?: R.drawable.default_thumbnail)
            .placeholder(R.drawable.default_thumbnail).into(binding.artWork)

        Glide.with(this).asBitmap()
            .load(metadata.artworkUri ?: R.drawable.default_thumbnail)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 15)))
            .transition(BitmapTransitionOptions.withCrossFade()).into(binding.backgroundArtwork)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        val binding = _binding ?: return
        binding.btnPlayPause.setImageResource(if (isPlaying) R.drawable.pause_with_circle else R.drawable.play_with_circle)
    }

    private fun updateShuffleRepeatUI() {

        binding.btnShuffle.alpha = if (player.shuffleModeEnabled) 1.0f else 0.5f

        val isRepeatOn = player.repeatMode != Player.REPEAT_MODE_OFF
        binding.btnRepeat.alpha = if (isRepeatOn) 1.0f else 0.5f
        val repeatIcon = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> R.drawable.repeat_all
            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
            else -> R.drawable.repeat
        }
        binding.btnRepeat.setImageResource(repeatIcon)

    }

    private fun formatDuration(ms: Int): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return String.Companion.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.removeListener(playerListener)
        handler.removeCallbacks(updateProgressAction)
        handler.removeCallbacks(lyricsUpdateAction)
        _binding = null
    }

    fun parseLrc(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("\\[(\\d{2}):(\\d{2})[\\.:](\\d{2,3})\\](.*)")
        lrcContent.lines().forEach { line ->
            regex.find(line)?.let { match ->
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msPart = match.groupValues[3]
                val ms = if (msPart.length == 2) msPart.toLong() * 10 else msPart.toLong()
                val totalTime = (min * 60 * 1000) + (sec * 1000) + ms
                lines.add(LyricLine(totalTime, match.groupValues[4].trim()))
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}