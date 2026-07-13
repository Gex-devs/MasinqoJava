package gex.com.masinqojava

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PlaylistFragment : Fragment() {

    private val viewModel: PlaylistViewModel by activityViewModels()
    private lateinit var adapter: PlaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_playlist, container, false)
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.playlist_recycler)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_playlist)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)

        adapter = PlaylistAdapter(emptyList(), onItemClick = { playlist ->
            // Open playlist details (songs in playlist)
            val fragment = OpenedPlaylistFragment.newInstance(playlist.id, playlist.name)
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_fragment_container, fragment) // Assuming there is a fragment_container in activity_main
                .addToBackStack(null)
                .commit()
        }, onLongClick = { playlist ->
            showPlaylistOptions(playlist)
        })

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewModel.playlistItems.observe(viewLifecycleOwner) { playlists ->
            adapter.updateList(playlists)
        }

        fab.setOnClickListener {
            showCreatePlaylistDialog()
        }

        swipeRefresh.setOnRefreshListener {
            // Room is observable, so it might not need manual refresh if using Flow/LiveData
            swipeRefresh.isRefreshing = false
        }

        return view
    }

    private fun showCreatePlaylistDialog() {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("New Playlist")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createPlaylist(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPlaylistOptions(playlist: gex.com.masinqojava.dataimport.PlaylistEntitiy) {
        val options = arrayOf("Rename", "Delete")
        AlertDialog.Builder(requireContext())
            .setTitle(playlist.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(playlist)
                    1 -> viewModel.deletePlaylist(playlist)
                }
            }
            .show()
    }

    private fun showRenameDialog(playlist: gex.com.masinqojava.dataimport.PlaylistEntitiy) {
        val input = EditText(requireContext())
        input.setText(playlist.name)
        AlertDialog.Builder(requireContext())
            .setTitle("Rename Playlist")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.renamePlaylist(playlist.id, newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
