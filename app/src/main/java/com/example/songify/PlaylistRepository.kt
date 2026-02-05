package com.example.songify

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.songify.network.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PlaylistRepository {

    private const val PREFS_NAME = "SongifyPlaylist"
    private const val PLAYLIST_KEY = "playlist"

    private lateinit var prefs: android.content.SharedPreferences
    private val gson = Gson()


    private val _playlist = MutableLiveData<MutableList<Song>>(mutableListOf())
    val playlist: LiveData<MutableList<Song>> = _playlist

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadPlaylist()
    }

    private fun loadPlaylist() {
        val jsonPlaylist = prefs.getString(PLAYLIST_KEY, null)
        if (jsonPlaylist != null) {
            val type = object : TypeToken<List<Song>>() {}.type
            val savedPlaylist: List<Song> = gson.fromJson(jsonPlaylist, type)
            _playlist.value = savedPlaylist.toMutableList()
        }
    }

    private fun savePlaylist() {
        val jsonPlaylist = gson.toJson(_playlist.value)
        prefs.edit().putString(PLAYLIST_KEY, jsonPlaylist).commit()
    }


    fun addToPlaylist(song: Song) {
        val currentPlaylist = _playlist.value ?: mutableListOf()
        if (!currentPlaylist.contains(song)) {
            val updatedPlaylist = currentPlaylist.toMutableList()
            updatedPlaylist.add(song)
            _playlist.value = updatedPlaylist
            savePlaylist()
        }
    }

    fun removeFromPlaylist(song: Song) {
        val currentPlaylist = _playlist.value ?: mutableListOf()
        if (currentPlaylist.contains(song)) {
            val updatedPlaylist = currentPlaylist.toMutableList()
            updatedPlaylist.remove(song)
            _playlist.value = updatedPlaylist
            savePlaylist()
        }
    }

    fun getPlaylist(): List<Song> {
        return _playlist.value ?: emptyList()
    }
}
