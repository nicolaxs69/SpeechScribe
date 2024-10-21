package com.theimpartialai.speechScribe.model

import com.theimpartialai.speechScribe.ui.savedRecording.PlayBackState
import kotlinx.coroutines.flow.MutableStateFlow

data class RecordingItemState(
    val recording: AudioRecording,
    val playbackState: MutableStateFlow<PlayBackState> = MutableStateFlow(PlayBackState.Pause)
)