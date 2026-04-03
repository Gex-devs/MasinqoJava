package gex.com.masinqojava

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class SongFragment : Fragment() {
    var recyclerView: RecyclerView? = null
    var songAdapter: SongAdapter? = null

    var swipeRefreshLayout: SwipeRefreshLayout? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_song, container, false)
        recyclerView = view.findViewById<RecyclerView>(R.id.song_recycler)
        swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefreshLayout?.setOnRefreshListener {
            val newSongs = MainActivity.getSongs(requireContext())
            val oldSongs = MainActivity.songs ?: emptyList()

            val diffResult = DiffUtil.calculateDiff(SongDiffCallback(oldSongs, newSongs))

            MainActivity.songs?.clear()
            MainActivity.songs?.addAll(newSongs)
            songAdapter?.let{
                diffResult.dispatchUpdatesTo(it)
            }
            swipeRefreshLayout?.isRefreshing = false
        }
        recyclerView!!.setHasFixedSize(true)
            if ((MainActivity.songs?.isNotEmpty() == true)) {
                songAdapter = SongAdapter(MainActivity.songs!!, requireContext())
                recyclerView!!.setAdapter(songAdapter)
                recyclerView!!.setLayoutManager(
                    LinearLayoutManager(
                        getContext(),
                        RecyclerView.VERTICAL,
                        false
                    )
                )
            } else {
                Toast.makeText(getContext(), "No songs found", Toast.LENGTH_SHORT).show()
            }
        return view
    }
}