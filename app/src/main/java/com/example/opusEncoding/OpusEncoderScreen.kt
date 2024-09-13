package com.example.opusEncoding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun OpusEncoderScreen() {

    val isRecording = false
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            onClick = {}
        ) {
            if (isRecording) Text("Stop recording") else Text("Start recording")
        }

        RecordingChannelSettings()
    }
}

@Composable
fun RecordingChannelSettings() {
    Row(
        modifier = Modifier.padding(8.dp)
    ) {
        MonoChannelButton()
        StereoChannelButton()
    }
}

@Composable
fun MonoChannelButton() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = true,
            onClick = {
//                onMonoChannelSelected()
            },
        )
        Text("Mono")
    }
}

@Composable
fun StereoChannelButton() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = false,
            onClick = {
                //onStereoChannelSelected()
            },
        )
        Text("Stereo")
    }
}

@Preview(showBackground = true)
@Composable
fun OpusEncoderScreenPreview() {
    OpusEncoderScreen()
}