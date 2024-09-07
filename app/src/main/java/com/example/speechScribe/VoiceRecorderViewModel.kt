package com.example.speechScribe

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class VoiceRecorderViewModel : ViewModel() {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private lateinit var fileName: String

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun initializeFileName(context: Context) {
        fileName = "${context.filesDir.absolutePath}/audioRecordTest.3gp"
    }

    fun startRecording(context: Context) {
        recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            // Set high quality audio sampling rate and bit rate
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)

            setOutputFile(fileName)


            try {
                prepare()
            } catch (e: Exception) {
                Log.e("VoiceRecorderViewModel", "prepare() failed")
            }

            start()
        }
        _isRecording.value = true
    }

    fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        _isRecording.value = false
    }

    fun startPlaying() {
        player = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
                _isPlaying.value = true
            } catch (e: IOException) {
                Log.e("AudioRecordViewModel", "prepared() failed")
            }
        }
    }

    fun stopPlaying() {
        player?.release()
        player = null
        _isPlaying.value = false
    }

    override fun onCleared() {
        super.onCleared()
        recorder?.release()
        recorder = null
        player?.release()
        player = null
    }
}
