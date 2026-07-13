package gex.com.masinqojava

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import gex.com.masinqojava.databinding.FragmentOpenedGenreBinding

class OpenedGenreFragment : Fragment() {

    private var _binding: FragmentOpenedGenreBinding? = null
    private val binding get() = _binding!!
    private val playbackViewModel: PlaybackViewModel by activityViewModels()
    private val playlistViewModel: PlaylistViewModel by activityViewModels()


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
        val adapter = SongAdapter(
            ArrayList(songs), requireContext(), playbackViewModel,
            onItemClick = { pos ->
                playbackViewModel.playList(songs, pos)
            }, onMoreOptionClick = { song ->
                val optionSheet = SongOptionBottomSheet(song, playbackViewModel, playlistViewModel)
                optionSheet.show(childFragmentManager, "SongOptions")

            })
        binding.openedGenreRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.openedGenreRecycler.adapter = adapter

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}