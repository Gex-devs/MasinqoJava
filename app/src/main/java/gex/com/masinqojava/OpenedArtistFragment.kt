package gex.com.masinqojava

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import gex.com.masinqojava.databinding.FragmentOpenedArtistBinding

class OpenedArtistFragment : Fragment() {
    private var _binding: FragmentOpenedArtistBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOpenedArtistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val artistId = arguments?.getLong("ARTIST_ID") ?: return
        val songs = MusicLoader.getSongsByArtist(requireContext(), artistId)
        val adapter = SongAdapter(songs, requireContext()) { position ->
            (activity as? MainActivity)?.let { main ->
                main.controller?.let { playerController ->
                    playerController.setMediaItems(songs.filterNotNull(), position, 0L)
                    playerController.prepare()
                    playerController.play()
                } ?: run {
                    Toast.makeText(requireContext(), "Player not ready", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.openedArtistRecycler.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}