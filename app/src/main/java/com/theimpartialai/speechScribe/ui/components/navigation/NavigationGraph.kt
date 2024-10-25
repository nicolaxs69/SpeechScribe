package com.theimpartialai.speechScribe.ui.components.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.theimpartialai.speechScribe.ui.recording.RecordingScreen
import com.theimpartialai.speechScribe.ui.recording.RecordingScreenViewModel
import com.theimpartialai.speechScribe.ui.savedRecording.SavedRecordingsScreen
import com.theimpartialai.speechScribe.ui.savedRecording.SavedRecordingsViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    onBottomBarVisibilityChanged: (Boolean) -> Unit,
    recordingScreenViewModel: RecordingScreenViewModel,
    savedRecordingsViewModel: SavedRecordingsViewModel,
    checkAndRequestPermissions: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = NavigationItem.RecordingScreen.route
    ) {

        composable(NavigationItem.SavedRecordings.route) {
            onBottomBarVisibilityChanged(true)

            val context = LocalContext.current
            val recordings by savedRecordingsViewModel.recordings.collectAsState()

            LaunchedEffect(Unit) {
                savedRecordingsViewModel.loadRecordings(context)
            }

            SavedRecordingsScreen(
                recordings = recordings,
                onDelete = { savedRecordingsViewModel.deleteRecording(it) },
                onTogglePlayback = { recording ->
                    savedRecordingsViewModel.togglePlayback(recording)
                },
                onMoreOptions = {}
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
                onResumeRecording = { context ->
                    recordingScreenViewModel.resumeRecording(context)
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