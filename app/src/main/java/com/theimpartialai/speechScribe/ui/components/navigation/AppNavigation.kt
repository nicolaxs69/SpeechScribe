package com.theimpartialai.speechScribe.ui.components.navigation

import androidx.annotation.DrawableRes
import com.theimpartialai.speechScribe.R

enum class Screen {
    RECORDING_SCREEN, SAVED_RECORDINGS, SETTINGS
}

sealed class NavigationItem(val route: String, @DrawableRes val icon: Int, val label: String) {
    data object SavedRecordings : NavigationItem(
        Screen.SAVED_RECORDINGS.name,
        icon = R.drawable.folder_icon,
        label = "Recordings"
    )

    data object RecordingScreen : NavigationItem(
        Screen.RECORDING_SCREEN.name,
        icon = R.drawable.mic_icon,
        label = "Record"
    )

    data object Settings : NavigationItem(
        Screen.SETTINGS.name,
        icon = R.drawable.settings_icon,
        label = "Settings"
    )
}