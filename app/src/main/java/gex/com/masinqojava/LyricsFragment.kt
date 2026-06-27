package gex.com.masinqojava

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
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
import gex.com.masinqojava.databinding.FragmentLyricsBinding
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.fragment.app.activityViewModels

class LyricsFragment(private val player: Player) : BottomSheetDialogFragment() {

    private var _binding: FragmentLyricsBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var lyricAdapter: LyricAdapter
    private var isLyricsLoading = false
    private lateinit var queueAdapter: SongAdapter

    private val viewModel: PlaybackViewModel by activityViewModels()

    private val lyricsUpdateAction = object : Runnable {
        override fun run() {
            if (_binding != null && ::lyricAdapter.isInitialized && player.isPlaying) {
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
            if (_binding == null) return
            if (playbackState == Player.STATE_READY) {
                val duration = player.duration.toInt()
                binding.seekbar.max = duration
                binding.totalTime.text = formatDuration(duration)
                handler.post(updateProgressAction)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (_binding == null) return
            updatePlayPauseIcon(isPlaying)
            if (isPlaying) {
                handler.post(updateProgressAction)
                handler.post(lyricsUpdateAction)
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

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet =
            dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLyricsBinding.inflate(inflater, container, false)
        player.addListener(playerListener)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lyricAdapter = LyricAdapter()
        binding.lyricsRecyclerView.adapter = lyricAdapter
        binding.lyricsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        queueAdapter = SongAdapter(ArrayList(), requireContext()) { position ->
            val upcoming = viewModel.upcomingItems.value
            if (position in upcoming.indices) {
                val targetIndex = upcoming[position].first
                player.seekTo(targetIndex, 0L)
                player.play()
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lyrics.collect { lyrics->
                lyricAdapter.setData(lyrics)
                updateLyricsUIState()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { loading->
                isLyricsLoading = loading
                updateLyricsUIState()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.upcomingItems.collect { pairs ->
                val displayList = pairs.map { it.second }
                queueAdapter.updateList(displayList)
            }
        }

        // Initial updates
        updatePlayPauseIcon(player.isPlaying)
        updateMetadataUI(player.mediaMetadata)

        if (player.playbackState == Player.STATE_READY) {
            val duration = player.duration.toInt()
            binding.seekbar.max = duration
            binding.totalTime.text = formatDuration(duration)
        }
        
        if (player.isPlaying) {
            handler.post(updateProgressAction)
            handler.post(lyricsUpdateAction)
        }

        binding.btnPlayPause.setOnClickListener {
            if (player.isPlaying) player.pause() else player.play()
        }
        binding.btnShuffle.setOnClickListener {
            viewModel.toggleShuffle()
            updateShuffleRepeatUI()
        }
        binding.btnRepeat.setOnClickListener {
            viewModel.toggleRepeat()
            updateShuffleRepeatUI()
        }
        
        binding.btnPrevious.setOnClickListener { player.seekToPreviousMediaItem() }
        binding.btnNext.setOnClickListener { player.seekToNextMediaItem() }
        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Tell ViewModel to seek and update the internal StateFlow
                    viewModel.seekTo(p.toLong())

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
                viewModel.setUserSeeking(true)
            }

            override fun onStopTrackingTouch(s: SeekBar?) {
                // Resume normal progress updates
                viewModel.setUserSeeking(false)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.removeListener(playerListener)
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }

    private fun updateMetadataUI(metadata: MediaMetadata) {
        if (_binding == null) return
        binding.songTitle.text = metadata.title ?: "Unknown Title"
        binding.artistName.text = metadata.artist ?: "Unknown Artist"

        Glide.with(this)
            .asBitmap()
            .load(metadata.artworkUri ?: R.drawable.default_thumbnail)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 15)))
            .transition(BitmapTransitionOptions.withCrossFade())
            .into(binding.backgroundArtwork)
    }

    private fun updateLyricsUIState() {
        if (_binding == null || !::lyricAdapter.isInitialized) return
        if (isLyricsLoading) {
            binding.lyricsShimmerView.visibility = View.VISIBLE
            binding.lyricsRecyclerView.visibility = View.GONE
            binding.lyricsEmptyText.visibility = View.GONE
        } else if (lyricAdapter.itemCount > 0) {
            binding.lyricsShimmerView.visibility = View.GONE
            binding.lyricsRecyclerView.visibility = View.VISIBLE
            binding.lyricsEmptyText.visibility = View.GONE
        } else {
            binding.lyricsShimmerView.visibility = View.GONE
            binding.lyricsRecyclerView.visibility = View.GONE
            binding.lyricsEmptyText.visibility = View.VISIBLE
        }
    }

    private fun centerLyricLine(position: Int) {
        if (_binding == null) return
        val scroller = object : LinearSmoothScroller(requireContext()) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START
            override fun calculateDtToFit(vS: Int, vE: Int, bS: Int, bE: Int, sP: Int): Int =
                (bS + (bE - bS) / 2) - (vS + (vE - vS) / 2)
        }
        scroller.targetPosition = position
        binding.lyricsRecyclerView.layoutManager?.startSmoothScroll(scroller)
    }

    private fun formatDuration(ms: Int): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        if (_binding == null) return
        binding.btnPlayPause.setImageResource(if (isPlaying) R.drawable.pause_with_circle else R.drawable.play_with_circle)
    }
    private fun updateShuffleRepeatUI() {

        binding.btnShuffle.alpha = if (player.shuffleModeEnabled) 1.0f else 0.5f

        val isRepeatOn = player.repeatMode != Player.REPEAT_MODE_OFF
        binding.btnRepeat.alpha = if (isRepeatOn) 1.0f else 0.5f
        val repeatIcon = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> gex.com.masinqojava.R.drawable.repeat_all
            Player.REPEAT_MODE_ONE -> gex.com.masinqojava.R.drawable.repeat_one
            else -> gex.com.masinqojava.R.drawable.repeat
        }
        binding.btnRepeat.setImageResource(repeatIcon)

    }
}
