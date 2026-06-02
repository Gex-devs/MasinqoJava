package gex.com.masinqojava

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GenreFragment : Fragment() {
    var recyclerView: RecyclerView? = null
    var genreAdapter: GenreAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_genres, container, false)
        recyclerView = view.findViewById(R.id.genre_recycler)
        recyclerView?.setHasFixedSize(true)
        if ((MainActivity.genres?.isNotEmpty() == true)) {
            genreAdapter = GenreAdapter(MainActivity.genres!!, requireContext()) { position ->
                (activity as? MainActivity)?.openGenre(position)
            }
            recyclerView?.setAdapter(genreAdapter)
            recyclerView?.setLayoutManager(
                LinearLayoutManager(
                    context,
                    RecyclerView.VERTICAL,
                    false
                )
            )
        } else {
            Toast.makeText(context, "No Genres available!", Toast.LENGTH_SHORT).show()
        }
        return view
    }

    fun filterGenre(query: String) {
        genreAdapter?.filter?.filter(query)
    }
}
