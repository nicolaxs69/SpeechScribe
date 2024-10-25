package com.theimpartialai.speechScribe.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.theimpartialai.speechScribe.model.AudioRecording
import com.theimpartialai.speechScribe.opusEncoding.utils.FileUtils.getOutputDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AudioRecordingImpl : AudioRecordingInterface {

    private val audioPlayer: AudioPlayer = AudioPlayerImpl()


    override suspend fun loadRecordings(context: Context): List<AudioRecording> {
        val outputDir = getOutputDirectory(context)
        val recordings = getRecordingsFromDirectory(outputDir)
        return recordings
    }

    private fun getRecordingsFromDirectory(directory: File): List<AudioRecording> {
        val files = directory.listFiles { file -> file.extension == "opus" } ?: emptyArray()
        return files.map { file ->
            AudioRecording(
                fileName = file.nameWithoutExtension,
                filePath = file.absolutePath,
                fileSize = file.length().toDouble(),
                duration = 0,
                timeStamp = file.lastModified()
            )
        }
    }


    private fun getAudioDuration(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationStr =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    override suspend fun togglePlayback(
        recording: AudioRecording,
        onPlaybackStarted: () -> Unit,
        onPlaybackPaused: () -> Unit,
        onPlaybackCompleted: () -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                when {
                    recording.isPlaying -> {
                        audioPlayer.pausePlayback()
                        onPlaybackPaused()
                    }

                    recording.isPaused -> {
                        audioPlayer.resumePlayback()
                        onPlaybackStarted()
                    }

                    else -> {
                        // Always start from beginning when starting fresh playback
                        audioPlayer.startPlayback(
                            filePath = recording.filePath,
                            position = 0L,
                            onComplete = onPlaybackCompleted
                        )
                        onPlaybackStarted()
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioRecordingImpl", "Error toggling playback", e)
                onPlaybackCompleted()
            }
        }
    }

    override fun stopPlayback() {
        audioPlayer.stopPlayback()
    }

    override fun release() {
        audioPlayer.release()
    }


    override suspend fun deleteRecording(recording: AudioRecording) {
        val file = File(recording.filePath)
        if (file.exists()) {
            file.delete()
        }
    }
}