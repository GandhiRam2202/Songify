package com.example.songify

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView

class PlayerFragment : Fragment() {

    private lateinit var playerView: PlayerView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as? MainActivity)?.onControllerReady = {
            if (isAdded) {
                setController(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player, container, false)
        playerView = view.findViewById(R.id.player_view)
        return view
    }

    override fun onStart() {
        super.onStart()
        (activity as? MainActivity)?.controller?.let {
            setController(it)
        }
    }

    private fun setController(controller: MediaController) {
        playerView.player = controller
    }

    override fun onStop() {
        super.onStop()
        playerView.player = null
    }

    override fun onDetach() {
        super.onDetach()
        (activity as? MainActivity)?.onControllerReady = null
    }
}