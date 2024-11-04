package com.theimpartialai.speechScribe.repository

import android.content.Context
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AudioRecordingImpl(context: Context) : AudioRecordingInterface {

    val TAG = "AudioRecordingImpl"
    private val appContext = context.applicationContext
    private var mediaRecorder: MediaRecorder? = null
    private val audioPlayer: AudioPlayer = AudioPlayerImpl(appContext)
    private var currentOutputFile: File? = null
    private var recordingJob: Job? = null
    private var isRecording = false
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override suspend fun startRecording(amplitudeListener: AmplitudeListener) {
        withContext(Dispatchers.IO) {
            try {
                val file = FileUtils.createOutputFile(appContext).first
                currentOutputFile = file
                mediaRecorder = setupMediaRecorder(file)
                isRecording = true

                recordingJob = ioScope.launch {
                    while (isActive) {
                        mediaRecorder?.let { recorder ->
                            readAmplitude(recorder, amplitudeListener)
                        }
                        delay(100)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
            }
        }
    }

    override suspend fun pauseRecording() {
        withContext(Dispatchers.IO) {
            mediaRecorder?.pause()
            isRecording = false
            recordingJob?.cancelAndJoin()
        }
    }

    override suspend fun resumeRecording(amplitudeListener: AmplitudeListener) {
        withContext(Dispatchers.IO) {
            try {
                mediaRecorder?.resume()
                isRecording = true

                recordingJob = ioScope.launch {
                    while (isActive && isRecording) {
                        mediaRecorder?.let { recorder ->
                            val amplitude = recorder.maxAmplitude
                            amplitudeListener.onAmplitude(amplitude)
                            delay(50)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming recording", e)
                isRecording = false
            }
        }
    }

    override suspend fun stopRecording() {
        withContext(Dispatchers.IO) {
            try {
                isRecording = false
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                recordingJob?.cancelAndJoin()

            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
            }
        }
    }

    override suspend fun discardRecording() {
        withContext(Dispatchers.IO) {
            try {
                isRecording = false
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                currentOutputFile?.delete()
                currentOutputFile = null
                recordingJob?.cancelAndJoin()
            } catch (e: Exception) {
                Log.e(TAG, "Error discarding recording", e)
            }
        }
    }


    override suspend fun loadRecordings(): List<AudioRecording> {
        val outputDir = getOutputDirectory(appContext)
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
                duration = getAudioDuration(file),
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

    private fun setupMediaRecorder(outputFile: File): MediaRecorder {
        return MediaRecorder(appContext).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
    }

    private fun readAmplitude(recorder: MediaRecorder, listener: AmplitudeListener) {
        try {
            listener.onAmplitude(recorder.maxAmplitude)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading amplitude", e)
        }
    }
}

