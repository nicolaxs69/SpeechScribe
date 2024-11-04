package com.theimpartialai.speechScribe.ui.savedRecording

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.theimpartialai.speechScribe.model.AudioRecording
import com.theimpartialai.speechScribe.repository.AudioPlayerImpl
import com.theimpartialai.speechScribe.repository.AudioRecordingImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SavedRecordingsViewModel(application: Application) : AndroidViewModel(application) {
    private val audioRepository = AudioRecordingImpl(application)
    private val audioPlayer = AudioPlayerImpl(application)
    private val _recordings = MutableStateFlow<List<AudioRecording>>(emptyList())
    val recordings: StateFlow<List<AudioRecording>> get() = _recordings

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
            // Stop any other playing recordings
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
}