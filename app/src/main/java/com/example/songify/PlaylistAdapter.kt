package com.example.songify

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.songify.network.Song

class PlaylistAdapter(
    private var songs: MutableList<Song>,
    private val onSongClick: (Song) -> Unit,
    private val onRemoveFromPlaylistClick: (Song) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    class PlaylistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvSongTitle)
        val artist: TextView = view.findViewById(R.id.tvSongArtist)
        val moreOptionsButton: ImageButton = view.findViewById(R.id.ivMoreOptions)
        val songImage: ImageView = view.findViewById(R.id.ivSongImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_song, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = song.title
        holder.artist.text = song.artists.joinToString(", ")
        holder.itemView.setOnClickListener { onSongClick(song) }
        holder.moreOptionsButton.setOnClickListener { showPopupMenu(holder.moreOptionsButton, song) }
        holder.songImage.load(song.imageUrl) {
            placeholder(R.drawable.ic_song_placeholder)
            error(R.drawable.ic_song_placeholder)
        }
    }

    private fun showPopupMenu(view: View, song: Song) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.song_options_menu, popup.menu)
        popup.menu.findItem(R.id.action_add_to_playlist).isVisible = false
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_remove_from_playlist -> {
                    onRemoveFromPlaylistClick(song)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun getItemCount() = songs.size

    fun updatePlaylist(newSongs: List<Song>) {
        songs.clear()
        songs.addAll(newSongs)
        notifyDataSetChanged()
    }
}