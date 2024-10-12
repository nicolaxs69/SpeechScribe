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
}