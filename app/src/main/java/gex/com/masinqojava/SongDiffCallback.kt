package gex.com.masinqojava

import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil

class SongDiffCallback(private val oldList: List<MediaItem?>, private val newList: List<MediaItem?>) :
    DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
        return oldList[oldPos]?.mediaId == newList[newPos]?.mediaId
    }

    override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {

        val oldItem = oldList[oldPos]
        val newItem = newList[newPos]

        return oldItem?.mediaMetadata?.title == newItem?.mediaMetadata?.title &&
                oldItem?.mediaMetadata?.artist == newItem?.mediaMetadata?.artist
    }

}
