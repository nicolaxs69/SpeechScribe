package com.theimpartialai.speechScribe.core.components.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.theimpartialai.speechScribe.features.recording.presentation.RecordingScreen
import com.theimpartialai.speechScribe.features.recording.presentation.RecordingScreenViewModel
import com.theimpartialai.speechScribe.features.saved_recordings.presentation.SavedRecordingsScreen
import com.theimpartialai.speechScribe.features.saved_recordings.presentation.SavedRecordingsScreenViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    recordingScreenViewModel: RecordingScreenViewModel,
    savedRecordingsViewModel: SavedRecordingsScreenViewModel,
    checkAndRequestPermissions: () -> Unit,
    onBottomBarVisibilityChanged: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = NavigationItem.RecordingScreen.route
    ) {

        composable(NavigationItem.SavedRecordings.route) {
            onBottomBarVisibilityChanged(true)

            val recordings by savedRecordingsViewModel.recordings.collectAsState()
            val uploadState by savedRecordingsViewModel.uploadState.collectAsState()

            LaunchedEffect(Unit) {
                savedRecordingsViewModel.loadRecordings()
            }

            SavedRecordingsScreen(
                recordings = recordings,
                uploadState = uploadState,
                onDelete = { recording ->
                    savedRecordingsViewModel.deleteRecording(recording)
                },
                onUpload = { recording ->
                    savedRecordingsViewModel.uploadRecording(recording)
                },
                onTogglePlayback = { recording ->
                    savedRecordingsViewModel.togglePlayback(recording)
                },
                onResetUploadState = { savedRecordingsViewModel.resetUploadStatus() }
            )
        }

        composable(NavigationItem.RecordingScreen.route) {
            onBottomBarVisibilityChanged(true)

            val recordingState by recordingScreenViewModel.uiState.collectAsState()
            val amplitudes by recordingScreenViewModel.amplitudes.collectAsState()

            RecordingScreen(
                uiState = recordingState,
                amplitudes = amplitudes,
                onStartRecording = { checkAndRequestPermissions() },
                onPauseRecording = { recordingScreenViewModel.pauseRecording() },
                onResumeRecording = {
                    recordingScreenViewModel.resumeRecording()
                },
                onStopRecording = { recordingScreenViewModel.stopRecording() },
                onDiscardRecording = { recordingScreenViewModel.discardRecording() }
            )
        }

        composable(NavigationItem.Settings.route) {
            onBottomBarVisibilityChanged(true)
            // TODO("Implement SettingsScreen()")
        }
    }
}