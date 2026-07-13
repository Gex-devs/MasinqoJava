package gex.com.masinqojava

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import gex.com.masinqojava.dataimport.PlaylistEntitiy

class PlaylistAdapter(
    private var playlists: List<PlaylistEntitiy>,
    private val onItemClick: (PlaylistEntitiy) -> Unit,
    private val onLongClick: ((PlaylistEntitiy) -> Unit)? = null
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.playlist_title)
        val icon: ImageView = itemView.findViewById(R.id.playlist_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.playlist_item_template, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.title.text = playlist.name
        holder.itemView.setOnClickListener { onItemClick(playlist) }
        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(playlist)
            true
        }
    }

    override fun getItemCount() = playlists.size

    fun updateList(newList: List<PlaylistEntitiy>) {
        playlists = newList
        notifyDataSetChanged()
    }
}
