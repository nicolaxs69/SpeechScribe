package com.theimpartialai.speechScribe.features.saved_recordings.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.theimpartialai.speechScribe.features.cloud.S3UploadManager
import com.theimpartialai.speechScribe.features.recording.data.repository.AudioRecordingImpl
import com.theimpartialai.speechScribe.features.saved_recordings.data.repository.AudioPlayerImpl
import com.theimpartialai.speechScribe.features.saved_recordings.domain.model.AudioRecording
import com.theimpartialai.speechScribe.features.saved_recordings.domain.model.UploadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SavedRecordingsScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val s3UploadManager = S3UploadManager(application)

    private val audioRepository = AudioRecordingImpl(application)
    private val audioPlayer = AudioPlayerImpl(application)
    private val _recordings = MutableStateFlow<List<AudioRecording>>(emptyList())
    val recordings: StateFlow<List<AudioRecording>> get() = _recordings

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState = _uploadState.asStateFlow()

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
        _uploadState.value = UploadState.Idle
    }

    private fun updateRecordingState(updatedRecording: AudioRecording) {
        val updatedList = _recordings.value.map { originalRecording ->
            if (originalRecording.fileName == updatedRecording.fileName) updatedRecording else originalRecording
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
                _uploadState.value = UploadState.Loading
                updateRecordingState(recording.copy(isUploading = true))

                withContext(Dispatchers.IO) {
                    val file = File(recording.filePath)
                    if (!file.exists()) {
                        throw Exception("File does not exist: ${recording.filePath}")
                    }

                    s3UploadManager.uploadFileToS3(
                        file,
                        onSuccess = {
                            viewModelScope.launch {
                                updateRecordingState(
                                    recording.copy(
                                        isUploading = false,
                                    )
                                )
                                _uploadState.value =
                                    UploadState.Success("File uploaded successfully")
                            }
                        },
                        onError = { error ->
                            viewModelScope.launch {
                                updateRecordingState(recording.copy(isUploading = false))
                                _uploadState.value =
                                    UploadState.Error(error.message ?: "Unknown error occurred")
                            }
                        },
                    )
                }
            } catch (e: Exception) {
                updateRecordingState(recording.copy(isUploading = false))
                _uploadState.value = UploadState.Error("Error uploading file: ${e.message}")
            }
        }
    }
}