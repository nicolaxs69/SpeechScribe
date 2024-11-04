package com.theimpartialai.speechScribe.repository

import com.theimpartialai.speechScribe.model.AudioRecording

interface AudioRecordingInterface {
    suspend fun startRecording(amplitudeListener: AmplitudeListener)
    suspend fun pauseRecording()
    suspend fun resumeRecording(amplitudeListener: AmplitudeListener)
    suspend fun stopRecording()
    suspend fun discardRecording()
    suspend fun loadRecordings(): List<AudioRecording>
    suspend fun deleteRecording(recording: AudioRecording)
    fun stopPlayback()
    fun release()
}

interface AmplitudeListener {
    fun onAmplitude(amplitude: Int)
}