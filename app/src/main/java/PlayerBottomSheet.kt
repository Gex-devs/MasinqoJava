package gex.com.masinqojava

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import gex.com.masinqojava.databinding.ExpandedPlaybackControlsBinding
import jp.wasabeef.glide.transformations.BlurTransformation
import java.util.Locale

class PlayerBottomSheet(private val player: Player) : BottomSheetDialogFragment() {
    private var _binding: ExpandedPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())

    private val upcomingItems = mutableListOf<Pair<Int, MediaItem>>()
    private lateinit var queueAdapter: SongAdapter
    private val updateProgressAction = object : Runnable {
        override fun run() {
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
            if (playbackState == Player.STATE_READY) {
                val duration = player.duration.toInt()
                binding.seekbar.max = duration
                binding.totalTime.text = formatDuration(duration)
                handler.post(updateProgressAction)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayPauseIcon(isPlaying)
            if (isPlaying) {
                handler.post(updateProgressAction)
            } else {
                handler.removeCallbacks(updateProgressAction)
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            updateMetadataUI(mediaMetadata)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            updateRepeatIcon(repeatMode)
            updateShadowQueue()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            updateShuffleIcon(shuffleModeEnabled)
            updateShadowQueue()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ExpandedPlaybackControlsBinding.inflate(inflater, container, false)

        binding.btnPrevious.setOnClickListener { player.seekToPreviousMediaItem() }
        binding.btnNext.setOnClickListener { player.seekToNextMediaItem() }

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player.seekTo(progress.toLong())
                    binding.currentTime.text = formatDuration(progress)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        binding.btnPlayPause.setOnClickListener {
            if (player.isPlaying) player.pause() else player.play()
        }

        binding.btnShuffle.setOnClickListener {
            player.shuffleModeEnabled = !player.shuffleModeEnabled
        }

        binding.btnRepeat.setOnClickListener {
            player.repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }

        player.addListener(playerListener)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        queueAdapter = SongAdapter(ArrayList(), requireContext()) { listPosition ->

            player.seekTo(upcomingItems[listPosition].first, 0)
            player.play()
        }
        binding.currentQueue.layoutManager = LinearLayoutManager(context)
        binding.currentQueue.adapter = queueAdapter
        updateShadowQueue()
        super.onViewCreated(view, savedInstanceState)
        syncUI()
        binding.btnQueueList.setOnClickListener {
            if (binding.nestedQueueContainer.isGone) {

                binding.nestedQueueContainer.visibility = View.VISIBLE
                binding.artWork.visibility = View.GONE
                binding.appBarLayout.updateLayoutParams {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            } else {
                binding.nestedQueueContainer.visibility = View.GONE
                binding.artWork.visibility = View.VISIBLE
                binding.appBarLayout.updateLayoutParams {
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
        }
    }

    private fun updateShadowQueue(){
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return
        upcomingItems.clear()
        var nextIndex = timeline.getNextWindowIndex(
            player.currentMediaItemIndex,
            player.repeatMode,
            player.shuffleModeEnabled
        )
        val seenIndices = mutableSetOf<Int>()
        seenIndices.add(player.currentMediaItemIndex)

        val window = Timeline.Window()
        while (nextIndex != C.INDEX_UNSET && !seenIndices.contains(nextIndex)){
            upcomingItems.add(nextIndex to timeline.getWindow(nextIndex, window).mediaItem)
            seenIndices.add(nextIndex)
            nextIndex = timeline.getNextWindowIndex(nextIndex, player.repeatMode, player.shuffleModeEnabled)
            if (upcomingItems.size >= 50) break
        }
        queueAdapter.updateList(upcomingItems.map { it.second })
    }
    private fun syncUI() {
        updateMetadataUI(player.mediaMetadata)
        updatePlayPauseIcon(player.isPlaying)
        updateShuffleIcon(player.shuffleModeEnabled)
        updateRepeatIcon(player.repeatMode)

        val duration = player.duration.toInt()
        if (duration > 0) {
            binding.seekbar.max = duration
            binding.totalTime.text = formatDuration(duration)
        }

        val currentPos = player.currentPosition.toInt()
        binding.seekbar.progress = currentPos
        binding.currentTime.text = formatDuration(currentPos)

        if (player.isPlaying) {
            handler.post(updateProgressAction)
        }
    }

    private fun updateMetadataUI(metadata: MediaMetadata) {
        binding.songTitle.text = metadata.title ?: "Unknown Title"
        binding.artistName.text = metadata.artist ?: "Unknown Artist"
        binding.songTitle.isSelected = true
        binding.artistName.isSelected = true

        // Main icon artwork
        Glide.with(this)
            .load(metadata.artworkUri)
            .placeholder(R.drawable.default_thumbnail)
            .error(R.drawable.default_thumbnail)
            .into(binding.artWork)

        // Frosted background optimizations:
        // 1. .asBitmap() to handle vector fallback blurring.
        // 2. sampling = 15 for maximum performance.
        // 3. .placeholder(current_drawable) to prevent flashing.
        // 4. Fallback to default_thumbnail ensures it is also blurred.
        Glide.with(this)
            .asBitmap()
            .load(metadata.artworkUri ?: R.drawable.default_thumbnail)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 15)))
            .transition(BitmapTransitionOptions.withCrossFade())
            .into(binding.backgroundArtwork)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.pause_with_circle else R.drawable.play_with_circle
        )
    }

    private fun updateShuffleIcon(shuffleModeEnabled: Boolean) {
        binding.btnShuffle.alpha = if (shuffleModeEnabled) 1.0f else 0.5f
    }

    private fun updateRepeatIcon(repeatMode: Int) {
        when (repeatMode) {
            Player.REPEAT_MODE_ALL -> {
                binding.btnRepeat.setImageResource(R.drawable.repeat_all)
                binding.btnRepeat.alpha = 1.0f
            }

            Player.REPEAT_MODE_ONE -> {
                binding.btnRepeat.setImageResource(R.drawable.repeat_one)
                binding.btnRepeat.alpha = 1.0f
            }

            Player.REPEAT_MODE_OFF -> {
                binding.btnRepeat.setImageResource(R.drawable.repeat)
                binding.btnRepeat.alpha = 0.5f
            }
        }
    }

    private fun formatDuration(ms: Int): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        player.removeListener(playerListener)
        handler.removeCallbacks(updateProgressAction)
        _binding = null
    }
}
