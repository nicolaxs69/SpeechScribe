package com.example.speechScribe.opusEncoding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.speechScribe.VoiceRecorderScreen
import com.example.speechScribe.VoiceRecorderViewModel
import com.example.speechScribe.ui.components.timer.TimerViewModel
import com.example.speechScribe.ui.theme.SpeechScribeTheme

class OpusEncoderActivity : AppCompatActivity() {
    private val viewModel: OpusEncoderViewModel by viewModels()
    private val voiceRecorderViewModel: VoiceRecorderViewModel by viewModels()
    private val timerViewmodel: TimerViewModel by viewModels()

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
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val amplitudes by viewModel.amplitudes.collectAsState()
            val timer by timerViewmodel.timerUiState.collectAsState()
            val recordingState by voiceRecorderViewModel.uiState.collectAsState()
            SpeechScribeTheme {

                VoiceRecorderScreen(
                    timerUiState = timer,
                    uiState = recordingState,
                    amplitudes = amplitudes,
                    onStartRecording = {
                        voiceRecorderViewModel.startRecording(this)
//                        checkAndRequestPermissions()
                    },
                    onPauseRecording = {
                        voiceRecorderViewModel.pauseRecording()
//                        viewModel.pauseRecording()
                    },
                    onResumeRecording = {
                        voiceRecorderViewModel.resumeRecording(this)
//                        timerViewmodel.startTimer()
//                        viewModel.resumeRecording(this)
                    },
                    onStopRecording = {
                        voiceRecorderViewModel.stopRecording()
//                        timerViewmodel.stopTimer()
//                        viewModel.stopRecording()
                    },
                )

//                OpusEncoderScreen(
//                    uiState = uiState,
//                    amplitudes = amplitudes,
//                    onStartRecording = { checkAndRequestPermissions() },
//                    onStopRecording = { viewModel.stopRecording() },
//                    onPauseRecording = { viewModel.pauseRecording() },
//                    onResumeRecording = { viewModel.resumeRecording(this) },
//                    onSampleRateChange = { viewModel.updateSampleRate(it) },
//                    onChannelModeChange = { viewModel.updateChannelMode(it) }
//                )
            }
        }
    }

    private fun startRecording() {
//        viewModel.startRecording(this)
//        timerViewmodel.startTimer()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSIONS_REQUESTED_CODE
            )
        } else {
            startRecording()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUESTED_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startRecording()
            } else {
                val deniedPermissions =
                    permissions.filterIndexed { index, _ ->

                        grantResults[index] != PackageManager.PERMISSION_GRANTED
                    }
                handleDeniedPermissions(deniedPermissions)
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
}