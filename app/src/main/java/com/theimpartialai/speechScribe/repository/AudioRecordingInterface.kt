package com.theimpartialai.speechScribe.repository

import android.content.Context
import com.theimpartialai.speechScribe.model.AudioRecording

interface AudioRecordingInterface {
    suspend fun loadRecordings(context: Context): List<AudioRecording>
    suspend fun deleteRecording(recording: AudioRecording)
    suspend fun togglePlayback(
        recording: AudioRecording,
        onPlaybackStarted: () -> Unit,
        onPlaybackPaused: () -> Unit,
        onPlaybackCompleted: () -> Unit
    )

    fun stopPlayback()
    fun release()
}