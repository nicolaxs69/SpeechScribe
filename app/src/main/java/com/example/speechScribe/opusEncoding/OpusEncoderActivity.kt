package com.example.speechScribe.opusEncoding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.opus.Constants
import com.example.opus.Opus
import com.example.speechScribe.ui.theme.SpeechScribeTheme

private const val TAG = "OpusEncoderActivity"

private val codec = Opus()
private val APPLICATION = Constants.Application.audio()
private var CHUNK_SIZE = 0
private lateinit var SAMPLE_RATE: Constants.SampleRate
private lateinit var CHANNELS: Constants.Channels
private lateinit var DEF_FRAME_SIZE: Constants.FrameSize
private lateinit var FRAME_SIZE_SHORT: Constants.FrameSize
private lateinit var FRAME_SIZE_BYTE: Constants.FrameSize

private var runLoop = false

class OpusEncoderActivity : AppCompatActivity() {
    private val PERMISSIONS_REQUESTED_CODE = 123

    private val requiredPermissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT <= VERSION_CODES.P) {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SAMPLE_RATE = Constants.SampleRate._16000()
        CHANNELS = Constants.Channels.mono()

        setContent {
            var selectedChannelMode by remember { mutableStateOf(AudioMode.STEREO) }

            SpeechScribeTheme {
                OpusEncoderScreen(
                    onSampleRateChange = { newSampleRate ->
                        SAMPLE_RATE = newSampleRate
                    },
                    onStartRecording = {
                        checkAndRequestPermissions()
                    },
                    onStopRecording = {
                        stopRecording()
                    },
                    onChannelModeChange = { newChannelMode ->
                        selectedChannelMode = newChannelMode
                        CHANNELS =
                            if (newChannelMode == AudioMode.MONO) {
                                Constants.Channels.mono()
                            } else {
                                Constants.Channels.stereo()
                            }
                    },
                    selectedChannelMode = selectedChannelMode
                )
            }
        }
    }

    private fun startLoop() {
        stopLoop()

        recalculateCodecValues()

        codec.encoderInit(SAMPLE_RATE, CHANNELS, APPLICATION)
        codec.decoderInit(SAMPLE_RATE, CHANNELS)

        ControllerAudio.initRecorder(this, SAMPLE_RATE.v, CHUNK_SIZE, CHANNELS.v == 1)
        ControllerAudio.initTrack(SAMPLE_RATE.v, CHANNELS.v == 1)
        ControllerAudio.startRecord()
        runLoop = true
        Thread {
            while (runLoop) {
                handleBytes()
            }
            if (!runLoop) {
                codec.encoderRelease()
                codec.decoderRelease()
            }
        }.start()
    }

    private fun stopLoop() {
        runLoop = false
    }

    private fun stopRecording() {
        stopLoop()
        ControllerAudio.stopRecord()
        ControllerAudio.stopTrack()
    }


    private fun handleBytes() {
        val frame = ControllerAudio.getFrame() ?: return
        val encodedFrame = codec.encode(frame, FRAME_SIZE_BYTE) ?: return
        Log.d(
            TAG,
            "encoded: ${frame.size} bytes of ${if (CHANNELS.v == 1) "MONO" else "STEREO"} audio into ${encodedFrame.size} bytes"
        )

        val decodedFrame = codec.decode(encodedFrame, FRAME_SIZE_BYTE) ?: return
        Log.d(TAG, "decoded: ${decodedFrame.size} bytes")

        ControllerAudio.write(decodedFrame)
        Log.d(TAG, "===========================================")
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSIONS_REQUESTED_CODE
            )
        } else {
            startLoop()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUESTED_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    startLoop()
                } else {
                    val deniedPermissions =
                        permissions.filterIndexed { index, _ -> grantResults[index] != PackageManager.PERMISSION_GRANTED }
                    handleDeniedPermissions(deniedPermissions)
                }
            }
        }
    }

    private fun handleDeniedPermissions(deniedPermissions: List<String>) {
        val message = when {
            Manifest.permission.RECORD_AUDIO in deniedPermissions ->
                "Audio recording permission is required for this app to function."

            deniedPermissions.isNotEmpty() ->
                "Some permissions were denied. The app may not function properly."

            else -> "Unknown error occurred with permissions."
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun recalculateCodecValues() {
        DEF_FRAME_SIZE = getDefaultFrameSize(SAMPLE_RATE.v)
        /** "CHUNK_SIZE = DEF_FRAME_SIZE.v * CHANNELS.v * 2" it's formula from opus.h "frame_size*channels*sizeof(opus_int16)" */
        CHUNK_SIZE =
            DEF_FRAME_SIZE.v * CHANNELS.v * 2                                              // bytes or shorts in a frame
        FRAME_SIZE_SHORT =
            Constants.FrameSize.fromValue(CHUNK_SIZE / CHANNELS.v)            // samples per channel
        FRAME_SIZE_BYTE =
            Constants.FrameSize.fromValue(CHUNK_SIZE / 2 / CHANNELS.v)         // samples per channel
    }

    private fun getDefaultFrameSize(v: Int): Constants.FrameSize {
        return when (v) {
            8000 -> Constants.FrameSize._160()
            12000 -> Constants.FrameSize._240()
            16000 -> Constants.FrameSize._160()
            24000 -> Constants.FrameSize._240()
            48000 -> Constants.FrameSize._120()
            else -> throw IllegalArgumentException()
        }
    }
}