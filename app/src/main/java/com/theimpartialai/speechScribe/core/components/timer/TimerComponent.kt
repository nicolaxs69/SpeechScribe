import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun TimerComponent(
    timerValue: Long,
    isPaused: Boolean,
    isRecording: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            text = timerValue.formatTime(),
            fontSize = 44.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = when {
                isPaused -> "Paused"
                isRecording -> "Recording"
                else -> ""
            },
            fontSize = 14.sp,
            color = Color.White
        )
    }
}

fun Long.formatTime(): String {
    val seconds = this % 60
    val minutes = (this / 60) % 60
    val hours = this / 3600
    return String.format(Locale.getDefault(), "%02d : %02d : %02d", hours, minutes, seconds)
}

@Preview(showBackground = true)
@Composable
fun TimerComponentPreview() {
    TimerComponent(
        timerValue = 1000,
        isRecording = true,
        isPaused = false
    )
}



