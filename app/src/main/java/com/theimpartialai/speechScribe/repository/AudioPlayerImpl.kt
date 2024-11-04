package com.theimpartialai.speechScribe.repository

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.theimpartialai.speechScribe.model.AudioRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AudioPlayerImpl(private val context: Context) : AudioPlayer {
    private var exoPlayer: ExoPlayer? = null

    override fun startPlayback(
        filePath: String,
        position: Long,
        onComplete: () -> Unit
    ) {
        stopPlayback()

        exoPlayer = ExoPlayer.Builder(context).build()

        val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))

        exoPlayer?.apply {
            setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            setMediaItem(mediaItem)
            prepare()
            seekTo(position)
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    Log.d("AudioPlayerImpl", "Playback state changed to: $playbackState")
                    Log.d("AudioPlayerImpl", "Player duration: $duration")
                    if (playbackState == Player.STATE_ENDED) {
                        onComplete()
                        release()
                        exoPlayer = null
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("AudioPlayerImpl", "Error playing audio", error)
                    release()
                    exoPlayer = null
                }
            }
            )
        }
    }

    override suspend fun togglePlayback(
        recording: AudioRecording,
        onPlaybackStarted: () -> Unit,
        onPlaybackPaused: () -> Unit,
        onPlaybackCompleted: () -> Unit
    ) {
        withContext(Dispatchers.Main) {
            try {
                when {
                    recording.isPlaying -> {
                        pausePlayback()
                        onPlaybackPaused()
                    }

                    recording.isPaused -> {
                        resumePlayback()
                        onPlaybackStarted()
                    }

                    else -> {
                        startPlayback(
                            filePath = recording.filePath,
                            position = 0L,
                            onComplete = onPlaybackCompleted
                        )
                        onPlaybackStarted()
                    }
                }

            } catch (e: Exception) {
                Log.e("AudioRecordingImpl", "Error toggling playback", e)
                onPlaybackCompleted()
            }
        }
    }

    override fun pausePlayback() {
        exoPlayer?.pause()
    }

    override fun resumePlayback() {
        exoPlayer?.play()
    }

    override fun stopPlayback() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun release() {
        stopPlayback()
    }
}