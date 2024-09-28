package com.example.speechScribe

import TimerComponent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.speechScribe.opusEncoding.OpusEncoderUiState
import com.example.speechScribe.ui.components.timer.TimerUiState
import com.example.speechScribe.ui.theme.Blue
import com.example.speechScribe.ui.theme.LightGray
import com.linc.audiowaveform.AudioWaveform
import com.linc.audiowaveform.infiniteVerticalGradient
import com.linc.audiowaveform.model.AmplitudeType
import com.linc.audiowaveform.model.WaveformAlignment

@Composable
fun VoiceRecorderScreen(
    uiState: VoiceRecorderUiState,
    timerUiState: TimerUiState,
    amplitudes: List<Int>,
    onStartRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onStopRecording: () -> Unit,
) {

    val animatedGradientBrush = Brush.infiniteVerticalGradient(
        colors = listOf(Color.Blue, Color.Green),
        animation = tween(durationMillis = 2000, easing = LinearEasing),
        width = 128F
    )

    Column(
        modifier = Modifier
            .padding(top = 360.dp)
            .fillMaxSize(),
//        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        AudioWaveform(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(100.dp),
//            amplitudes = amplitudes,
//            amplitudeType = AmplitudeType.Avg,
//            spikeWidth = 4.dp,
//            spikePadding = 2.dp,
//            spikeRadius = 4.dp,
//            progress = 1f, // Use 1f for full progress since it's live data
//            progressBrush = animatedGradientBrush,
//            waveformBrush = SolidColor(Color.LightGray),
//            waveformAlignment = WaveformAlignment.Center,
//            onProgressChange = {},
//            onProgressChangeFinished = {}
//        )

        TimerComponent(timerValue = uiState.timer)

        Spacer(modifier = Modifier.height(150.dp))

        RecordingButtons(
            uiState,
            timerUiState,
            onPauseRecording,
            onResumeRecording,
            onStartRecording,
            onStopRecording
        )
    }
}

@Composable
private fun RecordingButtons(
    uiState: VoiceRecorderUiState,
    timerUiState: TimerUiState,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            modifier = Modifier
                .padding(8.dp)
                .size(56.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = Blue),
            onClick = { onStopRecording() },
            content = {
                Icon(
                    modifier = Modifier
                        .size(30.dp),
                    imageVector = Icons.Default.Close,
                    tint = Color.White,
                    contentDescription = "Stop"
                )
            }
        )

        IconButton(
            modifier = Modifier
                .padding(8.dp)
                .size(80.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = LightGray),
            onClick = {
                when {
                    uiState.isRecording && !uiState.isPaused -> {
                        onPauseRecording()
                    }

                    uiState.isRecording && uiState.isPaused -> {
                        onResumeRecording()
                    }

                    else -> {
                        onStartRecording()
                    }
                }
            },
            content = {
                Icon(
                    modifier = Modifier
                        .size(40.dp),
                    imageVector = if (!uiState.isPaused && uiState.isRecording) {
                        ImageVector.vectorResource(id = R.drawable.pause_icon)
                    } else {
                        ImageVector.vectorResource(id = R.drawable.mic_icon)
                    },
                    tint = Color.Red,
                    contentDescription = "Record"
                )
            }
        )

        IconButton(
            modifier = Modifier
                .padding(8.dp)
                .size(56.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = Blue),
            onClick = { onStopRecording() },
            content = {
                Icon(
                    modifier = Modifier
                        .size(30.dp),
                    imageVector = Icons.Default.Done,
                    tint = Color.White,
                    contentDescription = "Save"
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceRecorderScreenPreview() {
    VoiceRecorderScreen(
        timerUiState = TimerUiState(),
        uiState = VoiceRecorderUiState(),
        amplitudes = listOf(1, 2, 3, 4, 5),
        onStartRecording = {},
        onPauseRecording = {},
        onResumeRecording = {},
        onStopRecording = {},
    )
}
