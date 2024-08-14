package gex.com.masinqojava;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;

import com.google.android.gms.common.util.concurrent.HandlerExecutor;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_READ_STORAGE_PERMISSION = 100;
    private static final String READ_STORAGE_PERMISSION = Manifest.permission.READ_MEDIA_AUDIO;
    static ArrayList<SongTemplate> songs;
    private ContentObserver contentObserver;
    private SongAdapter songAdapter = new SongAdapter(songs, MainActivity.this);
    SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestRuntimePermission();
        songAdapter = new SongAdapter(songs, this);

    }





    private void requestRuntimePermission(){
        if (ActivityCompat.checkSelfPermission(this, READ_STORAGE_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            songs = getSongs(MainActivity.this);
            initViewPager();

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, READ_STORAGE_PERMISSION)) {
            AlertDialog.Builder  builder = new AlertDialog.Builder(this);
            builder.setMessage("This app requires storage permissions to function properly.")
                    .setTitle("Permission required")
                    .setCancelable(false)
                    .setPositiveButton("Allow", ((dialog, which) -> {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{READ_STORAGE_PERMISSION}, REQUEST_READ_STORAGE_PERMISSION);
                        dialog.dismiss();
                    }))
                    .setNegativeButton("Deny", (((dialog, which) -> dialog.dismiss())));
            builder.show();

        }else {
            ActivityCompat.requestPermissions(this, new String[]{READ_STORAGE_PERMISSION}, REQUEST_READ_STORAGE_PERMISSION);
        }

    }


    private void initViewPager() {
        ViewPager2 viewPager = findViewById(R.id.viewpager);
        TabLayout tabLayout = findViewById(R.id.initview);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPagerAdapter.addFragment(new SongFragment(), "Songs");
        viewPagerAdapter.addFragment(new AlbumFragment(), "Album");
        viewPager.setAdapter(viewPagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager, ((tab, position) -> {
            tab.setText(viewPagerAdapter.getTitle(position));
        })).attach();
    }

    public static ArrayList<SongTemplate> tempAudioList = new ArrayList<>();

    public static ArrayList<SongTemplate> getSongs(Context context){

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projectionDefinition = new String[]{
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA
        };
        Cursor cursor = context.getContentResolver().query(uri, projectionDefinition, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                String artist = cursor.getString(1);
                String duration = cursor.getString(2);
                String album = cursor.getString(3);
                String path = cursor.getString(4);

                SongTemplate songTemplate = new SongTemplate(title, artist, path, album, duration);
                Log.d("song list","path: " + path + " artist: " + artist);
                tempAudioList.add(songTemplate);
            }
            cursor.close();
        }
        return tempAudioList;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_READ_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initViewPager();

            }

            else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, READ_STORAGE_PERMISSION)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("This feature is unavailable because it requires permissions that have been denied")
                        .setTitle("Permission required")
                        .setCancelable(false)
                        .setNegativeButton("Cancel", ((dialog, which) -> dialog.dismiss()))
                        .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);

                                dialog.dismiss();
                            }
                        });
                builder.show();
            }
            else {
               requestRuntimePermission();
            }
        }
    }
}
