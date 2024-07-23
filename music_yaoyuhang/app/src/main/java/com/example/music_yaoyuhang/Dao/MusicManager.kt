package com.example.music_yaoyuhang

import com.example.music_yaoyuhang.Dao.MusicInfo

object MusicManager {
    val playlist = mutableListOf<MusicInfo>()

    fun addToPlaylist(musicInfo: MusicInfo): Boolean {
        return if (playlist.contains(musicInfo)) {
            playlist.remove(musicInfo)
            false
        } else {
            playlist.add(musicInfo)
            true
        }
    }
}
