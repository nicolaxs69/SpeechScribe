package com.example.speechScribe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.speechScribe.ui.theme.RecordingAppTheme

class MainActivity : ComponentActivity() {
    private var permissionToRecordAccepted = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionToRecordAccepted = isGranted
        if (!permissionToRecordAccepted) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermission()

        setContent {
            RecordingAppTheme {
                VoiceRecorderScreen(
                    viewModel = VoiceRecorderViewModel()
                )
            }
        }
    }

    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.RECORD_AUDIO
        } else {
            Manifest.permission.RECORD_AUDIO
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                permissionToRecordAccepted = true
            }

            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}

