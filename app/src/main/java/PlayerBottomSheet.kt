package gex.com.masinqojava

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import gex.com.masinqojava.R
import gex.com.masinqojava.databinding.ExpandedPlaybackControlsBinding

class PlayerBottomSheet(private val player: ExoPlayer) : BottomSheetDialogFragment() {
    private var _binding: ExpandedPlaybackControlsBinding? = null

    private val binding get() = _binding!!

    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    val updateProgressAction = object : Runnable {
        override fun run() {
            player.let {
                if (it.isPlaying) {
                    binding.seekbar.progress = it.currentPosition.toInt()

                }
                handler.postDelayed(this, 1000)
            }
        }
    }
    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                binding.seekbar.max = player?.duration?.toInt() ?: 0
                handler.post(updateProgressAction)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {

            if (isPlaying) {
                binding.btnPlayPause.setImageResource(R.drawable.pause_with_circle)
                handler.post(updateProgressAction)
            } else {
                binding.btnPlayPause.setImageResource(R.drawable.play_with_circle)
                handler.removeCallbacks(updateProgressAction)
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
            binding.songTitle.text = mediaMetadata.title ?: "Unknown Title"
            binding.artistName.text = mediaMetadata.artist ?: "Unknown Artist"

            Glide.with(this@PlayerBottomSheet)
                .load(mediaMetadata.artworkUri)
                .placeholder(R.drawable.default_thumbnail)
                .error(R.drawable.default_thumbnail)
                .into(binding.artWork)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
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

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            binding.btnShuffle.alpha = if (shuffleModeEnabled) 1.0f else 0.5f
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ExpandedPlaybackControlsBinding.inflate(inflater, container, false)

        binding.btnPrevious.setOnClickListener {
            player?.seekToPreviousMediaItem()
        }
        binding.btnNext.setOnClickListener {
            player?.seekToNextMediaItem()
        }
        binding.seekbar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        player?.seekTo(progress.toLong())
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {

                }

            })

        binding.btnPlayPause.setOnClickListener {
            player?.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.play()
                }
            }
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

    override fun onDestroy() {
        super.onDestroy()
        player.removeListener(playerListener)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        player.removeListener(playerListener)
        handler.removeCallbacks(updateProgressAction)
        _binding = null


    }
}