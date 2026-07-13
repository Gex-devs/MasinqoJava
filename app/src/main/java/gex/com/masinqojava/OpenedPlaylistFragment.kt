package gex.com.masinqojava

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

class OpenedPlaylistFragment : Fragment() {

    private val playlistViewModel: PlaylistViewModel by activityViewModels()
    private val playbackViewModel: PlaybackViewModel by activityViewModels()
    private lateinit var adapter: SongAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_opened_playlist, container, false)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val recyclerView = view.findViewById<RecyclerView>(R.id.playlist_songs_recycler)

        val playlistId = arguments?.getLong(ARG_PLAYLIST_ID) ?: -1L
        val playlistName = arguments?.getString(ARG_PLAYLIST_NAME) ?: "Playlist"

        toolbar.title = playlistName
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = SongAdapter(
            ArrayList(),
            requireContext(),
            playbackViewModel,
            onItemClick = { position ->
                playbackViewModel.playList(adapter.getFilterableList(), position)
            },
            onMoreOptionClick = { song ->
                val optionSheet = SongOptionBottomSheet(song, playbackViewModel, playlistViewModel).apply {
                    arguments = Bundle().apply {
                        putLong("PLAYLIST_ID", playlistId)
                    }
                }
                optionSheet.show(childFragmentManager, "SongOptions")
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        playlistViewModel.currentPlaylistSongs.observe(viewLifecycleOwner) { songs ->
            adapter.updateList(songs)
        }

        playlistViewModel.loadSongsForPlaylist(playlistId)

        return view
    }

    companion object {
        private const val ARG_PLAYLIST_ID = "playlist_id"
        private const val ARG_PLAYLIST_NAME = "playlist_name"

        fun newInstance(id: Long, name: String) = OpenedPlaylistFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_PLAYLIST_ID, id)
                putString(ARG_PLAYLIST_NAME, name)
            }
        }
    }
}
