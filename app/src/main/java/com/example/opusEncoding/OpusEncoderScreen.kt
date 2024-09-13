package com.example.opusEncoding

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.opus.Constants

private lateinit var SAMPLE_RATE: Constants.SampleRate

@Composable
fun OpusEncoderScreen() {

    val isRecording = false
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SampleRateSlider()
//        Spacer(modifier = Modifier.padding(12.dp))
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
fun SampleRateSlider() {
    var slidePosition by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Slider(
            modifier = Modifier.padding(horizontal = 8.dp),
            value = slidePosition,
            onValueChange = { progress ->
                slidePosition = progress
                SAMPLE_RATE = getSampleRate(progress.toInt())
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            valueRange = 0f..50f,
            steps = 4,
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            text = "Sample rate: ${slidePosition.toInt()}",
        )
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

private fun getSampleRate(v: Int): Constants.SampleRate {
    return try {
        when (v) {
            0 -> Constants.SampleRate._8000()
            1 -> Constants.SampleRate._12000()
            2 -> Constants.SampleRate._16000()
            3 -> Constants.SampleRate._24000()
            4 -> Constants.SampleRate._48000()
            else -> throw IllegalArgumentException("Invalid sample rate value: $v")
        }
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        println("Invalid sample rate value provided. Defaulting to 16K.")
        Constants.SampleRate._16000() // Default to 8000 if an invalid value is provided
    }
}


@Preview(showBackground = true)
@Composable
fun OpusEncoderScreenPreview() {
    OpusEncoderScreen()
}