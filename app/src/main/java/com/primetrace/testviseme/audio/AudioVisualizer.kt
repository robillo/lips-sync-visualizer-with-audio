package com.primetrace.testviseme.audio

import android.media.MediaRecorder
import android.util.Log
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

class AudioVisualizer {
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    var onAmplitudeUpdate: ((Double) -> Unit)? = null

    fun start() {
        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile("/dev/null")
                prepare()
                start()
            }
            isRecording = true
            startAmplitudeCheck()
        } catch (e: Exception) {
            Log.e("AudioVisualizer", "Error starting media recorder", e)
        }
    }

    fun stop() {
        try {
            isRecording = false
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            Log.e("AudioVisualizer", "Error stopping media recorder", e)
        }
    }

    private fun startAmplitudeCheck() {
        Thread {
            val buffer = ShortArray(1024)
            while (isRecording) {
                try {
                    val amplitude = getAmplitude()
                    val db = 20 * log10(amplitude.toDouble())
                    // Normalize to 0-1 range (adjust these values based on testing)
                    val normalized = (db + 60) / 60.0
                    onAmplitudeUpdate?.invoke(normalized.coerceIn(0.0, 1.0))
                    Thread.sleep(50) // Update 20 times per second
                } catch (e: Exception) {
                    Log.e("AudioVisualizer", "Error in amplitude check", e)
                }
            }
        }.start()
    }

    private fun getAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }
}

// Simple moving average to smooth out amplitude values
class MovingAverage(private val size: Int) {
    private val queue = ArrayDeque<Double>(size)
    
    fun add(value: Double): Double {
        queue.addLast(value)
        if (queue.size > size) {
            queue.removeFirst()
        }
        return queue.average()
    }
}
