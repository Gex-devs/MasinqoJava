package gex.com.masinqojava

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import org.w3c.dom.Text


class MainActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        player = ExoPlayer.Builder(this).build()

        val playerView = findViewById<ConstraintLayout>(R.id.custom_playback)
        val btnPlayPause = playerView.findViewById<ImageButton>(R.id.btn_play_pause)
        val seekBar = findViewById<SeekBar>(R.id.seekbar)
        val songTitle = findViewById<TextView>(R.id.player_title)
        val songArtist = findViewById<TextView>(R.id.player_artist)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val updateProgressAction = object : Runnable {
            override fun run() {
                player?.let {
                    if (it.isPlaying) {
                        seekBar.progress = it.currentPosition.toInt()

                    }
                    handler.postDelayed(this, 1000)
                }
            }
        }

        findViewById<ImageButton>(R.id.btn_previous).setOnClickListener {
            player?.seekToPreviousMediaItem()
        }
        findViewById<ImageButton>(R.id.btn_next).setOnClickListener {
            player?.seekToNextMediaItem()
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player?.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnPlayPause.setOnClickListener {
            player?.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.play()
                }
            }
        }
        player?.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {

                    seekBar.max = player?.duration?.toInt() ?: 0
                    handler.post(updateProgressAction)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    btnPlayPause.setImageResource(R.drawable.pause_with_circle)
                    handler.post(updateProgressAction)
                } else {
                    btnPlayPause.setImageResource(R.drawable.play_with_circle)
                    handler.removeCallbacks(updateProgressAction)
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                songTitle.text = mediaMetadata.title ?: "Unknown Title"
                songArtist.text = mediaMetadata.artist ?: "Unknown Artist"

                val songArtwork = findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.art_work)
                Glide.with(this@MainActivity)
                    .load(mediaMetadata.artworkUri)
                    .into(songArtwork)
            }

//            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
//
//                val playerView = findViewById<androidx.media3.ui.PlayerView>(R.id.player_view)
//                playerView.player = player
//            }

        })

        requestRuntimePermission()

    }

    fun playSong(position: Int) {
        val playlist = songs?.filterNotNull() ?: return
        player?.let {
            it.setMediaItems(playlist, position, 0L)
            it.prepare()
            it.play()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }

    private fun requestRuntimePermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                READ_STORAGE_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            songs = getSongs(this@MainActivity)
            initViewPager()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                READ_STORAGE_PERMISSION
            )
        ) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("This app requires storage permissions to function properly.")
                .setTitle("Permission required")
                .setCancelable(false)
                .setPositiveButton(
                    "Allow",
                    (DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        ActivityCompat.requestPermissions(
                            this@MainActivity, arrayOf<String>(
                                READ_STORAGE_PERMISSION
                            ), REQUEST_READ_STORAGE_PERMISSION
                        )
                        dialog?.dismiss()
                    })
                )
                .setNegativeButton(
                    "Deny",
                    ((DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() }))
                )
            builder.show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(READ_STORAGE_PERMISSION),
                REQUEST_READ_STORAGE_PERMISSION
            )
        }
    }


    private fun initViewPager() {
        val viewPager = findViewById<ViewPager2>(R.id.viewpager)
        val tabLayout = findViewById<TabLayout>(R.id.initview)
        val viewPagerAdapter = ViewPagerAdapter(getSupportFragmentManager(), lifecycle)
        viewPagerAdapter.addFragment(SongFragment(), "Songs")
        viewPagerAdapter.addFragment(AlbumFragment(), "Album")
        viewPager.setAdapter(viewPagerAdapter)
        TabLayoutMediator(
            tabLayout,
            viewPager,
            (TabConfigurationStrategy { tab: TabLayout.Tab?, position: Int ->
                tab!!.setText(viewPagerAdapter.getTitle(position))
            })
        ).attach()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_READ_STORAGE_PERMISSION) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                songs = getSongs(this)
                initViewPager()
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    READ_STORAGE_PERMISSION
                )
            ) {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("This feature is unavailable because it requires permissions that have been denied")
                    .setTitle("Permission required")
                    .setCancelable(false)
                    .setNegativeButton(
                        "Cancel",
                        (DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() })
                    )
                    .setPositiveButton("Settings", object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", getPackageName(), null)
                            intent.setData(uri)
                            startActivity(intent)

                            dialog.dismiss()
                        }
                    })
                builder.show()
            } else {
                requestRuntimePermission()
            }
        }
    }

    companion object {
        private const val REQUEST_READ_STORAGE_PERMISSION = 100
        private val READ_STORAGE_PERMISSION =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        var songs: ArrayList<MediaItem?>? = null

        fun getSongs(context: Context): ArrayList<MediaItem?> {
            val tempAudioList = ArrayList<MediaItem?>()
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val title = cursor.getString(0)
                    val artist = cursor.getString(1)
                    val duration = cursor.getLong(2)
                    val path = cursor.getString(3)
                    val albumId = cursor.getLong(5)
                    val artUri = ContentUris.withAppendedId(
                        "content://media/external/audio/albumart".toUri(),
                        albumId
                    )

                    val mediaItem = MediaItem.Builder()
                        .setMediaId(path)
                        .setUri(path)
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(title)
                                .setArtist(artist)
                                .setDurationMs(duration)
                                .setArtworkUri(artUri)
                                .build()
                        )
                        .build()
                    tempAudioList.add(mediaItem)
                    Log.d("De Song list", "path: $path artist: $artist")
                }
            }
            return tempAudioList
        }


//        fun getSongs(context: Context): ArrayList<SongTemplate?> {
//            tempAudioList.clear()
//
//            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
//            val projectionDefinition: Array<String?> = arrayOf(
//                MediaStore.Audio.Media.TITLE,
//                MediaStore.Audio.Media.ARTIST,
//                MediaStore.Audio.Media.DURATION,
//                MediaStore.Audio.Media.ALBUM,
//                MediaStore.Audio.Media.DATA,
//                MediaStore.Audio.Media.ALBUM_ID
//            )
//            val cursor =
//                context.getContentResolver().query(uri, projectionDefinition, null, null, null)
//            if (cursor != null) {
//                while (cursor.moveToNext()) {
//                    val title = cursor.getString(0)
//                    val artist = cursor.getString(1)
//                    val duration = cursor.getString(2)
//                    val album = cursor.getString(3)
//                    val path = cursor.getString(4)
//                    val albumId = cursor.getLong(5)
//
//                    val songTemplate = SongTemplate(title, artist, path, album, duration, albumId)
//                    Log.d("song list", "path: " + path + " artist: " + artist)
//                    tempAudioList.add(songTemplate)
//                }
//                cursor.close()
//            }
//
//            Log.d("MusicDebug", "Songs found: " + tempAudioList.size)
//            return tempAudioList
//        }
    }
}
