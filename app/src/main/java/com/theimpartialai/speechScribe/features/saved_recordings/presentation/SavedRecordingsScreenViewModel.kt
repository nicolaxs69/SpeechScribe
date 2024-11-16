package com.theimpartialai.speechScribe.features.saved_recordings.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.theimpartialai.speechScribe.features.cloud.S3UploadManager
import com.theimpartialai.speechScribe.features.recording.data.repository.AudioRecordingImpl
import com.theimpartialai.speechScribe.features.saved_recordings.data.repository.AudioPlayerImpl
import com.theimpartialai.speechScribe.features.saved_recordings.domain.model.AudioRecording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class SavedRecordingsViewModel(application: Application) : AndroidViewModel(application) {
    private val s3UploadManager = S3UploadManager(application)

    private val audioRepository = AudioRecordingImpl(application)
    private val audioPlayer = AudioPlayerImpl(application)
    private val _recordings = MutableStateFlow<List<AudioRecording>>(emptyList())
    val recordings: StateFlow<List<AudioRecording>> get() = _recordings

    private val _uploadStatus = MutableStateFlow<String?>(null)
    val uploadStatus: StateFlow<String?> = _uploadStatus

    fun loadRecordings() {
        viewModelScope.launch {
            val recordings = audioRepository.loadRecordings()
            _recordings.value = recordings
        }
    }

    fun deleteRecording(recording: AudioRecording) {
        viewModelScope.launch {
            audioRepository.deleteRecording(recording)
            val updatedList = _recordings.value.toMutableList().apply {
                remove(recording)
            }
            _recordings.value = updatedList
        }
    }

    fun togglePlayback(recording: AudioRecording) {
        viewModelScope.launch {
            val currentlyPlaying = _recordings.value.find { it.isPlaying }

            if (currentlyPlaying != null && currentlyPlaying != recording) {
                updateRecordingState(
                    currentlyPlaying.copy(
                        isPlaying = false,
                        isPaused = false,
                        playbackPosition = 0
                    )
                )
                audioPlayer.stopPlayback()
            }

            audioPlayer.togglePlayback(
                recording = recording,
                onPlaybackStarted = {
                    updateRecordingState(
                        recording.copy(
                            isPlaying = true,
                            isPaused = false
                        )
                    )
                },
                onPlaybackPaused = {
                    updateRecordingState(
                        recording.copy(
                            isPlaying = false,
                            isPaused = true
                        )
                    )
                },
                onPlaybackCompleted = {
                    updateRecordingState(
                        recording.copy(
                            isPlaying = false,
                            isPaused = false,
                            playbackPosition = 0
                        )
                    )
                }
            )
        }
    }

    fun resetUploadStatus() {
        _uploadStatus.value = null
    }

    private fun updateRecordingState(updatedRecording: AudioRecording) {
        val updatedList = _recordings.value.map {
            if (it.fileName == updatedRecording.fileName) updatedRecording else it
        }
        _recordings.value = updatedList
    }

    override fun onCleared() {
        super.onCleared()
        audioRepository.release()
    }

    fun uploadRecording(recording: AudioRecording) {
        viewModelScope.launch {
            try {
                val file = File(recording.filePath)
                if (file.exists()) {
                    s3UploadManager.uploadFileToS3(file,
                        onSuccess = {
                            _uploadStatus.value = "File uploaded successfully"
                        },
                        onError = { error ->
                            _uploadStatus.value = "Upload failed: ${error.message}"
                        }
                    )
                } else {
                    _uploadStatus.value = "File does not exist: ${recording.filePath}"
                }
            } catch (e: Exception) {
                _uploadStatus.value = "Error uploading file: ${e.message}"
            }
        }
    }
}