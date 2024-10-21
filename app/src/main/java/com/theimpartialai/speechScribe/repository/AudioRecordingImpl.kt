package com.theimpartialai.speechScribe.repository

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.theimpartialai.speechScribe.model.AudioRecording
import com.theimpartialai.speechScribe.opusEncoding.utils.FileUtils.getOutputDirectory
import com.theimpartialai.speechScribe.ui.savedRecording.PlayBackState
import java.io.File


class AudioRecordingImpl : AudioRecordingInterface {
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecording: AudioRecording? = null
    private var pauseLength = 0
    private var playbackIsFinished = false

    private val playbackPositions = mutableMapOf<String, Int>()

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
        context: Context,
        recording: AudioRecording,
        playBackState: PlayBackState,
        onPlayBackComplete: (PlayBackState) -> Unit,
    ) {
        try {
            if (mediaPlayer == null || currentRecording?.filePath != recording.filePath) {
                // Initialize MediaPlayer with the new recording
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, Uri.fromFile(File(recording.filePath)))
                    prepare()
                    setOnCompletionListener {
                        playbackPositions[recording.filePath] = 0
                        playbackIsFinished = true
                        mediaPlayer?.release()
                        mediaPlayer = null
                        onPlayBackComplete(PlayBackState.Pause)
                    }
                }
                currentRecording = recording
                pauseLength = 0
            }
            when (playBackState) {
                is PlayBackState.Play -> {
                    mediaPlayer?.apply {
                        seekTo(playbackPositions[recording.filePath] ?: 0)
                        start()
                        onPlayBackComplete(PlayBackState.Play)
                    }
                }

                is PlayBackState.Pause -> {
                    mediaPlayer?.apply {
                        playbackPositions[recording.filePath] =
                            if (playbackIsFinished) 0 else currentPosition
                        pause()
                        onPlayBackComplete(PlayBackState.Pause)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecordingImpl", "Error playing recording: ${e.message}")
        }
    }

    override suspend fun deleteRecording(recording: AudioRecording) {
        val file = File(recording.filePath)
        if (file.exists()) {
            file.delete()
        }
    }
}