package com.theimpartialai.speechScribe.opusEncoding.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {
    fun createOutputFile(context: Context): Pair<File, FileOutputStream> {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "opus_recording_$timestamp.wav"
        val outputDir = getOutputDirectory(context)
        val file = File(outputDir, fileName)
        val outputStream = FileOutputStream(file)
        return Pair(file, outputStream)
    }

    fun getOutputDirectory(context: Context): File {
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
            ?: throw IllegalStateException("External storage is not available")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        return outputDir
    }
}