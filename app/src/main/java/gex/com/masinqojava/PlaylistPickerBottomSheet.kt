package gex.com.masinqojava

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class PlaylistPickerBottomSheet(private val song: MediaItem) : BottomSheetDialogFragment() {
    private val viewModel: PlaylistViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.playlist_picker_bottomsheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.playlist_recycler_view)
        val btnCreateNew = view.findViewById<MaterialButton>(R.id.btn_create_new)

        val adapter = PlaylistAdapter(emptyList(), onItemClick = { playlist ->
            viewModel.addSongToPlaylist(song, playlist.id)
            dismiss()
        })

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewModel.playlistItems.observe(viewLifecycleOwner) { playlists ->
            adapter.updateList(playlists)
        }

        btnCreateNew.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun showCreatePlaylistDialog() {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("New Playlist")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                Log.d(name, "Playlist name")
                if (name.isNotEmpty()) {
                    viewModel.createPlaylist(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

    }
}
