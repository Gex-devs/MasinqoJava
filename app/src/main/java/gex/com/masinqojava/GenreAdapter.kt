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


class GenreAdapter internal constructor(
    private val genres: ArrayList<MediaItem?>,
    private val context: Context,
    private val onItemClick: (position: Int) -> Unit
) : RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.genre_items_template, parent, false)
        return GenreViewHolder(view)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val genre = genres[position] ?: return
        val metadata = genre.mediaMetadata
        val count = metadata.extras?.getInt("track_count") ?: 0
        holder.genreName.text = metadata.title ?: "Unknown Genre"
        holder.numberOfSongs.text =
            context.resources.getQuantityString(R.plurals.track_count_labels, count, count)

        holder.itemView.setOnClickListener {
            try {
                val genreIdString = genre.mediaId
                val genreId = genreIdString.toLongOrNull() ?: -1L

                val fragment = OpenedGenreFragment().apply {
                    arguments = Bundle().apply {
                        putLong("GENRE_ID", genreId)
                    }
                }
                val activity = context as? AppCompatActivity
                activity?.supportFragmentManager?.beginTransaction()
                    ?.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    ?.replace(R.id.main_fragment_container, fragment)
                    ?.addToBackStack(null)
                    ?.commit()
            }catch (e: Exception){
                Log.d("GenreAdapter", "Click failed: ${e.message}")
            }

        }

    }

    override fun getItemCount(): Int {
        return genres.size
    }

    class GenreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var genreImage: ImageView = itemView.findViewById(R.id.genre_button)
        var genreName: TextView = itemView.findViewById(R.id.genre_name)
        var numberOfSongs: TextView = itemView.findViewById(R.id.number_of_songs)
    }
}