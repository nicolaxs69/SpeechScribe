package com.theimpartialai.speechScribe.opusEncoding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.theimpartialai.speechScribe.RecordingScreen
import com.theimpartialai.speechScribe.RecordingScreenViewModel
import com.theimpartialai.speechScribe.ui.theme.SpeechScribeTheme

class OpusEncoderActivity : AppCompatActivity() {
    private val voiceRecorderViewModel: RecordingScreenViewModel by viewModels()

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
            val amplitudes by voiceRecorderViewModel.amplitudes.collectAsState()
            val recordingState by voiceRecorderViewModel.uiState.collectAsState()
            SpeechScribeTheme {
                Log.d("OpusEncoderActivity", "onCreate: ${amplitudes.size}")
                RecordingScreen(
                    uiState = recordingState,
                    amplitudes = amplitudes,
                    onStartRecording = { checkAndRequestPermissions() },
                    onPauseRecording = { voiceRecorderViewModel.pauseRecording() },
                    onResumeRecording = { voiceRecorderViewModel.resumeRecording(this) },
                    onStopRecording = { voiceRecorderViewModel.stopRecording() },
                    onDiscardRecording = { voiceRecorderViewModel.discardRecording() }
                )
            }
        }
    }

    private fun startRecording() {
        voiceRecorderViewModel.startRecording(this)
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