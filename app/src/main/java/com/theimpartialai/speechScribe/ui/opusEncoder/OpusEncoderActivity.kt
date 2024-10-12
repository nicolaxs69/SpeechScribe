package com.theimpartialai.speechScribe.ui.opusEncoder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.theimpartialai.speechScribe.ui.components.navigation.BottomNavigationBar
import com.theimpartialai.speechScribe.ui.components.navigation.NavigationGraph
import com.theimpartialai.speechScribe.ui.recording.RecordingScreenViewModel
import com.theimpartialai.speechScribe.ui.theme.DarkBlue
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
            SpeechScribeTheme {
                val navController: NavHostController = rememberNavController()
                var buttonVisible by remember { mutableStateOf(true) }

                Scaffold(
                    containerColor = DarkBlue,
                    bottomBar = {
                        if (buttonVisible) {
                            BottomNavigationBar(
                                modifier = Modifier,
                                navController = navController,
                                state = buttonVisible
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .padding(paddingValues)
                    ) {
                        NavigationGraph(
                            navController = navController,
                            recordingScreenViewModel = voiceRecorderViewModel,
                            checkAndRequestPermissions = { checkAndRequestPermissions() },
                            onBottomBarVisibilityChanged = { isVisible ->
                                buttonVisible = isVisible
                            },
                        )
                    }
                }
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