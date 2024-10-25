package com.theimpartialai.speechScribe.repository

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.util.Log
import com.theimpartialai.speechScribe.model.AudioRecording
import com.theimpartialai.speechScribe.opusEncoding.utils.FileUtils
import com.theimpartialai.speechScribe.opusEncoding.utils.FileUtils.getOutputDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class AudioRecordingImpl : AudioRecordingInterface {

    private val audioPlayer: AudioPlayer = AudioPlayerImpl()
    private var audioRecord: AudioRecord? = null
    private var fileOutputStream: FileOutputStream? = null
    private var currentOutputFile: File? = null
    private var recordingJob: Job? = null
    private var isRecording = false


    @SuppressLint("MissingPermission")
    override suspend fun startRecording(context: Context) {
        withContext(Dispatchers.IO) {

            val (file, outputStream) = FileUtils.createOutputFile(context)
            currentOutputFile = file
            fileOutputStream = outputStream
            writeWavHeader(fileOutputStream!!, 0)

            val minBufferSize =
                AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize
            )
            audioRecord?.startRecording()
            isRecording = true

            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteArray(BUFFER_SIZE)
                while (isActive && isRecording) {
                    val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: -1
                    if (read > 0) {
                        fileOutputStream?.write(buffer, 0, read)
                    }
                }
            }
        }
    }

    override suspend fun pauseRecording() {
        withContext(Dispatchers.IO) {
            isRecording = false
            audioRecord?.stop()
            recordingJob?.cancelAndJoin()
        }
    }

    override suspend fun resumeRecording() {
        withContext(Dispatchers.IO) {
            audioRecord?.startRecording()
            isRecording = true

            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteArray(BUFFER_SIZE)
                while (isActive && isRecording) {
                    val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: -1
                    if (read > 0) {
                        fileOutputStream?.write(buffer, 0, read)
                    }
                }
            }
        }
    }

    override suspend fun stopRecording() {
        withContext(Dispatchers.IO) {
            isRecording = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingJob?.cancelAndJoin()
            fileOutputStream?.close()
            updateWavHeader(currentOutputFile!!)
        }
    }

    override suspend fun discardRecording() {
        withContext(Dispatchers.IO) {
            isRecording = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingJob?.cancelAndJoin()
            fileOutputStream?.close()
            currentOutputFile?.delete()
            currentOutputFile = null
        }
    }


    override suspend fun loadRecordings(context: Context): List<AudioRecording> {
        val outputDir = getOutputDirectory(context)
        val recordings = getRecordingsFromDirectory(outputDir)
        return recordings
    }

    private fun getRecordingsFromDirectory(directory: File): List<AudioRecording> {
        val files = directory.listFiles { file -> file.extension == "wav" } ?: emptyArray()
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
                // Reset states in case of error
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

    private fun updateWavHeader(wavFile: File) {
        val fileLength = wavFile.length().toInt() - 44 // Total file size - header size
        val raf = RandomAccessFile(wavFile, "rw")

        // Update chunk size
        raf.seek(4)
        writeInt(raf, 36 + fileLength)

        // Update data chunk size
        raf.seek(40)
        writeInt(raf, fileLength)

        raf.close()
    }

    private fun writeWavHeader(outputStream: FileOutputStream, fileLength: Int) {
        // RIFF header
        outputStream.write("RIFF".toByteArray())
        writeInt(outputStream, 36 + fileLength)
        outputStream.write("WAVE".toByteArray())

        // fmt chunk
        outputStream.write("fmt ".toByteArray())
        writeInt(outputStream, 16)
        writeShort(outputStream, 1)
        writeShort(outputStream, 1)
        writeInt(outputStream, SAMPLE_RATE)
        writeInt(outputStream, SAMPLE_RATE * 2)
        writeShort(outputStream, 2)
        writeShort(outputStream, 16)

        // data chunk
        outputStream.write("data".toByteArray())
        writeInt(outputStream, fileLength)
    }

    private fun writeInt(outputStream: FileOutputStream, value: Int) {
        outputStream.write(value)
        outputStream.write(value shr 8)
        outputStream.write(value shr 16)
        outputStream.write(value shr 24)
    }

    private fun writeInt(raf: RandomAccessFile, value: Int) {
        raf.write(value)
        raf.write(value shr 8)
        raf.write(value shr 16)
        raf.write(value shr 24)
    }

    private fun writeShort(outputStream: FileOutputStream, value: Int) {
        outputStream.write(value)
        outputStream.write(value shr 8)
    }

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE = 2048
    }
}

