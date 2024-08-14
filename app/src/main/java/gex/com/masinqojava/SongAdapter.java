package gex.com.masinqojava;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.IOException;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;

public class SongAdapter extends RecyclerView.Adapter {


    private final Context context;
    private final ArrayList<SongTemplate> songs;


    SongAdapter(ArrayList<SongTemplate> songs, Context context) {
        this.songs = songs;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.audio_items_template, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        SongViewHolder songViewHolder = (SongViewHolder) holder;
        songViewHolder.songTitle.setText(songs.get(position).getTitle());
        songViewHolder.songArtist.setText(songs.get(position).getArtist());
        songViewHolder.songDuration.setText(songs.get(position).getDuration());
        byte[] image = getAlbumArt(songs.get(position).getPath());
        if (image != null){
            Glide.with(context).asBitmap()
                    .load(image)
                    .into(songViewHolder.album_art);
        }
        else {
            Glide.with(context).asBitmap()
                    .load(R.drawable.ic_launcher_background)
                    .into(songViewHolder.album_art);
        }
    }

    public void clear(){
        songs.clear();
        notifyDataSetChanged();
    }
    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {

        TextView songTitle;
        TextView songArtist;
        TextView songDuration;
        ImageView album_art;
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            songTitle = itemView.findViewById(R.id.audio_title);
            songArtist = itemView.findViewById(R.id.audio_artist);
            album_art = itemView.findViewById(R.id.audio_icon);
            songDuration = itemView.findViewById(R.id.audio_duration);
        }
    }
    private byte[] getAlbumArt(String uri){

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            try {
            metadataRetriever.setDataSource(uri);
            byte[] art = metadataRetriever.getEmbeddedPicture();
            metadataRetriever.release();
            return art;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }
}
