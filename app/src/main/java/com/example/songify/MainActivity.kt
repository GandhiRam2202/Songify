package com.example.songify

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.songify.network.Song
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null
        private set(value) {
            controllerFuture = value?.let { MoreExecutors.listeningDecorator(MoreExecutors.newDirectExecutorService()).submit<MediaController> { it } }
        }

    internal var onControllerReady: ((MediaController) -> Unit)? = null

    private lateinit var miniPlayer: LinearLayout
    private lateinit var tvMiniTitle: TextView
    private lateinit var tvMiniArtist: TextView
    private lateinit var btnMiniPlayPause: ImageButton
    private lateinit var ivMiniCover: ImageView
    private lateinit var searchBar: EditText
    private lateinit var viewPager: ViewPager2
    private lateinit var drawer: DrawerLayout
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("SongifyPrefs", Context.MODE_PRIVATE)
        if (!prefs.contains("token")) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        window.statusBarColor = Color.parseColor("#0E0E0E")

        PlaylistRepository.init(this)

        initViews()
        setupNavigation()
        setupFragmentListener()
    }

    private fun setupFragmentListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment is PlayerFragment) {
                miniPlayer.visibility = View.GONE
            } else {
                if (controller?.currentMediaItem != null) {
                    miniPlayer.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun initViews() {
        miniPlayer = findViewById(R.id.miniPlayer)
        tvMiniTitle = findViewById(R.id.tvMiniTitle)
        tvMiniArtist = findViewById(R.id.tvMiniArtist)
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause)
        ivMiniCover = findViewById(R.id.ivMiniCover)
        searchBar = findViewById(R.id.searchBar)
        tabLayout = findViewById(R.id.tabLayout)

        btnMiniPlayPause.setOnClickListener { togglePlayPause() }
        miniPlayer.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment is PlayerFragment) {
                supportFragmentManager.popBackStack()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, PlayerFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                val currentFragment = supportFragmentManager.findFragmentByTag("f" + viewPager.currentItem)
                when (currentFragment) {
                    is SongsFragment -> currentFragment.filterSongs(query)
                    is AlbumsFragment -> currentFragment.filterAlbums(query)
                    is ComposerFragment -> currentFragment.filterComposers(query)
                    is PlaylistFragment -> currentFragment.filterPlaylist(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupPlayerListener() {
        val player = controller ?: return

        onControllerReady?.invoke(player)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (player.currentMediaItem != null && currentFragment !is PlayerFragment) {
            miniPlayer.visibility = View.VISIBLE
        }
        updateMiniPlayerUI(player.mediaMetadata)

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                btnMiniPlayPause.setImageResource(
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                )
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (currentFragment !is PlayerFragment) {
                    miniPlayer.visibility = View.VISIBLE
                }
                updateMiniPlayerUI(mediaMetadata)
            }
        })
    }

    private fun updateMiniPlayerUI(metadata: MediaMetadata) {
        tvMiniTitle.text = metadata.title ?: "Unknown"
        tvMiniArtist.text = metadata.artist ?: "Unknown"
        ivMiniCover.load(metadata.artworkUri) {
            placeholder(R.drawable.logo)
            error(R.drawable.logo)
        }
    }

    fun playSong(songList: List<Song>, startIndex: Int) {
        val player = controller ?: return
        val mediaItems = songList.map { song ->
            MediaItem.Builder()
                .setUri(song.mp3Url)
                .setMediaId(song._id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artists.joinToString(", "))
                        .setArtworkUri(android.net.Uri.parse(song.imageUrl))
                        .build()
                ).build()
        }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.play()
    }

    private fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({ setupPlayerListener() }, MoreExecutors.directExecutor())
    }

    private fun setupNavigation() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "SONGIFY"

        drawer = findViewById<DrawerLayout>(R.id.drawerLayout)
        val toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.open, R.string.close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navView = findViewById<NavigationView>(R.id.navView)
        navView.setNavigationItemSelectedListener(this)

        val width = (resources.displayMetrics.widthPixels * 0.60).toInt()
        val layoutParams = navView.layoutParams
        layoutParams.width = width
        navView.layoutParams = layoutParams

        viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = ViewPagerAdapter(this)

        tabLayout.setBackgroundColor(Color.parseColor("#0F0F0F"))
        tabLayout.setTabTextColors(Color.GRAY, Color.WHITE)
        tabLayout.setSelectedTabIndicatorColor(Color.BLUE)

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = when(pos) {
                0 -> "Songs"
                1 -> "Albums"
                2 -> "Composers"
                3 -> "Playlist"
                else -> null
            }
        }.attach()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_songs -> viewPager.currentItem = 0
            R.id.nav_albums -> viewPager.currentItem = 1
            R.id.nav_composers -> viewPager.currentItem = 2
            R.id.nav_playlist -> viewPager.currentItem = 3
            R.id.nav_feedback -> {
                startActivity(Intent(this, FeedbackActivity::class.java))
            }
            R.id.nav_logout -> {
                controller?.stop()
                miniPlayer.visibility = View.GONE
                val prefs = getSharedPreferences("SongifyPrefs", Context.MODE_PRIVATE)
                prefs.edit {
                    remove("token")
                }
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            else -> return false
        }
        drawer.closeDrawers()
        return true
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
