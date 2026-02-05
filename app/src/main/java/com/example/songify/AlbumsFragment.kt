package com.example.songify

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.songify.network.ApiService
import com.example.songify.network.Song
import com.example.songify.network.SongResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AlbumsFragment : Fragment() {

    private lateinit var rvAlbums: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var adapter: AlbumAdapter
    private var allAlbums: Map<String, List<Song>> = emptyMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_albums, container, false)
        rvAlbums = view.findViewById(R.id.rvAlbums)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayoutAlbums)
        rvAlbums.layoutManager = GridLayoutManager(requireContext(), 3)

        fetchAndGroupAlbums()

        swipeRefreshLayout.setOnRefreshListener {
            fetchAndGroupAlbums()
        }

        return view
    }

    private fun fetchAndGroupAlbums() {
        swipeRefreshLayout.isRefreshing = true
        val prefs = requireContext().getSharedPreferences("SongifyPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null) ?: return

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.SONGIFY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)
        service.getSongs(BuildConfig.SONGIFY_API_KEY, token).enqueue(object : Callback<SongResponse> {
            override fun onResponse(call: Call<SongResponse>, response: Response<SongResponse>) {
                if (!isAdded) return
                swipeRefreshLayout.isRefreshing = false
                if (response.isSuccessful) {
                    val allSongs = response.body()?.songs ?: response.body()?.data ?: emptyList()
                    allAlbums = allSongs.groupBy { it.album ?: "Single / Unknown" }.toSortedMap()
                    adapter = AlbumAdapter(allAlbums) { albumName, songs ->
                        showAlbumSongsDetail(albumName, songs)
                    }
                    rvAlbums.adapter = adapter
                }
            }

            override fun onFailure(call: Call<SongResponse>, t: Throwable) {
                if (!isAdded) return
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    fun filterAlbums(query: String) {
        val filteredAlbums = if (query.isEmpty()) {
            allAlbums
        } else {
            allAlbums.filter { it.key.contains(query, ignoreCase = true) }
        }
        adapter.updateAlbums(filteredAlbums)
    }

    private fun showAlbumSongsDetail(albumName: String, songs: List<Song>) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        dialog.setCanceledOnTouchOutside(true)

        val view = layoutInflater.inflate(R.layout.fragment_songs, null)
        view.setBackgroundColor(Color.parseColor("#0E0E0E"))

        val scale = resources.displayMetrics.density
        val topPaddingInPx = (110 * scale + 0.5f).toInt()
        view.setPadding(0, topPaddingInPx, 0, 0)

        val rv = view.findViewById<RecyclerView>(R.id.rvSongs)
        val title = view.findViewById<TextView>(R.id.tvResultSongs)

        title.visibility = View.VISIBLE
        title.text = albumName
        title.setTextColor(Color.parseColor("#3DDCFF"))

        rv.layoutManager = LinearLayoutManager(context)
        val sortedSongs = songs.sortedBy { it.title }
        rv.adapter = SongAdapter(sortedSongs, { selectedSong ->
            val index = sortedSongs.indexOf(selectedSong)
            (activity as? MainActivity)?.playSong(sortedSongs, index)
            dialog.dismiss()
        }, { songToAdd ->
            PlaylistRepository.addToPlaylist(songToAdd)
            Toast.makeText(requireContext(), "Added to playlist", Toast.LENGTH_SHORT).show()
        }, { songToRemove ->
            PlaylistRepository.removeFromPlaylist(songToRemove)
            Toast.makeText(requireContext(), "Removed from playlist", Toast.LENGTH_SHORT).show()
        })

        dialog.setContentView(view)

        dialog.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.60).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setGravity(Gravity.START)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.attributes.windowAnimations = android.R.style.Animation_Translucent
        }
        dialog.show()
    }
}