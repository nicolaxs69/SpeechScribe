package com.theimpartialai.speechScribe.features.saved_recordings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.theimpartialai.speechScribe.R
import com.theimpartialai.speechScribe.features.saved_recordings.domain.model.AudioRecording
import com.theimpartialai.speechScribe.features.saved_recordings.domain.model.UploadState
import com.theimpartialai.speechScribe.features.saved_recordings.presentation.components.UploadProgressIndicator

@Composable
fun SavedRecordingsScreen(
    recordings: List<AudioRecording>,
    uploadState: UploadState,
    onDelete: (AudioRecording) -> Unit,
    onUpload: (AudioRecording) -> Unit,
    onTogglePlayback: (AudioRecording) -> Unit,
    onResetUploadState: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uploadState) {
        when (uploadState) {
            is UploadState.Success -> {
                snackBarHostState.showSnackbar(message = uploadState.message)
                onResetUploadState()
            }

            is UploadState.Error -> {
                snackBarHostState.showSnackbar(message = uploadState.message)
                onResetUploadState()
            }

            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RecordingList(
                recordings = recordings,
                onUpload = onUpload,
                onDelete = onDelete,
                onTogglePlayback = onTogglePlayback,
            )
        }

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
fun RecordingList(
    recordings: List<AudioRecording>,
    onDelete: (AudioRecording) -> Unit,
    onUpload: (AudioRecording) -> Unit,
    onTogglePlayback: (AudioRecording) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, top = 4.dp)
    ) {
        items(recordings) { recording ->
            RecordingItem(
                recording = recording,
                onUpload = { onUpload(recording) },
                onDelete = { onDelete(recording) },
                onTogglePlayback = { onTogglePlayback(recording) },
            )
        }
    }
}

@Composable
fun RecordingItem(
    recording: AudioRecording,
    onDelete: () -> Unit,
    onUpload: () -> Unit,
    onTogglePlayback: () -> Unit,
) {
    val showMenu = remember { mutableStateOf(false) }

    val gradientColors = listOf(
        Color(0xFF333368),
        Color(0xFF151329),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(gradientColors),
                shape = MaterialTheme.shapes.small
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(Color(0xFF1A1A1A), MaterialTheme.shapes.small)
        ) {
            Icon(
                modifier = Modifier
                    .padding(1.dp)
                    .size(45.dp)
                    .clickable { onTogglePlayback() },
                imageVector = if (recording.isPlaying) {
                    ImageVector.vectorResource(id = R.drawable.pause_icon)
                } else {
                    ImageVector.vectorResource(id = R.drawable.mic_icon)
                },
                tint = Color.Red,
                contentDescription = "Waveform"
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = recording.fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recording.formattedDuration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
                Text(
                    text = recording.formattedFileSize,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
            }
        }

        if (recording.isUploading) {
            UploadProgressIndicator()
        } else {
            Icon(
                modifier = Modifier
                    .size(35.dp)
                    .clickable { showMenu.value = true },
                imageVector = Icons.Default.MoreVert,
                tint = Color.LightGray,
                contentDescription = "More options"
            )

            DropdownMenu(
                expanded = showMenu.value,
                onDismissRequest = { showMenu.value = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        onDelete()
                        showMenu.value = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Upload to server") },
                    onClick = {
                        onUpload()
                        showMenu.value = false
                    }
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}


@Preview(showBackground = true)
@Composable
fun SavedRecordingsScreenPreview() {
    SavedRecordingsScreen(
        recordings = List(25) { index ->
            AudioRecording(
                fileName = "Recording ${index + 1}",
                filePath = "",
                fileSize = (index + 1) * 1.5,
                duration = ((index + 1) * 10.0).toLong(),
                timeStamp = System.currentTimeMillis()
            )
        },
        uploadState = UploadState.Idle,
        onUpload = {},
        onDelete = {},
        onTogglePlayback = {},
        onResetUploadState = {}
    )
}