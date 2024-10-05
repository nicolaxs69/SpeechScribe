package com.theimpartialai.speechScribe

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private var permissionToRecordAccepted = false

    private val PERMISSIONS_REQUESTED_CODE = 123


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionToRecordAccepted = isGranted
        if (!permissionToRecordAccepted) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        checkAndRequestPermission()

        setContent {
//            SpeechScribeTheme {
//                VoiceRecorderScreen(
//                    viewModel = VoiceRecorderViewModel()
//                )
//            }
        }
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == PERMISSIONS_REQUESTED_CODE) {
//            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
////                startRecording()
//            } else {
//                val deniedPermissions =
//                    permissions.filterIndexed { index, _ ->
//
//                        grantResults[index] != PackageManager.PERMISSION_GRANTED
//                    }
//                handleDeniedPermissions(deniedPermissions)
//            }
//        }
//    }

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

