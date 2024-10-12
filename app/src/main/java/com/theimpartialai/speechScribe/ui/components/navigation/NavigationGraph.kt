package com.theimpartialai.speechScribe.ui.components.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.theimpartialai.speechScribe.ui.recording.RecordingScreen
import com.theimpartialai.speechScribe.ui.recording.RecordingScreenViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    onBottomBarVisibilityChanged: (Boolean) -> Unit,
    recordingScreenViewModel: RecordingScreenViewModel,
    checkAndRequestPermissions: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = NavigationItem.RecordingScreen.route
    ) {

        composable(NavigationItem.SavedRecordings.route) {
            onBottomBarVisibilityChanged(true)
            TODO("Implement SavedRecordingsScreen()")
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
            TODO("Implement SettingsScreen()")
        }
    }
}