package com.example.songify

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
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

class SongsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvResult: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var allSongs: List<Song> = emptyList()
    private lateinit var adapter: SongAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_songs, container, false)

        // Force fragment background to match your Player theme
        view.setBackgroundColor(android.graphics.Color.parseColor("#0E0E0E"))

        recyclerView = view.findViewById(R.id.rvSongs)
        tvResult = view.findViewById(R.id.tvResultSongs)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        fetchSongs()

        swipeRefreshLayout.setOnRefreshListener {
            fetchSongs()
        }

        return view
    }

    fun filterSongs(query: String) {
        val filteredList = if (query.isEmpty()) {
            allSongs
        } else {
            allSongs.filter { it.title.contains(query, ignoreCase = true) }
        }
        adapter.updateList(filteredList)
    }

    private fun fetchSongs() {
        tvResult.visibility = View.VISIBLE
        tvResult.text = "Fetching your music..."
        swipeRefreshLayout.isRefreshing = true

        val sharedPref = requireContext().getSharedPreferences("SongifyPrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", null)

        if (token == null) {
            tvResult.text = "Session expired. Please login."
            swipeRefreshLayout.isRefreshing = false
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.SONGIFY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)

        // FULL UPDATE: Ensure your API Key and Token are correct
        service.getSongs(BuildConfig.SONGIFY_API_KEY, token).enqueue(object : Callback<SongResponse> {
            override fun onResponse(call: Call<SongResponse>, response: Response<SongResponse>) {
                if (!isAdded) return
                swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful) {
                    val body = response.body()
                    val songList = body?.songs ?: body?.data ?: emptyList()
                    allSongs = songList.sortedBy { it.title }

                    if (allSongs.isNotEmpty()) {
                        adapter = SongAdapter(allSongs, { selectedSong ->
                            val clickedIndex = allSongs.indexOf(selectedSong)
                            // Tells MainActivity to play the specific song from the full list
                            (activity as? MainActivity)?.playSong(allSongs, clickedIndex)
                        }, { songToadd ->
                            PlaylistRepository.addToPlaylist(songToadd)
                            Toast.makeText(requireContext(), "Added to playlist", Toast.LENGTH_SHORT).show()
                        }, { songToRemove ->
                            PlaylistRepository.removeFromPlaylist(songToRemove)
                            Toast.makeText(requireContext(), "Removed from playlist", Toast.LENGTH_SHORT).show()
                        })
                        recyclerView.adapter = adapter
                        tvResult.visibility = View.GONE
                    } else {
                        tvResult.text = "No songs found on server."
                    }
                } else {
                    tvResult.text = "Server Error: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<SongResponse>, t: Throwable) {
                if (isAdded) {
                    tvResult.text = "Network Error. Check internet."
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        })
    }
}