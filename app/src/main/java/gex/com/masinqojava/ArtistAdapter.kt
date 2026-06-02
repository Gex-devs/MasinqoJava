package gex.com.masinqojava

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gex.com.masinqojava.R
import gex.com.masinqojava.gex.com.masinqojava.OpenedArtistFragment
import org.w3c.dom.Text
import java.util.Locale

class ArtistAdapter internal constructor(
    private val artists: ArrayList<MediaItem?>,
    private val context: Context,
    private val onItemClick: (position: Int) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>(), Filterable {

    private var artistsFull: List<MediaItem?> = ArrayList(artists)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.artist_items_template, parent, false)
        return ArtistViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ArtistViewHolder,
        position: Int
    ) {
        val artist = artists[position] ?: return
        val metadata = artist.mediaMetadata
        val albumCount = metadata.extras?.getInt("album_count") ?: 0
        val songCount = metadata.extras?.getInt("track_count") ?: 0
        holder.albumTitle.text = metadata.artist ?: "Unknown Artist"
        holder.numberOfSongs.text =
            context.resources.getQuantityString(R.plurals.track_count_labels, songCount, songCount)
        holder.numberOfAlbums.text = context.resources.getQuantityString(
            R.plurals.albums_count_labels,
            albumCount,
            albumCount
        )

        Glide.with(context).asBitmap()
            .load(metadata.artworkUri)
            .thumbnail(
                Glide.with(context)
                    .asBitmap()
                    .load(R.drawable.artists_temp)
                    .centerInside()
            )
            .placeholder(R.drawable.artists_temp)
            .error(R.drawable.artists_temp)
            .centerInside()
            .into(holder.albumArt)
        holder.itemView.setOnClickListener {
            try {
                val artistIdString = artist.mediaId
                val artistId = artistIdString.toLongOrNull() ?: -1L

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
                Log.d("ArtistAdapter", "Click failed: ${e.message}")
            }
            Log.d("ArtistAdapter", "Artist: ${metadata.artist}, Uri: ${metadata.artworkUri}")
        }
    }

    override fun getItemCount(): Int {
        val artistSize = artists.size
        return artistSize
    }

    override fun getFilter(): Filter? {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults? {
                val filterPattern = constraint.toString().trim().lowercase(Locale.ROOT) ?: ""
                val filteredList = if (filterPattern.isEmpty()) {
                    artistsFull
                }else{
                    artistsFull.filter { item ->
                        val metadata = item?.mediaMetadata
                        metadata?.artist?.toString()?.lowercase(Locale.ROOT)
                            ?.contains(filterPattern) == true || metadata?.title?.toString()?.lowercase(
                            Locale.ROOT)?.contains(filterPattern) == true
                    }
                }
                return FilterResults().apply { values = filteredList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                artists.clear()
                if (results?.values != null){
                    artists.addAll(results.values as List<MediaItem?>)
                }
                notifyDataSetChanged()
            }
        }
    }

    class ArtistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var albumTitle: TextView = itemView.findViewById(R.id.artist_name)
        val albumArt: ImageView = itemView.findViewById(R.id.album_art)
        val numberOfAlbums: TextView = itemView.findViewById(R.id.number_of_albums)
        val numberOfSongs: TextView = itemView.findViewById(R.id.number_of_songs)
    }
}