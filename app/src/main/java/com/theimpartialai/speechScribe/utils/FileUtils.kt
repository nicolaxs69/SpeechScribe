package com.theimpartialai.speechScribe.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {
    private const val FILENAME = "wav"

    private fun createS3Filename(
        category: String = "speech"
    ): String {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        val sanitizedCategory = category.replace(Regex("[^a-zA-Z0-9-]"), "-")
        return "recording-$sanitizedCategory-$timestamp.$FILENAME"
    }

    fun createOutputFile(context: Context): Pair<File, FileOutputStream> {
        val s3Key = createS3Filename()
        val localFilename = s3Key.substringAfterLast("/")
        val outputDir = getOutputDirectory(context)
        val file = File(outputDir, localFilename)
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