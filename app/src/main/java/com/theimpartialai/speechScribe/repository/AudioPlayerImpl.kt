package com.theimpartialai.speechScribe.repository

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

class AudioPlayerImpl : AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var currentFilePath: String? = null

    override fun startPlayback(filePath: String, position: Long, onComplete: () -> Unit) {
        stopPlayback()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(filePath)
            setOnCompletionListener {
                onComplete()
                release()
                mediaPlayer = null
            }
            prepare()
            seekTo(position.toInt())
            start()
        }
        currentFilePath = filePath
    }

    override fun pausePlayback() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerImpl", "Error pausing playback", e)
        }
    }

    override fun resumePlayback() {
        try {
            mediaPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerImpl", "Error resuming playback", e)
        }
    }

    override fun stopPlayback() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            mediaPlayer = null
            currentFilePath = null
        } catch (e: Exception) {
            Log.e("AudioPlayerImpl", "Error stopping playback", e)
        }
    }

    override fun release() {
        stopPlayback()
    }

    fun getCurrentPosition(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: Exception) {
            Log.e("AudioPlayerImpl", "Error getting current position", e)
            0L
        }
    }
}