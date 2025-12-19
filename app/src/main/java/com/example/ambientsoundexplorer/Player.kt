package com.example.ambientsoundexplorer

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.session.MediaSession

object Player {
    lateinit var mediaSession: MediaSession
    val player: MediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        setOnPreparedListener {
            it.start()
        }
    }

    fun init(context: Context) {
        mediaSession = MediaSession(context, "MusicService")
    }
}