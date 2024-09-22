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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.opus.Constants

private enum class SamplingRate(val value: String, val sampleRate: Constants.SampleRate) {
    RATE_8K("8000", Constants.SampleRate._8000()),
    RATE_12K("12000", Constants.SampleRate._12000()),
    RATE_16K("16000", Constants.SampleRate._16000()),
    RATE_24K("24000", Constants.SampleRate._24000()),
    RATE_48K("48000", Constants.SampleRate._48000())
}

@Composable
fun OpusEncoderScreen(
    uiState: OpusEncoderUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSampleRateChange: (Constants.SampleRate) -> Unit,
    onChannelModeChange: (AudioMode) -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SampleRateSlider(
            selectedSampleRate = uiState.sampleRate,
            onSampleRateChange = onSampleRateChange,
            isRecording = uiState.isRecording
        )

        RecordingChannelSettings(
            selectedChannelMode = uiState.channelMode,
            onChannelModeChange = onChannelModeChange,
            isRecording = uiState.isRecording
        )

        Button(
            onClick = if (uiState.isRecording) onStopRecording else onStartRecording,
            enabled = !uiState.isRecording || uiState.opusFile != null
        ) {
            Text(if (uiState.isRecording) "Stop Recording" else "Start Recording")
        }

        if (uiState.isRecording) {
            Text(
                "Recording in progress...",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun SampleRateSlider(
    selectedSampleRate: Constants.SampleRate,
    onSampleRateChange: (Constants.SampleRate) -> Unit,
    isRecording: Boolean
) {
    val selectedSamplingRate = SamplingRate.entries.find { it.sampleRate == selectedSampleRate }
        ?: SamplingRate.RATE_8K

    Column(
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Slider(
            enabled = !isRecording,
            modifier = Modifier.padding(horizontal = 8.dp),
            value = SamplingRate.entries.indexOf(selectedSamplingRate).toFloat(),
            onValueChange = { progress ->
                val newSamplingRate = SamplingRate.entries[progress.toInt()]
                onSampleRateChange(newSamplingRate.sampleRate)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            valueRange = 0f..(SamplingRate.entries.size - 1).toFloat(),
            steps = 3,
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            text = "Sample rate: ${selectedSamplingRate.value} Hz"
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
        uiState = OpusEncoderUiState()
    )
}