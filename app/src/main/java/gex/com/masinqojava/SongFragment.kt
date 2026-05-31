package gex.com.masinqojava

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.time.temporal.TemporalQuery


class SongFragment : Fragment() {
    var recyclerView: RecyclerView? = null
    var songAdapter: SongAdapter? = null

    var swipeRefreshLayout: SwipeRefreshLayout? = null

    private val newSongs by lazy { MusicLoader.getSongs(requireContext()) }
    val oldSongs = MainActivity.songs ?: emptyList()


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
        if ((MainActivity.songs?.isNotEmpty() == true)) {
            songAdapter = SongAdapter(MainActivity.songs!!, requireContext()) { position ->
                (activity as? MainActivity)?.playSong(position)
            }
            recyclerView!!.setAdapter(songAdapter)
            recyclerView!!.setLayoutManager(
                LinearLayoutManager(
                    context,
                    RecyclerView.VERTICAL,
                    false
                )
            )
        } else {
            Toast.makeText(context, "No songs found", Toast.LENGTH_SHORT).show()
        }
        return view
    }

    fun filter(query: String) {
        songAdapter?.filter?.filter(query)
    }

}