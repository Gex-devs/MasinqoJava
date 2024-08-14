package gex.com.masinqojava;

import static gex.com.masinqojava.MainActivity.songs;

import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


public class SongFragment extends Fragment {

    RecyclerView recyclerView;
    SongAdapter songAdapter;


    public SongFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_song, container, false);
        recyclerView = view.findViewById(R.id.song_recycler);
        recyclerView.setHasFixedSize(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!(songs.isEmpty())){
                songAdapter = new SongAdapter(songs, getContext());
                recyclerView.setAdapter(songAdapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
            }else{
                Toast.makeText(getContext(), "No songs found", Toast.LENGTH_SHORT).show();
            }
        }
        return view;
    }
}