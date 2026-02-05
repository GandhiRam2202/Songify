package com.example.songify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.songify.network.Song

class PlaylistFragment : Fragment() {

    private lateinit var playlistAdapter: PlaylistAdapter
    private var fullPlaylist: List<Song> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.playlistRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        playlistAdapter = PlaylistAdapter(mutableListOf(), { song ->
            val playlist = PlaylistRepository.getPlaylist()
            val index = playlist.indexOf(song)
            if (index != -1) {
                (activity as? MainActivity)?.playSong(playlist, index)
            }
        }, { songToRemove ->
            PlaylistRepository.removeFromPlaylist(songToRemove)
            Toast.makeText(requireContext(), "Removed from playlist", Toast.LENGTH_SHORT).show()
        })
        recyclerView.adapter = playlistAdapter

        PlaylistRepository.playlist.observe(viewLifecycleOwner, Observer {
            fullPlaylist = it
            playlistAdapter.updatePlaylist(it)
        })
    }

    fun filterPlaylist(query: String) {
        val filteredPlaylist = if (query.isEmpty()) {
            fullPlaylist
        } else {
            fullPlaylist.filter { it.title.contains(query, ignoreCase = true) }
        }
        playlistAdapter.updatePlaylist(filteredPlaylist)
    }
}