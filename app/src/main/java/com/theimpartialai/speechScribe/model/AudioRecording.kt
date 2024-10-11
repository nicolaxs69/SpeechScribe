package com.theimpartialai.speechScribe.model

import java.util.Locale

data class AudioRecording(
    val fileName: String,
    val duration: Double,
    val filePath: String,
    val fileSize: Double,
    val timeStamp: Long,
) {
    val formattedDuration: String
        get() {
            val minutes = duration.toInt()
            val seconds = ((duration - minutes) * 60).toInt()
            return String.format(Locale.getDefault(), "%02d:%02d min", minutes, seconds)
        }

    val formattedFileSize: String
        get() = String.format(Locale.getDefault(), "%.2f mb", fileSize)
}