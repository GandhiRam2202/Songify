package com.example.songify

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.songify.network.Song

class ComposerAdapter(
    private var composers: Map<String, List<Song>>,
    private val onComposerClick: (String, List<Song>) -> Unit
) : RecyclerView.Adapter<ComposerAdapter.ComposerViewHolder>() {

    private var sortedComposers = composers.keys.sorted()

    inner class ComposerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivComposerImage: ImageView = itemView.findViewById(R.id.ivComposerImage)
        private val tvComposerName: TextView = itemView.findViewById(R.id.tvComposerName)

        fun bind(composerName: String, songs: List<Song>) {
            tvComposerName.text = composerName
            // Find a song with a composer image URL, or use a placeholder
            val imageUrl = songs.firstNotNullOfOrNull { it.composerImageUrl } ?: R.drawable.ic_song_placeholder
            ivComposerImage.load(imageUrl) {
                placeholder(R.drawable.ic_song_placeholder)
                error(R.drawable.ic_song_placeholder)
            }
            itemView.setOnClickListener { onComposerClick(composerName, songs) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_composer, parent, false)
        return ComposerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComposerViewHolder, position: Int) {
        val composerName = sortedComposers[position]
        val songs = composers[composerName] ?: emptyList()
        holder.bind(composerName, songs)
    }

    override fun getItemCount(): Int = sortedComposers.size

    fun updateComposers(newComposers: Map<String, List<Song>>) {
        composers = newComposers
        sortedComposers = newComposers.keys.sorted()
        notifyDataSetChanged()
    }
}