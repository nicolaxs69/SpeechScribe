package com.theimpartialai.speechScribe.repository

import com.theimpartialai.speechScribe.model.AudioRecording

interface AudioPlayer {
    fun startPlayback(
        filePath: String,
        position: Long = 0,
        onComplete: () -> Unit
    )

    suspend fun togglePlayback(
        recording: AudioRecording,
        onPlaybackStarted: () -> Unit,
        onPlaybackPaused: () -> Unit,
        onPlaybackCompleted: () -> Unit
    )

    fun pausePlayback()
    fun resumePlayback()
    fun stopPlayback()
    fun release()
}