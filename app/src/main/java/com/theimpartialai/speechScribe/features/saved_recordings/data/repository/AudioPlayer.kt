package com.theimpartialai.speechScribe.features.saved_recordings.data.repository

import com.theimpartialai.speechScribe.features.saved_recordings.domain.model.AudioRecording

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