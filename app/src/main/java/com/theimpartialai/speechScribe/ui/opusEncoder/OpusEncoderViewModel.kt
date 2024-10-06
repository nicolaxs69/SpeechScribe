package com.theimpartialai.speechScribe.ui.opusEncoder

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
import com.theimpartialai.speechScribe.ui.recording.AudioMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.gagravarr.opus.OpusAudioData
import org.gagravarr.opus.OpusFile
import org.gagravarr.opus.OpusInfo
import org.gagravarr.opus.OpusTags
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.sqrt


class OpusEncoderViewModel : ViewModel() {

    companion object {
        private const val TAG = "OpusEncoderViewModel"
        private const val AMPLITUDE_THRESHOLD = 40
        private const val BASELINE_AMPLITUDE = 0
        private const val SMOOTHING_WINDOW_SIZE = 5
        private const val BYTES_PER_SAMPLE = 2 // For 16-bit audio
    }

    private val _uiState = MutableStateFlow(OpusEncoderUiState())
    val uiState: StateFlow<OpusEncoderUiState> = _uiState.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Int>>(emptyList())
    val amplitudes: StateFlow<List<Int>> = _amplitudes.asStateFlow()

    private val codec = Opus()
    private val application = Application.audio()
    private var currentOutputFile: File? = null
    private var fileOutputStream: FileOutputStream? = null

    private val amplitudeBuffer = mutableListOf<Int>()

    fun updateSampleRate(sampleRate: SampleRate) {
        _uiState.update { it.copy(sampleRate = sampleRate) }
        recalculateCodecValues()
    }

    fun updateChannelMode(channelMode: AudioMode) {
        _uiState.update {
            it.copy(
                channelMode = channelMode,
                channels = if (channelMode == AudioMode.MONO) Channels.mono() else Channels.stereo()
            )
        }
        recalculateCodecValues()
    }

    fun startRecording(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _amplitudes.update { emptyList() } // Clear the list of amplitudes
                initializeOpusFile(context)
                initializeCodec()
                initializeAudioControllers(context)
                _uiState.update { it.copy(isRecording = true) }
                startRecordingLoop()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
            }
        }
    }

    private fun initializeAudioControllers(context: Context) {
        with(uiState.value) {
            ControllerAudio.initRecorder(context, sampleRate.value, chunkSize, channels.value == 1)
            ControllerAudio.initTrack(sampleRate.value, channels.value == 1)
            ControllerAudio.startRecord()
        }
    }

    fun stopRecording() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isRecording = false) }
                releaseResources()
                _amplitudes.update { emptyList() } // Clear the list of amplitudes
                Log.d(TAG, "Recording saved: ${currentOutputFile?.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
            }
        }
    }

    private fun startRecordingLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            while (_uiState.value.isRecording) {
                processAudioFrame()
            }
        }
    }

    fun pauseRecording() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isPaused = true) }
                ControllerAudio.stopRecord()  // Stop capturing audio frames
                Log.d(TAG, "Recording paused")
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing recording", e)
            }
        }
    }

    fun resumeRecording(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isPaused = false) }
                initializeAudioControllers(context)  // Resume capturing audio frames
                Log.d(TAG, "Recording resumed")
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming recording", e)
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
        if (_uiState.value.isPaused) {
            // If the recording is paused, skip processing the audio frame
            return
        }

        ControllerAudio.getFrame()?.let { frame ->

            val amplitude = calculateAmplitude(frame)

            val processedAmplitude =
                if (amplitude > AMPLITUDE_THRESHOLD) amplitude else BASELINE_AMPLITUDE
            amplitudeBuffer.add(processedAmplitude)

            if (amplitudeBuffer.size >= SMOOTHING_WINDOW_SIZE) {
                val smoothedAmplitude = amplitudeBuffer.average().toInt()
                amplitudeBuffer.removeAt(0)

                _amplitudes.update { it + smoothedAmplitude }
            } else {
                _amplitudes.update { it + BASELINE_AMPLITUDE }
            }

            codec.encode(frame, _uiState.value.frameSizeByte)?.let { encodedFrame ->
                writeEncodedFrame(encodedFrame)
                playDecodedFrame(encodedFrame)
            }
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

    private fun playDecodedFrame(encodedFrame: ByteArray) {
        codec.decode(encodedFrame, _uiState.value.frameSizeByte)?.let { decodedFrame ->
            ControllerAudio.write(decodedFrame)
        }
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
            Log.e(TAG, "Error releasing resources", e)
        } finally {
            _uiState.update { it.copy(opusFile = null) }
            fileOutputStream = null
        }
    }

    private fun initializeCodec() {
        codec.encoderInit(_uiState.value.sampleRate, _uiState.value.channels, application)
        codec.decoderInit(_uiState.value.sampleRate, _uiState.value.channels)
    }

    private fun recalculateCodecValues() {
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

data class OpusEncoderUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val sampleRate: SampleRate = SampleRate._16000(),
    val channelMode: AudioMode = AudioMode.MONO,
    val channels: Channels = Channels.mono(),
    val defFrameSize: FrameSize = FrameSize._160(),
    val chunkSize: Int = 0,
    val frameSizeShort: FrameSize = FrameSize._160(),
    val frameSizeByte: FrameSize = FrameSize._160(),
    val opusFile: OpusFile? = null
)

enum class AudioMode { MONO, STEREO }

