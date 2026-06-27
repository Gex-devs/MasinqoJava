package gex.com.masinqojava

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import gex.com.masinqojava.dataimport.LyricLine

class LyricAdapter(private var lyrics: List<LyricLine> = emptyList()) :
    RecyclerView.Adapter<LyricAdapter.LyricViewHolder>() {

    private var activeIndex = -1

    fun setData(newLyrics: List<LyricLine>) {
        lyrics = newLyrics
        activeIndex = -1
        notifyDataSetChanged()
    }

    fun updateActiveLine(timeMs: Long): Int {
        val newIndex = lyrics.indexOfLast { it.timeMs <= timeMs }
        if (newIndex != activeIndex) {
            val oldIndex = activeIndex
            activeIndex = newIndex
            if (oldIndex != -1) notifyItemChanged(oldIndex)
            if (activeIndex != -1) notifyItemChanged(activeIndex)
        }
        return activeIndex
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lyric, parent, false)
        return LyricViewHolder(view)
    }

    override fun onBindViewHolder(holder: LyricViewHolder, position: Int) {
        val line = lyrics[position]
        holder.lyricText.text = line.text
        
        if (position == activeIndex) {
            holder.lyricText.setTextColor(Color.WHITE)
            holder.lyricText.alpha = 1.0f
            holder.lyricText.scaleX = 1.1f
            holder.lyricText.scaleY = 1.1f
        } else {
            holder.lyricText.setTextColor(Color.GRAY)
            holder.lyricText.alpha = 0.4f
            holder.lyricText.scaleX = 1.0f
            holder.lyricText.scaleY = 1.0f
        }
    }

    override fun getItemCount(): Int = lyrics.size

    class LyricViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lyricText: TextView = itemView.findViewById(R.id.lyric_text)
    }
}
