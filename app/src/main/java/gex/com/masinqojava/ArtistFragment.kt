package gex.com.masinqojava

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ArtistFragment : Fragment() {

    var recyclerView: RecyclerView? = null
    var artistAdapter: ArtistAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_artists, container, false)
        recyclerView = view.findViewById(R.id.artist_recycler)
        recyclerView?.setHasFixedSize(true)
        if ((MainActivity.artists?.isNotEmpty() == true)) {
            artistAdapter = ArtistAdapter(MainActivity.artists!!, requireContext()) { position ->
                (activity as? MainActivity)?.openArtist(position)
            }
            recyclerView?.setAdapter(artistAdapter)
            recyclerView?.setLayoutManager(
                LinearLayoutManager(
                    context,
                    RecyclerView.VERTICAL,
                    false
                )
            )
        } else {
            Toast.makeText(context, "No artists found!", Toast.LENGTH_SHORT).show()
        }
        return view
    }

    fun filterArtist(query: String) {
        artistAdapter?.filter?.filter(query)
    }
}

