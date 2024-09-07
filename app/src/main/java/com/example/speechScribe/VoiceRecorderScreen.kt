package com.example.speechScribe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun VoiceRecorderScreen(
    viewModel: VoiceRecorderViewModel
) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initializeFileName(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Button(
            modifier = Modifier
                .padding(8.dp),
            onClick = {
                if (isRecording) viewModel.stopRecording() else viewModel.startRecording(context)
            }
        )
        {
            Text(if (isRecording) "Stop recording" else "Start recording")
        }

        Button(
            modifier = Modifier
                .padding(8.dp),
            onClick = {
                if (isPlaying) viewModel.stopPlaying() else viewModel.startPlaying()
            }
        )
        {
            Text(if (isPlaying) "Stop playing" else "Start playing")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceRecorderScreenPreview() {
    VoiceRecorderScreen(viewModel = VoiceRecorderViewModel())
}
