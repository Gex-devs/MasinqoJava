package gex.com.masinqojava

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.IOException
import java.util.Locale
import androidx.core.net.toUri
import androidx.media3.common.MediaItem

class SongAdapter internal constructor(
    private val songs: ArrayList<MediaItem?>,
    private val context: Context,
    private val onItemClick: (position: Int) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.audio_items_template, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]?: return
        val metadata = song.mediaMetadata
        holder.songTitle.text = metadata.title
        holder.songArtist.text = metadata.artist
        holder.songDuration.text = formatDuration(metadata.durationMs?.toInt())

        Glide.with(context)
            .asBitmap()
            .load(metadata.artworkUri)
            .thumbnail(
                Glide.with(context)
                    .asBitmap()
                    .load(R.drawable.ic_launcher_background)
                    .centerCrop()
            )
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_foreground)
            .centerCrop()
            .into(holder.albumArt)
        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    fun clear() {
        songs.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var songTitle: TextView = itemView.findViewById<TextView>(R.id.audio_title)
        var songArtist: TextView = itemView.findViewById<TextView>(R.id.audio_artist)
        var albumArt: ImageView = itemView.findViewById<ImageView>(R.id.audio_icon)
        var songDuration: TextView = itemView.findViewById<TextView>(R.id.audio_duration)
    }


    private fun formatDuration(rawDuration: Int?): String {
        val seconds = (rawDuration?.div(1000))?.rem(60)
        val minutes = (rawDuration?.div((1000 * 60)))?.rem(60)
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}
