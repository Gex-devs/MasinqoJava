package gex.com.masinqojava

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import gex.com.masinqojava.databinding.FragmentOpenedAlbumBinding

class OpenedAlbumFragment : Fragment() {
    private var _binding: FragmentOpenedAlbumBinding? = null
    private val binding get() = _binding!!
    private val playbackViewModel: PlaybackViewModel by activityViewModels()
    private val playlistViewModel: PlaylistViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOpenedAlbumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val albumId = arguments?.getLong("ALBUM_ID") ?: return
        val songs = MusicLoader.getSongsByAlbum(requireContext(), albumId)
        
        val adapter = SongAdapter(
            ArrayList(songs), requireContext(), playbackViewModel,
            onItemClick = { pos ->
                playbackViewModel.playList(songs, pos)
            }, onMoreOptionClick = { song ->
                val optionSheet = SongOptionBottomSheet(song, playbackViewModel, playlistViewModel)
                optionSheet.show(childFragmentManager, "SongOptions")
            })
            
        binding.openedAlbumRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.openedAlbumRecycler.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
