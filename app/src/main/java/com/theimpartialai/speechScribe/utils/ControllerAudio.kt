package com.theimpartialai.speechScribe.utils

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File

object ControllerAudio {

    private const val TAG = "ControllerAudio"
    private var frameSize: Int = -1
    private lateinit var recorder: AudioRecord
    private var micEnabled = false
    private lateinit var track: AudioTrack
    private var trackReady = false
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null
    private var audioFile: File? = null


    fun initRecorder(
        context: Context,
        sampleRate: Int,
        frameSize: Int,
        isMono: Boolean,
    ) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Permission to record audio not granted")
            return
        }

        val channelConfig =
            if (isMono) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
        val bufferSize =
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)

        try {
            recorder = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            ControllerAudio.frameSize = frameSize
            audioFile = audioFile

            if (NoiseSuppressor.isAvailable()) {
                try {
                    noiseSuppressor = NoiseSuppressor.create(recorder.audioSessionId)
                    noiseSuppressor?.enabled = true
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to init noise suppressor: $e")
                }
            }

            if (AutomaticGainControl.isAvailable()) {
                try {
                    automaticGainControl = AutomaticGainControl.create(recorder.audioSessionId)
                    automaticGainControl?.enabled = true
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to init automatic gain control: $e")
                }
            }
            onMicStateChange(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing recorder: $e")
        }
    }

    fun startRecord() {
        if (ControllerAudio::recorder.isInitialized && recorder.state == AudioRecord.STATE_INITIALIZED) {
            recorder.startRecording()
            micEnabled = true
        }
    }

    fun getFrame(): ByteArray? {
        if (!ControllerAudio::recorder.isInitialized || !micEnabled) return null
        val frame = ByteArray(frameSize)
        val read = recorder.read(frame, 0, frameSize)
        return if (read == frameSize) frame else null
    }

    fun onMicStateChange(micEnabled: Boolean) {
        ControllerAudio.micEnabled = micEnabled
    }

    fun stopRecord() {
        if (ControllerAudio::recorder.isInitialized) {
            try {
                if (recorder.state == AudioRecord.STATE_INITIALIZED) recorder.stop()
                recorder.release()
                noiseSuppressor?.release()
                automaticGainControl?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recorder: $e")
            }
        }
    }

    fun initTrack(sampleRate: Int, isMono: Boolean) {
        val channelConfig =
            if (isMono) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val bufferSize =
            AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)

        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.play()
                trackReady = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing track: $e")
        }
    }


    fun write(frame: ShortArray) {
        if (trackReady) {
            track.write(frame, 0, frame.size)
        }
    }

    fun write(frame: ByteArray) {
        if (trackReady) {
            track.write(frame, 0, frame.size)
        }
    }

    fun stopTrack() {
        if (ControllerAudio::track.isInitialized && trackReady) {
            if (track.state == AudioTrack.STATE_INITIALIZED) track.stop()
            track.flush()
            track.release()
            trackReady = false
        }
    }

    fun destroy() {
        stopRecord()
        stopTrack()
    }
}