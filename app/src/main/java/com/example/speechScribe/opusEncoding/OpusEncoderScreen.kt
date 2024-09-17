package com.example.speechScribe.opusEncoding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.opus.Constants

enum class AudioMode { MONO, STEREO }

private enum class SamplingRate(val value: String, val sampleRate: Constants.SampleRate) {
    RATE_8K("8000", Constants.SampleRate._8000()),
    RATE_12K("12000", Constants.SampleRate._12000()),
    RATE_16K("16000", Constants.SampleRate._16000()),
    RATE_24K("24000", Constants.SampleRate._24000()),
    RATE_48K("48000", Constants.SampleRate._48000())
}

@Composable
fun OpusEncoderScreen(
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSampleRateChange: (Constants.SampleRate) -> Unit,
    onChannelModeChange: (AudioMode) -> Unit,
    selectedChannelMode: AudioMode
) {

    var isRecording by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SampleRateSlider(onSampleRateChange, isRecording)
        Button(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            onClick = {
                isRecording = !isRecording
                if (isRecording) {
                    onStartRecording()
                } else {
                    onStopRecording()
                }
            },
        ) {
            Text(if (isRecording) "Stop recording" else "Start recording")
        }

        RecordingChannelSettings(
            selectedChannelMode = selectedChannelMode,
            onChannelModeChange = onChannelModeChange,
            isRecording = isRecording
        )
    }
}

@Composable
fun SampleRateSlider(
    onSampleRateChange: (Constants.SampleRate) -> Unit,
    isRecording: Boolean
) {
    var selectedSampleRate by remember { mutableStateOf(SamplingRate.RATE_8K) }

    Column(
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Slider(
            enabled = !isRecording,
            modifier = Modifier.padding(horizontal = 8.dp),
            value = selectedSampleRate.ordinal.toFloat(),
            onValueChange = { progress ->
                selectedSampleRate = SamplingRate.entries.toTypedArray()[progress.toInt()]
                onSampleRateChange(selectedSampleRate.sampleRate)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            valueRange = 0f..(SamplingRate.entries.size - 1f),
            steps = 3,
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            text = "Sample rate: ${selectedSampleRate.value} Hz",
        )
    }
}

@Composable
fun RecordingChannelSettings(
    selectedChannelMode: AudioMode,
    onChannelModeChange: (AudioMode) -> Unit,
    isRecording: Boolean
) {
    Row(
        modifier = Modifier.padding(8.dp)
    ) {
        ChannelButton(
            label = "Mono",
            selected = selectedChannelMode == AudioMode.MONO,
            onClick = {
                onChannelModeChange(AudioMode.MONO)
            },
            enabled = !isRecording
        )
        ChannelButton(
            label = "Stereo",
            selected = selectedChannelMode == AudioMode.STEREO,
            onClick = {
                onChannelModeChange(AudioMode.STEREO)
            },
            enabled = !isRecording
        )
    }
}

@Composable
fun ChannelButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            enabled = enabled,
            selected = selected,
            onClick = onClick,
        )
        Text(label)
    }
}

@Preview(showBackground = true)
@Composable
fun OpusEncoderScreenPreview() {
    OpusEncoderScreen(
        onStartRecording = {},
        onStopRecording = {},
        onChannelModeChange = {},
        onSampleRateChange = {},
        selectedChannelMode = AudioMode.MONO
    )
}