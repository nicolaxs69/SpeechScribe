package com.theimpartialai.speechScribe.repository

interface AudioPlayer {
    fun startPlayback(filePath: String, position: Long = 0, onComplete: () -> Unit)
    fun pausePlayback()
    fun resumePlayback()
    fun stopPlayback()
    fun release()
}