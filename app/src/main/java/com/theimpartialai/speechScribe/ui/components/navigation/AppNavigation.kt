package com.theimpartialai.speechScribe.ui.components.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen {
    RECORDING_SCREEN, SAVED_RECORDINGS, SETTINGS
}

sealed class NavigationItem(val route: String, val icon: ImageVector, val label: String) {
    data object SavedRecordings : NavigationItem(
        Screen.SAVED_RECORDINGS.name,
        icon = Icons.Default.Search,
        label = "Saved Recordings"
    )

    data object RecordingScreen : NavigationItem(
        Screen.RECORDING_SCREEN.name,
        icon = Icons.Default.Home,
        label = "Recording"
    )

    data object Settings : NavigationItem(
        Screen.SETTINGS.name,
        icon = Icons.Default.Person,
        label = "Settings"
    )
}