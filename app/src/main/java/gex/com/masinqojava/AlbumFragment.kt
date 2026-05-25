package gex.com.masinqojava

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class AlbumFragment : Fragment() {
    var recyclerView: RecyclerView? = null
    var albumAdapter: AlbumAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_albums, container, false)
        recyclerView = view.findViewById(R.id.album_recycler)
        recyclerView?.layoutManager = GridLayoutManager(context, 2)

        recyclerView?.setHasFixedSize(true)
        if ((MainActivity.albums?.isNotEmpty() == true)) {
            albumAdapter = AlbumAdapter(MainActivity.albums!!, requireContext()) { position ->
                (activity as? MainActivity)?.openAlbum(position)
            }
            recyclerView?.setAdapter(albumAdapter)
        } else {
            Toast.makeText(context, "No albums found!", Toast.LENGTH_SHORT).show()
        }
        return view
    }

}