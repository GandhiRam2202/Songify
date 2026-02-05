package com.example.songify

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 4 // We have 4 tabs

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SongsFragment()
            1 -> AlbumsFragment()
            2 -> ComposerFragment()
            3 -> PlaylistFragment()
            else -> SongsFragment()
        }
    }
}