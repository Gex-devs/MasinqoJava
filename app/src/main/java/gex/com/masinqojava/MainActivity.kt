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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.SessionToken
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var playerView: ConstraintLayout
    private lateinit var btnPlayPause: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var songArtwork: ShapeableImageView
    private lateinit var backgroundArtwork: ImageView
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val viewModel: PlaybackViewModel by viewModels()
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
        handler.removeCallbacks(updateProgressAction)
        controller?.let {
            MediaController.releaseFuture(controllerFuture!!)
        }
    }

    private fun setupUIWithPlayer(connectPlayer: Player?) {
        if (connectPlayer == null) return

        viewModel.setPlayer(connectPlayer)


        if (connectPlayer.mediaItemCount == 0) {
            val lastMediaId = PlaybackSettings.getLastMediaId(this)
            val songsList = MainActivity.songs

            if (!songsList.isNullOrEmpty()) {
                val songToLoad: MediaItem
                val startPosition: Long

                val savedIndex = songsList.indexOfFirst { it?.mediaId == lastMediaId }

                if (lastMediaId != null && savedIndex != -1) {
                    songToLoad = songsList[savedIndex]!!
                    startPosition = PlaybackSettings.getLastPosition(this)

                    connectPlayer.setMediaItem(songToLoad, startPosition)
                    connectPlayer.prepare() // CRITICAL: Tell the player to load the data

                    updateMiniPlayerUI(songToLoad.mediaMetadata)
                } else {
                    songToLoad = songsList[0]!!
                    startPosition = 0L
                }

                connectPlayer.setMediaItem(songToLoad, startPosition)
                connectPlayer.prepare() // CRITICAL: Tell the player to load the data

                // Immediately update UI with the loaded song's metadata
                updateMiniPlayerUI(songToLoad.mediaMetadata)
            }
        } else {
            // If player was already playing (service was alive), sync UI immediately
            updateMiniPlayerUI(connectPlayer.mediaMetadata)
        }

        // Sync Play/Pause icon state immediately upon opening
        updatePlayPauseIcon(connectPlayer.isPlaying)

        connectPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseIcon(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    progressBar.max = connectPlayer.duration.toInt()
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                updateMiniPlayerUI(mediaMetadata)
            }
        })
    }

    // Add this helper to MainActivity to keep icons in sync
    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.pause_with_circle
            else R.drawable.play_with_circle
        )
    }

    private fun updateMiniPlayerUI(metadata: MediaMetadata) {
        songTitle.text = metadata.title ?: "Unknown Title"
        songArtist.text = metadata.artist ?: "Unknown Artist"

        songTitle.isSelected = true
        songArtist.isSelected = true

        // 1. Small icon artwork
        Glide.with(this@MainActivity)
            .load(metadata.artworkUri)
            .placeholder(R.drawable.default_thumbnail)
            .error(R.drawable.default_thumbnail)
            .into(songArtwork)

        // 2. Background Frosted Artwork (OPTIMIZED)
        // load() ensures blurred default art is shown if artworkUri is null.
        // asBitmap() ensures vectors are rasterized before blurring.
        Glide.with(this@MainActivity)
            .asBitmap()
            .load(metadata.artworkUri ?: R.drawable.default_thumbnail)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 10)))
            .transition(BitmapTransitionOptions.withCrossFade())
            .into(backgroundArtwork)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            controller == null
        }
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.custom_playback)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        progressBar = findViewById(R.id.progressBar)
        songTitle = findViewById(R.id.player_title)
        songArtist = findViewById(R.id.player_artist)
        searchView = findViewById(R.id.search_view)
        songArtwork = findViewById(R.id.art_work)
        backgroundArtwork = findViewById(R.id.background_artwork)

        findViewById<ImageButton>(R.id.btn_previous).setOnClickListener {
            controller?.seekToPreviousMediaItem()
        }
        findViewById<ImageButton>(R.id.btn_next).setOnClickListener {
            controller?.seekToNextMediaItem()
        }

        lifecycleScope.launch {
            viewModel.currentMetadata.collect { metadata ->
                metadata?.let { updateMiniPlayerUI(it) }
            }
        }
        lifecycleScope.launch {
            viewModel.currentPosition.collect { position ->
                progressBar.progress = position.toInt()
            }
        }
        lifecycleScope.launch {
            viewModel.isPlaying.collect { isPlaying ->
                updatePlayPauseIcon(isPlaying)
            }
        }

        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText ?: ""
                val fragments = supportFragmentManager.fragments
                for (fragment in fragments) {
                    when (fragment) {
                        is SongFragment -> fragment.filterSongs(query)
                        is ArtistFragment -> fragment.filterArtist(query)
                        is AlbumFragment -> fragment.filterAlbums(query)
                        is GenreFragment -> fragment.filterGenre(query)
                    }
                }
                return true
            }
        })

        btnPlayPause.setOnClickListener {
            controller?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
        }

        findViewById<View>(R.id.meta_info).setOnClickListener {
            controller?.let {
                val bottomSheet = PlayerBottomSheet(it)
                bottomSheet.show(supportFragmentManager, "PlayerBottomSheet")
            }
        }


        permissionManager = PermissionManager(this) {
            initViewPager()
            controller?.let { setupUIWithPlayer(it) }
        }
        permissionManager.requestRuntimePermission()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.handlePermissionResult(requestCode, grantResults)
    }

    fun openAlbum(position: Int) {
        val selectedAlbum = albums?.get(position)
        val albumId = selectedAlbum?.mediaId?.toLong() ?: return
        val albumSongs = MusicLoader.getSongsByAlbum(this, albumId)

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
        val genreId = selectedGenre?.mediaId?.toLong() ?: return
        val genreSongs = MusicLoader.getSongsByGenre(this, genreId)

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
        val artistId = selectedArtist?.mediaId?.toLong() ?: return
        val artistSongs = MusicLoader.getSongsByArtist(this, artistId)

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
    }

    fun initViewPager() {
        val viewPager = findViewById<ViewPager2>(R.id.viewpager)
        val tabLayout = findViewById<TabLayout>(R.id.initView)
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        viewPagerAdapter.addFragment(SongFragment(), "Songs")
        viewPagerAdapter.addFragment(ArtistFragment(), "Artists")
        viewPagerAdapter.addFragment(AlbumFragment(), "Albums")
        viewPagerAdapter.addFragment(GenreFragment(), "Genres")
        viewPagerAdapter.addFragment(PlaylistFragment(), "Playlists")
        viewPager.adapter = viewPagerAdapter
        TabLayoutMediator(
            tabLayout, viewPager, (TabConfigurationStrategy { tab: TabLayout.Tab?, position: Int ->
                tab!!.text = viewPagerAdapter.getTitle(position)
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
