package com.theimpartialai.speechScribe.features.saved_recordings.domain.model

import java.util.Locale

data class AudioRecording(
    val fileName: String,
    val duration: Long,
    val filePath: String,
    val fileSize: Double,
    val timeStamp: Long,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val playbackPosition: Long = 0
) {
    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d min", minutes, seconds)
        }

    val formattedFileSize: String
        get() = String.format(Locale.getDefault(), "%.2f MB", fileSize / (1024 * 1024))
}