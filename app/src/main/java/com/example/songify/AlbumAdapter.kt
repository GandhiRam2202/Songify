package com.example.songify

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.songify.R
import com.example.songify.network.Song

class AlbumAdapter(
    private var albums: Map<String, List<Song>>,
    private val onClick: (String, List<Song>) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    private var albumList = albums.keys.toList()

    class AlbumViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvAlbumName)
        val image: ImageView = view.findViewById(R.id.ivAlbumArt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val name = albumList[position]
        val songs = albums[name]
        holder.name.text = name

        // Use the first song's image as the album art
        songs?.firstOrNull()?.imageUrl?.let {
            holder.image.load(it) {
                crossfade(true)
                placeholder(R.drawable.logo) // Optional: a placeholder image
                error(R.drawable.logo) // Optional: an error image
            }
        } ?: holder.image.setImageResource(R.drawable.logo)

        holder.itemView.setOnClickListener { onClick(name, songs ?: emptyList()) }
    }

    override fun getItemCount() = albumList.size

    fun updateAlbums(newAlbums: Map<String, List<Song>>) {
        albums = newAlbums
        albumList = newAlbums.keys.toList()
        notifyDataSetChanged()
    }
}