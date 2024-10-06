package com.theimpartialai.speechScribe.ui.recording

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.opus.Constants.Application
import com.example.opus.Constants.Channels
import com.example.opus.Constants.FrameSize
import com.example.opus.Constants.SampleRate
import com.example.opus.Opus
import com.theimpartialai.speechScribe.opusEncoding.utils.ControllerAudio
import com.theimpartialai.speechScribe.opusEncoding.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.gagravarr.opus.OpusAudioData
import org.gagravarr.opus.OpusFile
import org.gagravarr.opus.OpusInfo
import org.gagravarr.opus.OpusTags
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.sqrt

class RecordingScreenViewModel : ViewModel() {
    companion object {
        private const val TAG = "VoiceRecorderViewModel"
        private const val AMPLITUDE_THRESHOLD = 40
        private const val BASELINE_AMPLITUDE = 10
        private const val SMOOTHING_WINDOW_SIZE = 5
        private const val BYTES_PER_SAMPLE = 2 // For 16-bit audio
        private const val MAX_AMPLITUDES = 300
    }

    private val _uiState = MutableStateFlow(VoiceRecorderUiState())
    val uiState: StateFlow<VoiceRecorderUiState> = _uiState.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Int>>(emptyList())
    val amplitudes: StateFlow<List<Int>> = _amplitudes.asStateFlow()

    private val codec = Opus()
    private val application = Application.audio()
    private var currentOutputFile: File? = null
    private var fileOutputStream: FileOutputStream? = null

    private val amplitudeBuffer = mutableListOf<Int>()

    private var timerJob: Job? = null

    /**
    +      * Starts the recording process by initializing the necessary components and updating the UI state.
    +      *
    +      * @param context The context used to initialize audio controllers and file operations.
    +      */

