package com.theimpartialai.speechScribe.ui.recording

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.theimpartialai.speechScribe.repository.AmplitudeListener
import com.theimpartialai.speechScribe.repository.AudioRecordingImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections

class RecordingScreenViewModel(application: Application) : AndroidViewModel(application),
    AmplitudeListener {
    companion object {
        private const val TAG = "VoiceRecorderViewModel"
        private const val MAX_RECORDER_AMPLITUDE = 32767
        private const val DISPLAY_AMPLITUDE_MAX = 100 // Desired max for visualization
        private const val SMOOTHING_WINDOW_SIZE = 5
        private const val MAX_AMPLITUDES = 300
        private const val AMPLITUDE_UPDATE_INTERVAL = 50L // 50ms
    }

    private var recordingStartTime: Long = 0L
    private var recordingEndTime: Long = 0L
    private var timerJob: Job? = null

    private val _uiState = MutableStateFlow(VoiceRecorderUiState())
    val uiState: StateFlow<VoiceRecorderUiState> = _uiState.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Int>>(emptyList())
    val amplitudes: StateFlow<List<Int>> = _amplitudes.asStateFlow()

    private val amplitudeBuffer = Collections.synchronizedList(mutableListOf<Int>())

    private val audioRepository = AudioRecordingImpl(application)

    /**
    +      * Starts the recording process by initializing the necessary components and updating the UI state.
    +      *
    +      * @param context The context used to initialize audio controllers and file operations.
    +      */

    fun startRecording() {
        recordingStartTime = System.currentTimeMillis()
        viewModelScope.launch {
            audioRepository.startRecording(this@RecordingScreenViewModel)
            _uiState.update { it.copy(recordingState = RecordingState.Recording) }
            startTimer()
        }
    }


    fun stopRecording() {
        recordingEndTime = System.currentTimeMillis()
        timerJob?.cancel()
        timerJob = null

        viewModelScope.launch {
            _uiState.update { it.copy(timer = 0, recordingState = RecordingState.Idle) }
            _amplitudes.update { emptyList() }
        }
    }

    fun pauseRecording() {
        viewModelScope.launch {
            audioRepository.pauseRecording()
            _uiState.update { it.copy(recordingState = RecordingState.Paused) }
            timerJob?.cancel()
        }
    }

    fun resumeRecording() {
        val pauseDuration = System.currentTimeMillis() - recordingEndTime
        recordingStartTime += pauseDuration

        viewModelScope.launch {
            audioRepository.resumeRecording(this@RecordingScreenViewModel)
            _uiState.update { it.copy(recordingState = RecordingState.Recording) }
            startTimer()
        }
    }

    fun discardRecording() {
        timerJob?.cancel()
        timerJob = null
        recordingStartTime = 0L
        recordingEndTime = 0L

        viewModelScope.launch {
            audioRepository.discardRecording()
            _uiState.update { it.copy(timer = 0, recordingState = RecordingState.Idle) }
            _amplitudes.update { emptyList() }
        }
    }


    private fun updateAmplitudes(amplitude: Int) {
        val scaledAmplitude =
            (amplitude.toFloat() / MAX_RECORDER_AMPLITUDE * DISPLAY_AMPLITUDE_MAX).toInt()
                .coerceIn(0, DISPLAY_AMPLITUDE_MAX)

        synchronized(amplitudeBuffer) {
            amplitudeBuffer.add(scaledAmplitude)
            if (amplitudeBuffer.size >= SMOOTHING_WINDOW_SIZE) {
                val smoothedAmplitude = amplitudeBuffer.average().toInt()
                amplitudeBuffer.removeAt(0)
                _amplitudes.update { (it + smoothedAmplitude).takeLast(MAX_AMPLITUDES) }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { it.copy(timer = it.timer + 1) }
            }
        }
    }

    override fun onAmplitude(amplitude: Int) {
        updateAmplitudes(amplitude)
    }
}

sealed class RecordingState {
    data object Idle : RecordingState()
    data object Recording : RecordingState()
    data object Paused : RecordingState()
}

data class VoiceRecorderUiState(
    val timer: Long = 0,
    val recordingState: RecordingState = RecordingState.Idle
)

const val ANIMATION_DURATION = 300

enum class AudioMode { MONO, STEREO }

