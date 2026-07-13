package gex.com.masinqojava

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.media3.common.MediaItem
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class SongOptionBottomSheet(
    private val song: MediaItem,
    private val playbackViewModel: PlaybackViewModel,
    private val playlistViewModel: PlaylistViewModel
) : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.song_options_bottomsheet, container, false)
        view.findViewById<MaterialButton>(R.id.btn_play).setOnClickListener {
            playbackViewModel.play(song)
            dismiss()
        }
        view.findViewById<MaterialButton>(R.id.btn_play_next).setOnClickListener {
            playbackViewModel.playNext(song)
            dismiss()
        }
        view.findViewById<MaterialButton>(R.id.btn_queue_add).setOnClickListener {
            playbackViewModel.addToQueue(song)
            Toast.makeText(requireContext(), "${song.mediaMetadata.title} added to end of queue", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        view.findViewById<MaterialButton>(R.id.btn_queue_remove).setOnClickListener {
            playbackViewModel.removeFromQueue(song)
            dismiss()
        }
        view.findViewById<MaterialButton>(R.id.btn_playlist_add).setOnClickListener {
            dismiss()
            val picker = PlaylistPickerBottomSheet(song)
            picker.show(parentFragmentManager, "PlaylistPicker")
        }
        view.findViewById<MaterialButton>(R.id.btn_playlist_remove).setOnClickListener {
            val currentPlaylistId = arguments?.getLong("PLAYLIST_ID") ?: -1L
            if (currentPlaylistId != -1L) {
                playlistViewModel.removeFromPlaylist(song, currentPlaylistId)
                dismiss()
            }
        }
        return view
    }
}
