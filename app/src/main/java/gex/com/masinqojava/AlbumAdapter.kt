package gex.com.masinqojava

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AlbumAdapter internal constructor(
    private val albums: ArrayList<MediaItem?>,
    private val context: Context,
    private val onItemClick: (position: Int) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.album_items_template, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = albums[position] ?: return
        val metadata = album.mediaMetadata
        val count = metadata.extras?.getInt("track_count")?:0
        holder.albumTitle.text = metadata.title
        holder.albumArtist.text = metadata.artist
        holder.numberOfSongs.text = context.resources.getQuantityString(R.plurals.track_count_labels, count, count)

        Glide.with(context)
            .asBitmap()
            .load(metadata.artworkUri)
            .thumbnail(
                Glide.with(context)
                    .asBitmap()
                    .load(R.drawable.album_temp)
                    .centerInside()
            )
            .placeholder(R.drawable.album_temp)
            .error(R.drawable.album_temp)
            .centerInside()
            .into(holder.albumArt)
        holder.itemView.setOnClickListener {
            try {
                val albumIdString = album.mediaId
                val albumId = albumIdString.toLongOrNull() ?: -1L

                val fragment = OpenedAlbumFragment().apply {
                    arguments = Bundle().apply {
                        putLong("ALBUM_ID", albumId)
                    }
                }

                val activity = context as? AppCompatActivity
                activity?.supportFragmentManager?.beginTransaction()
                    ?.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    ?.replace(R.id.main_fragment_container, fragment)
                    ?.addToBackStack(null)
                    ?.commit()
            } catch (e: Exception) {
                Log.d("AlbumAdapter", "Click failed: ${e.message}")
            }
        }
    }

    override fun getItemCount(): Int {
        val albumSize = albums.size
        return albumSize

    }

    class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var albumTitle: TextView = itemView.findViewById<TextView>(R.id.album_title)
        var albumArtist: TextView = itemView.findViewById<TextView>(R.id.album_artist)
        var albumArt: ImageView = itemView.findViewById<ImageView>(R.id.album_art)
        var numberOfSongs: TextView = itemView.findViewById<TextView>(R.id.number_of_songs)
    }
}

