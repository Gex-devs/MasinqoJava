package gex.com.masinqojava

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gex.com.masinqojava.dataimport.AppDatabase
import gex.com.masinqojava.dataimport.ArtistImage
import gex.com.masinqojava.networkimport.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ArtistAdapter internal constructor(
    private var artists: ArrayList<MediaItem?>,
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onItemClick: (position: Int) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>(), Filterable {

    private var artistsFull: List<MediaItem?> = ArrayList(artists)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.artist_items_template, parent, false)
        return ArtistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val artist = artists[position] ?: return
        val metadata = artist.mediaMetadata
        // Split at comma and take the first name
        val fullArtistName = metadata.artist?.toString() ?: "Unknown artist"
        val artistName = fullArtistName.split(",", "/", "&", " - ")[0].trim()

        val albumCount = metadata.extras?.getInt("album_count") ?: 0
        val songCount = metadata.extras?.getInt("track_count") ?: 0
        
        holder.albumTitle.text = fullArtistName
        holder.numberOfSongs.text =
            context.resources.getQuantityString(R.plurals.track_count_labels, songCount, songCount)
        holder.numberOfAlbums.text = context.resources.getQuantityString(
            R.plurals.albums_count_labels,
            albumCount,
            albumCount
        )

        // 1. Initial load: Load the local representative art.
        Glide.with(context)
            .load(metadata.artworkUri)
            .placeholder(R.drawable.artists_temp)
            .error(R.drawable.artists_temp)
            .centerInside()
            .into(holder.albumArt)

        // 2. Reliable Network Fetch logic using coroutine
        lifecycleScope.launch {
            val artistDao = AppDatabase.getDatabase(context).artistDao()
            val localArtist = artistDao.getArtistByName(artistName)

            if (localArtist?.imageUrl != null) {
                withContext(Dispatchers.Main) {
                    Glide.with(context).load(localArtist.imageUrl).into(holder.albumArt)
                }
            } else {
                try {
                    val response = RetrofitInstance.api.searchArtist(artistName)
                    val deezerArtist = response.data.firstOrNull()

                    if (deezerArtist != null) {
                        val imageUrl = deezerArtist.picture_medium

                        artistDao.insertArtist(ArtistImage(Id= deezerArtist.id, artistName = artistName, imageUrl = imageUrl))

                        withContext(Dispatchers.Main) {
                            Glide.with(context).load(imageUrl).into(holder.albumArt)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Deezer", "Fetch failed: ${e.message}")
                }
            }
        }

        holder.itemView.setOnClickListener {
            try {
                val artistId = artist.mediaId.toLongOrNull() ?: -1L
                val fragment = OpenedArtistFragment().apply {
                    arguments = Bundle().apply {
                        putLong("ARTIST_ID", artistId)
                    }
                }
                val activity = context as? AppCompatActivity
                activity?.supportFragmentManager?.beginTransaction()
                    ?.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    ?.replace(R.id.main_fragment_container, fragment)
                    ?.addToBackStack(null)
                    ?.commit()
            } catch (e: Exception) {
                Log.e("ArtistAdapter", "Fragment transition failed: ${e.message}")
                onItemClick(position)
            }
        }
    }

    override fun getItemCount(): Int = artists.size

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterPattern = constraint.toString().trim().lowercase(Locale.ROOT)
            val filteredList = if (filterPattern.isEmpty()) {
                artistsFull
            } else {
                artistsFull.filter { item ->
                    item?.mediaMetadata?.artist?.toString()?.lowercase(Locale.ROOT)?.contains(filterPattern) == true
                }
            }
            return FilterResults().apply { values = filteredList }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            artists.clear()
            if (results?.values != null) {
                @Suppress("UNCHECKED_CAST")
                artists.addAll(results.values as List<MediaItem?>)
            }
            notifyDataSetChanged()
        }
    }

    class ArtistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var albumTitle: TextView = itemView.findViewById(R.id.artist_name)
        val albumArt: ImageView = itemView.findViewById(R.id.album_art)
        val numberOfAlbums: TextView = itemView.findViewById(R.id.number_of_albums)
        val numberOfSongs: TextView = itemView.findViewById(R.id.number_of_songs)
    }
}
