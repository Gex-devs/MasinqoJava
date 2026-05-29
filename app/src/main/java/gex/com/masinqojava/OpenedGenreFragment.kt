package gex.com.masinqojava

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import gex.com.masinqojava.databinding.FragmentOpenedGenreBinding

class OpenedGenreFragment : Fragment() {

    private var _binding: FragmentOpenedGenreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOpenedGenreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val genreId = arguments?.getLong("GENRE_ID") ?: return
        val songs = MusicLoader.getSongsByGenre(requireContext(), genreId)
        val adapter = SongAdapter(songs, requireContext()) { position ->
            (activity as? MainActivity)?.let { main ->
                main.player?.setMediaItems(songs.filterNotNull(), position, 0L)
                main.player?.prepare()
                main.player?.play()
            }
        }
        binding.openedGenreRecycler.adapter = adapter

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}