package gex.com.masinqojava

import android.Manifest
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.media3.session.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.SessionToken
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlin.jvm.java


class MainActivity : AppCompatActivity() {
//    public var player: ExoPlayer? = null

    private lateinit var permissionManager: PermissionManager
    private lateinit var playerView: ConstraintLayout
    private lateinit var btnPlayPause: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var artworkView: ImageView
    private var controllerFuture: ListenableFuture<MediaController>? = null
    val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    val updateProgressAction = object : Runnable {
        override fun run() {
            controller?.let {
                if (it.isPlaying) {
                    progressBar.progress = it.currentPosition.toInt()

                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            setupUIWithPlayer(controller)
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        super.onStop()
        MediaController.releaseFuture(controllerFuture!!)
    }

    private fun setupUIWithPlayer(connectPlayer: Player?){
        if (connectPlayer == null) return

        connectPlayer.addListener(object : Player.Listener{
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                btnPlayPause.setImageResource(
                    if (isPlaying)R.drawable.pause_with_circle
                    else R.drawable.play_with_circle
                )
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                songTitle.text = mediaMetadata.title?: "Unknown Title"
                songArtist.text = mediaMetadata.artist?: "Unknown Artist"
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.custom_playback)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        progressBar = findViewById(R.id.progressBar)
        songTitle = findViewById(R.id.player_title)
        songArtist = findViewById(R.id.player_artist)

        findViewById<ImageButton>(R.id.btn_previous).setOnClickListener {
            controller?.seekToPreviousMediaItem()
        }
        findViewById<ImageButton>(R.id.btn_next).setOnClickListener {
            controller?.seekToNextMediaItem()
        }

        btnPlayPause.setOnClickListener {
            controller?.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.play()
                }
            }
        }
        findViewById<View>(R.id.meta_info).setOnClickListener {
            val bottomSheet = PlayerBottomSheet(controller!!)
            bottomSheet.show(supportFragmentManager, "PlayerBottomSheet")
        }
        controller?.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {

                    progressBar.max = controller?.duration?.toInt() ?: 0
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

                Glide.with(this@MainActivity).load(mediaMetadata.artworkUri)
                    .placeholder(R.drawable.default_thumbnail).error(R.drawable.default_thumbnail)
                    .into(songArtwork)

            }

        })

        permissionManager = PermissionManager(this) {
            initViewPager()
        }
        permissionManager.requestRuntimePermission()


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,

        ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.handlePermissionResult(requestCode, grantResults)
    }

    fun playSong(position: Int) {
        val playlist = songs?.filterNotNull() ?: return
        controller?.let {
            it.setMediaItems(playlist, position, 0L)
            it.prepare()
            it.play()
        }
    }

    fun openAlbum(position: Int) {
        val selectedAlbum = albums?.get(position)
        val albumId = selectedAlbum?.mediaId?.toLong()
        val albumSongs = MusicLoader.getSongsByAlbum(this, albumId!!)

        if (albumSongs.isNotEmpty()) {
            controller?.let {
                val playlist = albumSongs.filterNotNull()
                it.setMediaItems(playlist, 0, 0L)
                it.prepare()
                it.play()
            }
        }
    }

    fun openGenre(position: Int) {
        val selectedGenre = genres?.get(position)
        val genreId = selectedGenre?.mediaId?.toLong()
        val genreSongs = MusicLoader.getSongsByGenre(this, genreId!!)

        if (genreSongs.isNotEmpty()) {
            controller?.let {
                val playlist = genreSongs.filterNotNull()
                it.setMediaItems(playlist, 0, 0L)
                it.prepare()
                it.play()
            }
        }
    }

    fun openArtist(position: Int) {
        val selectedArtist = artists?.get(position)
        val artistId = selectedArtist?.mediaId?.toLong()
        val artistSongs = MusicLoader.getSongsByArtist(this, artistId!!)

        if (artistSongs.isNotEmpty()) {
            controller?.let {
                val playlist = artistSongs.filterNotNull()
                it.setMediaItems(playlist, 0, 0L)
                it.prepare()
                it.play()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressAction)
        controller?.release()
    }


    fun initViewPager() {
        val viewPager = findViewById<ViewPager2>(R.id.viewpager)
        val tabLayout = findViewById<TabLayout>(R.id.initView)
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        viewPagerAdapter.addFragment(GenreFragment(), "Genres")
        viewPagerAdapter.addFragment(ArtistFragment(), "Artists")
        viewPagerAdapter.addFragment(AlbumFragment(), "Albums")
        viewPagerAdapter.addFragment(SongFragment(), "Songs")
        viewPager.setAdapter(viewPagerAdapter)
        TabLayoutMediator(
            tabLayout, viewPager, (TabConfigurationStrategy { tab: TabLayout.Tab?, position: Int ->
                tab!!.setText(viewPagerAdapter.getTitle(position))
            })
        ).attach()
    }


    companion object {
        const val REQUEST_READ_STORAGE_PERMISSION = 100
        val READ_STORAGE_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        var songs: ArrayList<MediaItem?>? = null
        var albums: ArrayList<MediaItem?>? = null
        var artists: ArrayList<MediaItem?>? = null
        var genres: ArrayList<MediaItem?>? = null

    }
}