    fun startRecording(context: Context) {
        _uiState.update { it.copy(recordingState = RecordingState.Recording) }
        startTimer()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _amplitudes.update { emptyList() } // Clear the list of amplitudes
                initializeRecording(context)

                while (_uiState.value.recordingState is RecordingState.Recording) {
                    processAudioFrame()
                }

            } catch (e: Exception) {
                Log.e(TAG, "IO Error starting recording: ${e.message}", e)
            }
        }
    }

    private fun initializeRecording(context: Context) {
        calculateCodecValues()
        initializeOpusFile(context)
        initializeCodec()
        resumeAudioRecording(context)
    }


    private fun resumeAudioRecording(context: Context) {
        with(uiState.value) {
            ControllerAudio.initRecorder(context, sampleRate.value, chunkSize, channels.value == 1)
            ControllerAudio.initTrack(sampleRate.value, channels.value == 1)
            ControllerAudio.startRecord()
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(timer = 0, recordingState = RecordingState.Idle) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                releaseResources()
                _amplitudes.update { emptyList() } // Clear the list of amplitudes
                Log.d(TAG, "Recording saved: ${currentOutputFile?.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
            }
        }
    }

    fun pauseRecording() {
        timerJob?.cancel()
        _uiState.update { it.copy(recordingState = RecordingState.Paused) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                ControllerAudio.stopRecord()  // Stop capturing audio frames
                Log.d(TAG, "Recording paused")

            } catch (e: Exception) {
                Log.e(TAG, "Error pausing recording", e)
            }
        }
    }

    fun resumeRecording(context: Context) {
        startTimer()
        _uiState.update { it.copy(recordingState = RecordingState.Recording) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                resumeAudioRecording(context)  // Resume capturing audio frames
                while (_uiState.value.recordingState is RecordingState.Recording) {
                    processAudioFrame()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming recording", e)
            }
        }
    }

    fun discardRecording() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(timer = 0, recordingState = RecordingState.Idle) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Stop recording and release resources
                ControllerAudio.stopRecord()
                ControllerAudio.stopTrack()
                releaseCodec()
                // Close and delete the opus file without saving
                _uiState.value.opusFile?.close()
                fileOutputStream?.close()
                currentOutputFile?.delete() // Delete the file
                // Reset the state
                _uiState.update { it.copy(opusFile = null) }
                fileOutputStream = null
                currentOutputFile = null
                _amplitudes.update { emptyList() }
                Log.d(TAG, "Recording discarded")
            } catch (e: Exception) {
                Log.e(TAG, "Error discarding recording", e)
            }
        }
    }

    /**
     * Processes an audio frame by performing the following steps:
     * 1. Retrieves an audio frame from the ControllerAudio.
     * 2. Calculates the amplitude of the frame.
     * 3. Compares the calculated amplitude with a predefined threshold and adds the appropriate amplitude to the buffer.
     * 4. If the buffer size reaches the smoothing window size, calculates the average amplitude, updates the amplitude list, and removes the oldest amplitude from the buffer.
     * 5. If the buffer size is less than the smoothing window size, updates the amplitude list with the baseline amplitude.
     * 6. Encodes the audio frame using the codec and, if successful, writes and plays the encoded frame.
     */
    private fun processAudioFrame() {
        if (_uiState.value.recordingState is RecordingState.Paused) {
            // If the recording is paused, skip processing the audio frame
            return
        }
        ControllerAudio.getFrame()?.let { frame ->
            computeWaveformAmplitudes(frame)

            codec.encode(frame, _uiState.value.frameSizeByte)?.let { encodedFrame ->
                writeEncodedFrame(encodedFrame)
            }
        }
    }

    private fun computeWaveformAmplitudes(frame: ByteArray) {
        val amplitude = calculateAmplitude(frame)

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


    private fun calculateAmplitude(frame: ByteArray): Int {
        // Calculate the root mean square (RMS) amplitude of the frame
        val sum = frame.sumOf { it * it }
        val rms = sqrt(sum.toDouble() / frame.size)
        return rms.toInt()
    }

    private fun writeEncodedFrame(encodedFrame: ByteArray) {
        val data = OpusAudioData(encodedFrame)
        _uiState.value.opusFile?.writeAudioData(data)
    }

    private fun releaseCodec() {
        codec.encoderRelease()
        codec.decoderRelease()
    }

    private fun releaseResources() {
        try {
            ControllerAudio.stopRecord()
            ControllerAudio.stopTrack()
            releaseCodec()
            _uiState.value.opusFile?.close()
            fileOutputStream?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error stopping recording: ${e.message}", e)
        } finally {
            _uiState.update { it.copy(opusFile = null) }
            fileOutputStream = null
        }
    }

    private fun initializeCodec() {
        codec.encoderInit(_uiState.value.sampleRate, _uiState.value.channels, application)
        codec.decoderInit(_uiState.value.sampleRate, _uiState.value.channels)
    }

    private fun calculateCodecValues() {
        val state = uiState.value
        val defFrameSize = getDefaultFrameSize(state.sampleRate.value)
        val chunkSize = defFrameSize.value * state.channels.value * BYTES_PER_SAMPLE
        val frameSizeShort = FrameSize.fromValue(chunkSize / state.channels.value)
        val frameSizeByte =
            FrameSize.fromValue(chunkSize / BYTES_PER_SAMPLE / state.channels.value)

        _uiState.update {
            it.copy(
                defFrameSize = defFrameSize,
                chunkSize = chunkSize,
                frameSizeShort = frameSizeShort,
                frameSizeByte = frameSizeByte
            )
        }
    }

    private fun getDefaultFrameSize(sampleRate: Int): FrameSize {
        return when (sampleRate) {
            8000 -> FrameSize._160()
            12000 -> FrameSize._240()
            16000 -> FrameSize._160()
            24000 -> FrameSize._240()
            48000 -> FrameSize._120()
            else -> throw IllegalArgumentException("Unsupported sample rate: $sampleRate")
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update {
                    it.copy(timer = it.timer + 1)
                }
            }
        }
    }

    private fun initializeOpusFile(context: Context) {
        val (file, outputStream) = FileUtils.createOutputFile(context)
        currentOutputFile = file
        Log.d(TAG, "Output file path: ${currentOutputFile?.absolutePath}")

        val tags = OpusTags()
        val info = OpusInfo().apply {
            numChannels = _uiState.value.channels.value
            setSampleRate(_uiState.value.sampleRate.value.toLong())
        }
        _uiState.update { it.copy(opusFile = OpusFile(outputStream, info, tags)) }
    }
}

sealed class RecordingState {
    data object Idle : RecordingState()
    data object Recording : RecordingState()
    data object Paused : RecordingState()
}

data class VoiceRecorderUiState(
    val timer: Long = 0,
    val recordingState: RecordingState = RecordingState.Idle,
    val sampleRate: SampleRate = SampleRate._16000(),
    val channelMode: AudioMode = AudioMode.MONO,
    val channels: Channels = Channels.mono(),
    val defFrameSize: FrameSize = FrameSize._160(),
    val chunkSize: Int = 0,
    val frameSizeShort: FrameSize = FrameSize._160(),
    val frameSizeByte: FrameSize = FrameSize._160(),
    val opusFile: OpusFile? = null
)

const val ANIMATION_DURATION = 300

enum class AudioMode { MONO, STEREO }

