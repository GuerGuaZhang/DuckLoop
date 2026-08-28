package com.Eason.DuckLoop

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.animation.ValueAnimator

class AudioDucker(
    private val audioManager: AudioManager,
    private val musicPlayer: MusicPlayer
) {

    private val tag = "AudioDucker"

    private var volumeAnimator: ValueAnimator? = null

    private val audioFocusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { /* Not used - we manage volume ourselves */ }
            .build()
    }

    /**
     * Smoothly transition volume to a target level.
     * @param target Target volume (0.0 ~ 1.0)
     * @param durationMs Animation duration in milliseconds
     */
    fun smoothVolumeTo(target: Float, durationMs: Long = 300) {
        cancelAnimation()

        val startVolume = musicPlayer.volume
        if (startVolume == target) return

        val animator = ValueAnimator.ofFloat(startVolume, target).apply {
            this.duration = durationMs
            addUpdateListener { animation ->
                musicPlayer.volume = animation.animatedValue as Float
            }
            start()
        }
        volumeAnimator = animator
        Log.d(tag, "Smooth volume: $startVolume -> $target over ${durationMs}ms")
    }

    /**
     * Request transient audio focus for announcement playback.
     */
    fun requestAudioFocus() {
        try {
            val result = audioManager.requestAudioFocus(audioFocusRequest)
            Log.d(tag, "AudioFocus request result: $result")
        } catch (e: Exception) {
            Log.e(tag, "Failed to request audio focus", e)
        }
    }

    /**
     * Abandon (release) audio focus after announcement playback.
     */
    fun abandonAudioFocus() {
        try {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
            Log.d(tag, "AudioFocus abandoned")
        } catch (e: Exception) {
            Log.e(tag, "Failed to abandon audio focus", e)
        }
    }

    /**
     * Cancel any ongoing volume animation immediately.
     */
    fun cancelAnimation() {
        volumeAnimator?.let {
            if (it.isRunning) {
                it.cancel()
            }
            it.removeAllUpdateListeners()
        }
        volumeAnimator = null
    }
}