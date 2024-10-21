package com.theimpartialai.speechScribe.ui.savedRecording

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theimpartialai.speechScribe.model.AudioRecording
import com.theimpartialai.speechScribe.repository.AudioRecordingImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SavedRecordingsViewModel : ViewModel() {
    private val audioRepository = AudioRecordingImpl()

    private val _recordings = MutableStateFlow<List<AudioRecording>>(emptyList())
    val recordings: StateFlow<List<AudioRecording>> get() = _recordings

    private val _playbackState = MutableStateFlow<PlayBackState>(PlayBackState.Pause)
    val playBackState: StateFlow<PlayBackState> get() = _playbackState

    private val _currentlyPlayingItem = MutableStateFlow<AudioRecording?>(null)
    val currentlyPlayingItem: StateFlow<AudioRecording?> = _currentlyPlayingItem

    fun loadRecordings(context: Context) {
        viewModelScope.launch {
            val recordings = audioRepository.loadRecordings(context)
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

//    fun playRecording(context: Context, recording: AudioRecording, isPaused: PlayBackState) {
//        viewModelScope.launch {
//            audioRepository.togglePlayback(context, recording, isPaused) { newPlayBackState ->
//                _playbackState.value = newPlayBackState
//            }
//        }
//    }

    fun playRecording3(context: Context, recording: AudioRecording, isPaused: PlayBackState) {
        viewModelScope.launch {
            audioRepository.togglePlayback(context, recording, isPaused) { newPlayBackState ->
                _playbackState.value = newPlayBackState
                if (newPlayBackState == PlayBackState.Play) {
                    _currentlyPlayingItem.value = recording
                } else if (newPlayBackState == PlayBackState.Pause) {
                    _currentlyPlayingItem.value = null
                }
            }
        }
    }

    fun playRecording(context: Context, recording: AudioRecording, playBackState: PlayBackState) {
        viewModelScope.launch {
            audioRepository.togglePlayback(context, recording, playBackState) { newPlayBackState ->
                _playbackState.value = newPlayBackState
                if (newPlayBackState == PlayBackState.Play) {
                    _currentlyPlayingItem.value = recording
                } else {
                    _currentlyPlayingItem.value = null
                }
            }
        }
    }

    fun onPlaybackComplete() {
        _playbackState.value = PlayBackState.Pause
        _currentlyPlayingItem.value = null
    }
}

sealed class PlayBackState {
    data object Play : PlayBackState()
    data object Pause : PlayBackState()
}