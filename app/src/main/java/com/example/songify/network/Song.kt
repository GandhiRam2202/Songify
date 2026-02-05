package com.example.songify.network

data class Song(
    val _id: String,           // Matches JSON "_id"
    val title: String,         // Matches JSON "title"
    val artists: List<String>, // MUST be List because JSON is ["Name"]
    val album: String?,
    val composers: List<String>?,
    val composerImageUrl: String?,
    val mp3Url: String?,
    val imageUrl: String?      // Added this from your sample
)