package com.theimpartialai.speechScribe.ui.recording

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theimpartialai.speechScribe.repository.AudioRecordingImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt

class RecordingScreenViewModel : ViewModel() {
    companion object {
        private const val TAG = "VoiceRecorderViewModel"
        private const val AMPLITUDE_THRESHOLD = 40
        private const val BASELINE_AMPLITUDE = 10
        private const val SMOOTHING_WINDOW_SIZE = 5
        private const val MAX_AMPLITUDES = 300

        // Audio configuration
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE = 2048
    }

    private var recordingStartTime: Long = 0L
    private var recordingEndTime: Long = 0L
    private var audioRecord: AudioRecord? = null
    private var currentOutputFile: File? = null
    private var fileOutputStream: FileOutputStream? = null
    private var recordingJob: Job? = null
    private var timerJob: Job? = null

    private val _uiState = MutableStateFlow(VoiceRecorderUiState())
    val uiState: StateFlow<VoiceRecorderUiState> = _uiState.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Int>>(emptyList())
    val amplitudes: StateFlow<List<Int>> = _amplitudes.asStateFlow()

    private val amplitudeBuffer = mutableListOf<Int>()

    private val audioRepository = AudioRecordingImpl()

    /**
    +      * Starts the recording process by initializing the necessary components and updating the UI state.
    +      *
    +      * @param context The context used to initialize audio controllers and file operations.
    +      */

    fun startRecording(context: Context) {
        recordingStartTime = System.currentTimeMillis()
        viewModelScope.launch {
            audioRepository.startRecording(context)
            _uiState.update { it.copy(recordingState = RecordingState.Recording) }
            startTimer()
        }
    }

    private fun startRecordingJob() {
        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(BUFFER_SIZE)
            while (isActive && _uiState.value.recordingState is RecordingState.Recording) {
                val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: -1
                if (read > 0) {
                    processAudioData(buffer, read)
                }
            }
        }
    }

    private fun processAudioData(buffer: ByteArray, bytesRead: Int) {
        // Write audio data to file
        fileOutputStream?.write(buffer, 0, bytesRead)

        // Calculate and update amplitude
        val amplitude = calculateAmplitude(buffer)
        updateAmplitudes(amplitude)
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
            audioRepository.resumeRecording()
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


    private fun calculateAmplitude(buffer: ByteArray): Int {
        var sum = 0.0
        // Process 16-bit samples
        for (i in buffer.indices step 2) {
            val sample = (buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)
            sum += sample * sample
        }
        val rms = sqrt(sum / (buffer.size / 2))
        return rms.toInt()
    }

    private fun updateAmplitudes(amplitude: Int) {
        val processedAmplitude =
            if (amplitude > AMPLITUDE_THRESHOLD) amplitude else BASELINE_AMPLITUDE
        amplitudeBuffer.add(processedAmplitude)

        if (amplitudeBuffer.size >= SMOOTHING_WINDOW_SIZE) {
            val smoothedAmplitude = amplitudeBuffer.average().toInt()
            amplitudeBuffer.removeAt(0)
            _amplitudes.update { (it + smoothedAmplitude).takeLast(MAX_AMPLITUDES) }
        } else {
            _amplitudes.update { (it + BASELINE_AMPLITUDE).takeLast(MAX_AMPLITUDES) }
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

