package com.theimpartialai.speechScribe.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import com.theimpartialai.speechScribe.model.AudioRecording
import com.theimpartialai.speechScribe.opusEncoding.utils.FileUtils.getOutputDirectory
import java.io.File

class AudioRecordingImpl : AudioRecordingInterface {
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


    override suspend fun deleteRecording(recording: AudioRecording) {
        val file = File(recording.filePath)
        if (file.exists()) {
            file.delete()
        }
    }
}