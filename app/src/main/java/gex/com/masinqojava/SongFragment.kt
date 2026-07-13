package gex.com.masinqojava

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class SongFragment : Fragment() {
    var recyclerView: RecyclerView? = null
    var songAdapter: SongAdapter? = null

    var swipeRefreshLayout: SwipeRefreshLayout? = null

    private val newSongs by lazy { MusicLoader.getSongs(requireContext()) }
    val oldSongs = MainActivity.songs ?: emptyList()
    private val playbackViewModel: PlaybackViewModel by activityViewModels()
    private val playlistViewModel: PlaylistViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_song, container, false)
        recyclerView = view.findViewById(R.id.song_recycler)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh)
        swipeRefreshLayout?.setOnRefreshListener {

            val diffResult = DiffUtil.calculateDiff(SongDiffCallback(oldSongs, newSongs))

            MainActivity.songs?.clear()
            MainActivity.songs?.addAll(newSongs)
            songAdapter?.let {
                diffResult.dispatchUpdatesTo(it)
            }
            swipeRefreshLayout?.isRefreshing = false
        }
        recyclerView!!.setHasFixedSize(true)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val songs = MainActivity.songs
        if (!songs.isNullOrEmpty()){
            songAdapter = SongAdapter(
                songs = songs,
                context = requireContext(),
                viewModel = playbackViewModel,
                onItemClick = {pos->
                    playbackViewModel.playList(songs,pos)
                },
                onMoreOptionClick = {song ->
                    val optionSheet = SongOptionBottomSheet(song, playbackViewModel,playlistViewModel)
                    optionSheet.show(childFragmentManager, "SongOptions")
                }
            )
            recyclerView?.adapter = songAdapter
            recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        }else{
            Toast.makeText(context, "No songs found", Toast.LENGTH_SHORT).show()
        }
    }

    fun filterSongs(query: String) {
        songAdapter?.filter?.filter(query)
    }

}