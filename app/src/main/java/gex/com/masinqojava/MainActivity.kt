package gex.com.masinqojava

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import gex.com.masinqojava.gex.com.masinqojava.MusicLoader


class MainActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null

    private lateinit var playerView: ConstraintLayout
    private lateinit var btnPlayPause: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var artworkView: ImageView
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    val updateProgressAction = object : Runnable {
        override fun run() {
            player?.let {
                if (it.isPlaying) {
                    progressBar.progress = it.currentPosition.toInt()

                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.custom_playback)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        progressBar = findViewById(R.id.progressBar)
        songTitle = findViewById(R.id.player_title)
        songArtist = findViewById(R.id.player_artist)
        player = ExoPlayer.Builder(this).build()

        findViewById<ImageButton>(R.id.btn_previous).setOnClickListener {
            player?.seekToPreviousMediaItem()
        }
        findViewById<ImageButton>(R.id.btn_next).setOnClickListener {
            player?.seekToNextMediaItem()
        }

        btnPlayPause.setOnClickListener {
            player?.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.play()
                }
            }
        }
        findViewById<View>(R.id.meta_info).setOnClickListener {
            val bottomSheet = PlayerBottomSheet(player!!)
            bottomSheet.show(supportFragmentManager, "PlayerBottomSheet")
        }
        player?.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {

                    progressBar.max = player?.duration?.toInt() ?: 0
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

                songTitle.isSelected = true
                songArtist.isSelected = true

                val songArtwork =
                    findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.art_work)

                Glide.with(this@MainActivity)
                    .load(mediaMetadata.artworkUri)
                    .placeholder(R.drawable.default_thumbnail)
                    .error(R.drawable.default_thumbnail)
                    .into(songArtwork)

            }

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
        handler.removeCallbacks(updateProgressAction)
        player?.release()
    }

    private fun requestRuntimePermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                READ_STORAGE_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            songs = MusicLoader().getSongs(this)
            albums = MusicLoader().getAlbums(this)
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
                arrayOf(READ_STORAGE_PERMISSION),
                REQUEST_READ_STORAGE_PERMISSION
            )
        }
    }


    private fun initViewPager() {
        val viewPager = findViewById<ViewPager2>(R.id.viewpager)
        val tabLayout = findViewById<TabLayout>(R.id.initView)
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
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
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                songs = MusicLoader().getSongs(this)
                albums = MusicLoader().getAlbums(this)
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
        var albums: ArrayList<MediaItem?>? = null

    }
}
