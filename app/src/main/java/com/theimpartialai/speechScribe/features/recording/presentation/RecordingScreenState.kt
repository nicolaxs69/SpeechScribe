package com.theimpartialai.speechScribe.features.recording.presentation

sealed class RecordingState {
    data object Idle : RecordingState()
    data object Recording : RecordingState()
    data object Paused : RecordingState()
}

data class RecordingScreenUiState(
    val timer: Long = 0,
    val recordingState: RecordingState = RecordingState.Idle
)

const val ANIMATION_DURATION = 300
enum class AudioMode { MONO, STEREO }