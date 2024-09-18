package com.example.speechScribe.opusEncoding

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.opus.Constants
import com.example.opus.Opus
import com.example.speechScribe.opusEncoding.utils.ControllerAudio
import com.example.speechScribe.opusEncoding.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val application = Constants.Application.audio()
    private var currentOutputFile: File? = null

    fun updateSampleRate(sampleRate: Constants.SampleRate) {
        _uiState.value = _uiState.value.copy(sampleRate = sampleRate)
        recalculateCodecValues()
    }

    fun updateChannelMode(channelMode: AudioMode) {
        _uiState.value = _uiState.value.copy(
            channelMode = channelMode,
            channels = if (channelMode == AudioMode.MONO) Constants.Channels.mono() else Constants.Channels.stereo()
        )
        recalculateCodecValues()
    }

    fun startRecording(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            initializeOpusFile(context)
            initializeCodec()
            ControllerAudio.initRecorder(
                context,
                _uiState.value.sampleRate.v,
                _uiState.value.chunkSize,
                _uiState.value.channels.v == 1
            )
            ControllerAudio.initTrack(_uiState.value.sampleRate.v, _uiState.value.channels.v == 1)
            ControllerAudio.startRecord()
            _uiState.value = _uiState.value.copy(isRecording = true)
            startRecordingLoop()
        }
    }

    fun stopRecording() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isRecording = false)
            ControllerAudio.stopRecord()
            ControllerAudio.stopTrack()
            releaseCodec()
            _uiState.value.opusFile?.close()
            _uiState.value.fileOutputStream?.close()
            _uiState.value = _uiState.value.copy(opusFile = null, fileOutputStream = null)
            Log.d(TAG, "Recording saved: ${currentOutputFile?.absolutePath}")
        }
    }

    private fun startRecordingLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            while (_uiState.value.isRecording) {
                handleBytes()
            }
        }
    }

    private fun handleBytes() {
        val frame = ControllerAudio.getFrame() ?: return
        val encodedFrame = codec.encode(frame, _uiState.value.frameSizeByte) ?: return
        val data = OpusAudioData(encodedFrame)

        _uiState.value.opusFile?.writeAudioData(data)
        val decodedFrame = codec.decode(encodedFrame, _uiState.value.frameSizeByte) ?: return
        ControllerAudio.write(decodedFrame)
    }

    private fun releaseCodec() {
        codec.encoderRelease()
        codec.decoderRelease()
    }

    private fun initializeCodec() {
        codec.encoderInit(_uiState.value.sampleRate, _uiState.value.channels, application)
        codec.decoderInit(_uiState.value.sampleRate, _uiState.value.channels)
    }

    private fun recalculateCodecValues() {
        val defFrameSize = getDefaultFrameSize(_uiState.value.sampleRate.v)
        val chunkSize = defFrameSize.v * _uiState.value.channels.v * 2
        val frameSizeShort = Constants.FrameSize.fromValue(chunkSize / _uiState.value.channels.v)
        val frameSizeByte = Constants.FrameSize.fromValue(chunkSize / 2 / _uiState.value.channels.v)

        _uiState.value = _uiState.value.copy(
            defFrameSize = defFrameSize,
            chunkSize = chunkSize,
            frameSizeShort = frameSizeShort,
            frameSizeByte = frameSizeByte
        )
    }

    private fun getDefaultFrameSize(sampleRate: Int): Constants.FrameSize {
        return when (sampleRate) {
            8000 -> Constants.FrameSize._160()
            12000 -> Constants.FrameSize._240()
            16000 -> Constants.FrameSize._160()
            24000 -> Constants.FrameSize._240()
            48000 -> Constants.FrameSize._120()
            else -> throw IllegalArgumentException("Unsupported sample rate: $sampleRate")
        }
    }

    private fun initializeOpusFile(context: Context) {
        val (file, outputStream) = FileUtils.createOutputFile(context)
        currentOutputFile = file
        Log.d(TAG, "Output file path: ${currentOutputFile?.absolutePath}")

        val tags = OpusTags()
        val info = OpusInfo().apply {
            numChannels = _uiState.value.channels.v
            setSampleRate(_uiState.value.sampleRate.v.toLong())
        }
        val opusFile = OpusFile(outputStream, info, tags)
        _uiState.value = _uiState.value.copy(opusFile = opusFile, fileOutputStream = outputStream)
    }

}


data class OpusEncoderUiState(
    val isRecording: Boolean = false,
    val sampleRate: Constants.SampleRate = Constants.SampleRate._16000(),
    val channelMode: AudioMode = AudioMode.MONO,
    val channels: Constants.Channels = Constants.Channels.mono(),
    val defFrameSize: Constants.FrameSize = Constants.FrameSize._160(),
    val chunkSize: Int = 0,
    val frameSizeShort: Constants.FrameSize = Constants.FrameSize._160(),
    val frameSizeByte: Constants.FrameSize = Constants.FrameSize._160(),
    val opusFile: OpusFile? = null,
    val fileOutputStream: FileOutputStream? = null
)
