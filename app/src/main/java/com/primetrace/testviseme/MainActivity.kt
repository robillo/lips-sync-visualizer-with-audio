package com.primetrace.testviseme

import android.content.res.Resources
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.primetrace.testviseme.BuildConfig
import com.primetrace.testviseme.R
import com.primetrace.testviseme.databinding.ActivityMainBinding
import java.util.*
import kotlin.math.abs
import kotlin.math.ln

private const val TAG = "MainActivity"
private const val UPDATE_INTERVAL_MS = 50L
private const val AVERAGE_WINDOW = 5

/**
 * Main activity that demonstrates real-time lip-sync animation based on audio input.
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var mediaPlayer: MediaPlayer? = null
    private var visualizer: Visualizer? = null
    private var isPlaying = false
    private var lastUpdateTime = 0L
    
    // Circular buffer for smoothing amplitude values
    private val amplitudeQueue = ArrayDeque<Double>().apply { 
        repeat(AVERAGE_WINDOW) { add(0.0) } 
    }
    
    // Mapping of mouth states to their corresponding drawable resources
    private val mouthDrawables = listOf(
        R.drawable.bmp,          // 0: Closed
        R.drawable.fv,          // 1: Slightly open
        R.drawable.th,          // 2: Slightly open
        R.drawable.r,          // 3: Slightly open
        R.drawable.cdgknstxyz,         // 4: Mostly open
        R.drawable.l,          // 5: Slightly open
        R.drawable.ae,         // 6: Half open
        R.drawable.qw,         // 7: Mostly open
        R.drawable.o         // 8: Fully open
    )
    
    // Visualizer data capture listener
    private val visualizerListener = object : Visualizer.OnDataCaptureListener {
        override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) {}
        override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {}
    }
    
    // Visualizer data capture for FFT
    private val fftListener = object : Visualizer.OnDataCaptureListener {
        override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {
            // Process FFT data to get amplitude
            val amplitude = calculateAmplitudeFromFFT(fft)
            updateMouthState(amplitude)
        }
        
        override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) {}
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Show debug info in debug builds
        binding.debugLayout.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
        
        // Set up play button
        binding.playButton.setOnClickListener {
            if (isPlaying) {
                stopPlayback()
            } else {
                startPlayback()
            }
        }
        
        // Initialize MediaPlayer
        setupMediaPlayer()
    }
    
    /**
     * Sets up the MediaPlayer with the audio file
     */
    private fun setupMediaPlayer() {
        try {
            // First, check if the raw resource exists
            val resourceId = resources.getIdentifier("song", "raw", packageName)
            if (resourceId == 0) {
                throw Resources.NotFoundException("song.mp3 not found in raw resources")
            }
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                // Load the audio file from raw resources
                val afd = resources.openRawResourceFd(resourceId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                
                setOnPreparedListener { mp ->
                    // Set up visualizer when media player is prepared
                    try {
                        setupVisualizer(mp.audioSessionId)
                        binding.playButton.isEnabled = true
                        binding.playButton.text = "Play"
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting up visualizer: ${e.message}")
                        showError("Audio visualization not available")
                        binding.playButton.isEnabled = false
                    }
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    showError("Error playing audio")
                    this@MainActivity.isPlaying = false
                    binding.playButton.text = "Play"
                    binding.playButton.isEnabled = false
                    true
                }
                
                setOnCompletionListener {
                    // Reset UI when playback completes
                    this@MainActivity.isPlaying = false
                    binding.playButton.text = "Play"
                    // Reset mouth to closed position
                    runOnUiThread { binding.mouthImage.setImageResource(mouthDrawables[0]) }
                }
                
                prepareAsync()
            }
        } catch (e: Resources.NotFoundException) {
            val errorMsg = "Audio file not found. Please add 'song.mp3' to res/raw/ folder."
            Log.e(TAG, errorMsg)
            showError(errorMsg)
            binding.playButton.isEnabled = false
        } catch (e: Exception) {
            val errorMsg = "Error initializing audio player: ${e.message}"
            Log.e(TAG, errorMsg, e)
            showError("Error initializing audio player")
            binding.playButton.isEnabled = false
        }
    }
    
    /**
     * Sets up the Visualizer to capture audio data
     */
    private fun setupVisualizer(audioSessionId: Int) {
        try {
            // Release any existing visualizer
            releaseVisualizer()
            
            try {
                // Create visualizer with the audio session ID
                visualizer = Visualizer(audioSessionId)
                
                // Configure the visualizer
                visualizer?.apply {
                    try {
                        // Log Visualizer capabilities for debugging
                        Log.d(TAG, "Initializing Visualizer with session ID: $audioSessionId")
                        
                        // Try to get capture size range (may throw exception on some devices)
                        try {
                            val captureRange = Visualizer.getCaptureSizeRange()
                            Log.d(TAG, "Capture size range: ${captureRange[0]} to ${captureRange[1]}")
                            Log.d(TAG, "Max capture rate: ${Visualizer.getMaxCaptureRate()}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not get Visualizer capabilities: ${e.message}")
                        }
                        
                        // Use default capture size (don't try to set it explicitly)
                        measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS
                        
                        // Set data capture listener with a reasonable capture rate
                        val captureRate = Visualizer.getMaxCaptureRate() / 2
                        setDataCaptureListener(
                            fftListener,
                            captureRate,
                            false,  // No PCM data
                            true    // FFT data
                        )
                        
                        // Enable the visualizer
                        enabled = true
                        Log.d(TAG, "Visualizer setup complete")
                        return // Successfully set up visualizer
                    } catch (e: Exception) {
                        val errorMsg = "Error configuring Visualizer: ${e.javaClass.simpleName} - ${e.message}"
                        Log.e(TAG, errorMsg, e)
                        release()
                        throw IllegalStateException(errorMsg, e)
                    }
                } ?: throw IllegalStateException("Failed to create Visualizer instance")
                
            } catch (e: Exception) {
                Log.w(TAG, "Visualizer not available, falling back to timer-based animation")
                // Fall back to timer-based animation
                setupFallbackVisualization()
            }
            
        } catch (e: Exception) {
            val errorMsg = "Error initializing Visualizer: ${e.message}"
            Log.e(TAG, errorMsg, e)
            showError("Visualizer not available: ${e.message}")
            setupFallbackVisualization()
        }
    }
    
    /**
     * Sets up a fallback visualization using a timer when Visualizer is not available
     */
    private fun setupFallbackVisualization() {
        // This is a simple fallback that just cycles through mouth states
        // In a real app, you might want to implement a more sophisticated fallback
        android.os.Handler().postDelayed(object : Runnable {
            override fun run() {
                if (isPlaying) {
                    // Generate a fake amplitude value that changes over time
                    val time = System.currentTimeMillis() / 100.0
                    val fakeAmplitude = (Math.sin(time) + 1) / 2.0 // 0.0 to 1.0
                    updateMouthState(fakeAmplitude)
                    android.os.Handler().postDelayed(this, 50) // 20 FPS
                }
            }
        }, 50)
    }
    
    /**
     * Releases the visualizer resources
     */
    private fun releaseVisualizer() {
        try {
            visualizer?.let { viz ->
                viz.enabled = false
                viz.release()
            }
            visualizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing Visualizer: ${e.message}")
        }
    }
    
    /**
     * Starts audio playback and visualization
     */
    private fun startPlayback() {
        mediaPlayer?.let { mp ->
            if (!mp.isPlaying) {
                try {
                    mp.start()
                    isPlaying = true
                    binding.playButton.text = "Stop"
                    startVisualization()
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting playback: ${e.message}")
                    showError("Error playing audio")
                }
            }
        } ?: run {
            setupMediaPlayer()
        }
    }
    
    /**
     * Stops audio playback and visualization
     */
    private fun stopPlayback() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                mp.seekTo(0)  // Rewind to start
            }
            isPlaying = false
            binding.playButton.text = "Play"
        }
    }
    
    /**
     * Starts the visualization of the audio
     */
    private fun startVisualization() {
        // The visualization is handled by the Visualizer's data capture listener
        // which was set up in setupVisualizer()
    }
    
    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Calculates the amplitude from FFT data
     */
    private fun calculateAmplitudeFromFFT(fft: ByteArray): Double {
        if (fft.isEmpty()) return 0.0
        
        // Calculate the magnitude of the FFT
        val magnitude = fft.take(128).fold(0.0) { acc, byte ->
            acc + abs(byte.toDouble() / 128.0)
        } / 128.0
        
        // Convert to decibels (log scale)
        val db = if (magnitude > 0) 20 * ln(magnitude) else -60.0
        
        // Normalize to 0-1 range (assuming -60 to 0 dB range)
        return ((db + 60) / 60.0).coerceIn(0.0, 1.0)
    }
    
    /**
     * Updates the mouth state based on the current audio amplitude.
     * @param amplitude The current audio amplitude (0.0 to 1.0)
     */
    private fun updateMouthState(amplitude: Double) {
        // Update the moving average for smoother transitions
        amplitudeQueue.removeFirst()
        amplitudeQueue.addLast(amplitude)
        val smoothedAmplitude = amplitudeQueue.average()
        
        // Determine the mouth state based on amplitude
        val state = when {
            smoothedAmplitude < 0.01 -> 0  // Closed
            smoothedAmplitude < 0.03 -> 1  // Slightly open
            smoothedAmplitude < 0.05 -> 2  // Half open
            smoothedAmplitude < 0.07 -> 3  // Half open
            smoothedAmplitude < 0.09 -> 4  // Half open
            smoothedAmplitude < 0.12 -> 5  // Mostly open
            smoothedAmplitude < 0.14 -> 6  // Mostly open
            smoothedAmplitude < 0.16 -> 7  // Mostly open
            else -> 4                     // Fully open
        }
        
        // Update the UI on the main thread
        runOnUiThread {
            try {
                // Update the mouth image
                binding.mouthImage.setImageResource(mouthDrawables[state])
                
                // Add a subtle scale animation
                val scale = 1f + (amplitude * 0.2f).toFloat()
                binding.mouthImage.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(50)
                    .start()
                
                // Update debug info if visible
                if (binding.debugLayout.visibility == View.VISIBLE) {
                    binding.amplitudeText.text = String.format("Amplitude: %.2f", smoothedAmplitude)
                    binding.stateText.text = "State: $state"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating UI: ${e.message}")
            }
        }
    }
    
    /**
     * Releases all resources
     */
    private fun releaseResources() {
        try {
            // Release the visualizer
            visualizer?.let { viz ->
                viz.enabled = false
                viz.release()
                visualizer = null
            }
            
            // Release the media player
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources: ${e.message}")
        }
    }
    
    override fun onResume() {
        super.onResume()
        // No need to request permission anymore as we're using a local file
    }
    
    override fun onPause() {
        super.onPause()
        stopPlayback()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        releaseResources()
    }
}