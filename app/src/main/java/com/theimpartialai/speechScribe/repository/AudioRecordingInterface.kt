package com.theimpartialai.speechScribe.repository

import android.content.Context
import com.theimpartialai.speechScribe.model.AudioRecording
import com.theimpartialai.speechScribe.ui.savedRecording.PlayBackState

interface AudioRecordingInterface {
    suspend fun loadRecordings(context: Context): List<AudioRecording>
    suspend fun deleteRecording(recording: AudioRecording)
    suspend fun togglePlayback(
        context: Context,
        recording: AudioRecording,
        playBackState: PlayBackState,
        onPlayBackComplete: (PlayBackState) -> Unit,
    )
}