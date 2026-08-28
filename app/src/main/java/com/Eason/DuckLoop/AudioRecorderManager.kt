package com.Eason.DuckLoop

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File

class AudioRecorderManager(private val context: Context) {

    private val tag = "AudioRecorderManager"

    private var recorder: MediaRecorder? = null

    private val outputDir = File(context.filesDir, "recordings").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val outputFile = File(outputDir, "announcement.m4a")

    /**
     * Start recording audio.
     * @return The output file that will contain the recording.
     * @throws IllegalStateException if recording is already in progress.
     */
    @Throws(IllegalStateException::class)
    fun startRecording(): File {
        if (recorder != null) {
            throw IllegalStateException("Recording already in progress")
        }

        try {
            // Delete previous recording file
            if (outputFile.exists()) {
                outputFile.delete()
            }

            @Suppress("DEPRECATION")
            val newRecorder = MediaRecorder()
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setOutputFile(outputFile.absolutePath)

            newRecorder.prepare()
            newRecorder.start()

            recorder = newRecorder
            Log.d(tag, "Recording started: ${outputFile.absolutePath}")
            return outputFile
        } catch (e: Exception) {
            Log.e(tag, "Failed to start recording", e)
            recorder = null
            throw e
        }
    }

    /**
     * Stop recording and release the recorder.
     * @return The recorded file, or null if no recording was in progress.
     */
    fun stopRecording(): File? {
        val currentRecorder = recorder ?: return null

        return try {
            currentRecorder.apply {
                stop()
                release()
            }
            recorder = null
            Log.d(tag, "Recording stopped: ${outputFile.absolutePath}")
            outputFile
        } catch (e: Exception) {
            Log.e(tag, "Error stopping recording", e)
            recorder = null
            null
        }
    }

    /**
     * Get the current recording file (may or may not exist).
     */
    fun getRecordingFile(): File = outputFile

    /**
     * Check if the recording file exists.
     */
    fun hasRecording(): Boolean = outputFile.exists() && outputFile.length() > 0
}