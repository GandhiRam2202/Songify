package com.example.songify.network

import com.google.gson.annotations.SerializedName

data class SongResponse(
    // Check your API response: Is it "songs", "data", or "allSongs"?
    @SerializedName("songs")
    val songs: List<Song>? = null,

    @SerializedName("data")
    val data: List<Song>? = null
) {
    // This helper function gets whichever one is not null
    fun getList(): List<Song> = songs ?: data ?: emptyList()
}