package com.example.speechScribe.opusEncoding

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.opus.Constants.Application
import com.example.opus.Constants.Channels
import com.example.opus.Constants.FrameSize
import com.example.opus.Constants.SampleRate
import com.example.opus.Opus
import com.example.speechScribe.opusEncoding.utils.ControllerAudio
import com.example.speechScribe.opusEncoding.utils.FileUtils
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


class OpusEncoderViewModel : ViewModel() {
    private val TAG = "OpusEncoderViewModel"

    private val _uiState = MutableStateFlow(OpusEncoderUiState())
    val uiState: StateFlow<OpusEncoderUiState> = _uiState.asStateFlow()

    private val codec = Opus()
    private val application = Application.audio()
    private var currentOutputFile: File? = null
    private var fileOutputStream: FileOutputStream? = null

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
            initializeOpusFile(context)
            initializeCodec()
            initializeCodec(context)
            _uiState.update { it.copy(isRecording = true) }
            startRecordingLoop()
        }
    }

    private fun initializeCodec(context: Context) {
        with(uiState.value) {
            ControllerAudio.initRecorder(context, sampleRate.value, chunkSize, channels.value == 1)
            ControllerAudio.initTrack(sampleRate.value, channels.value == 1)
            ControllerAudio.startRecord()
        }
    }

    fun stopRecording() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRecording = false) }
            releaseResources()
            Log.d(TAG, "Recording saved: ${currentOutputFile?.absolutePath}")
        }
    }

    private fun startRecordingLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            while (_uiState.value.isRecording) {
                processAudioFrame()
            }
        }
    }

    private fun processAudioFrame() {
        ControllerAudio.getFrame()?.let { frame ->
            codec.encode(frame, _uiState.value.frameSizeByte)?.let { encodedFrame ->
                writeEncodedFrame(encodedFrame)
                playDecodedFrame(encodedFrame)
            }
        }
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
        ControllerAudio.stopRecord()
        ControllerAudio.stopTrack()
        releaseCodec()
        _uiState.value.opusFile?.close()
        fileOutputStream?.close()
        _uiState.update { it.copy(opusFile = null) }
        fileOutputStream = null
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
        val frameSizeByte = FrameSize.fromValue(chunkSize / BYTES_PER_SAMPLE / state.channels.value)

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

private const val BYTES_PER_SAMPLE = 2 // For 16-bit audio


