package com.Eason.DuckLoop

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

class AnnouncePlayer(context: Context) {

    private val tag = "AnnouncePlayer"

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val announceList = mutableListOf<Uri>()
    private var currentAnnounceIndex = 0
    private var listener: Player.Listener? = null

    fun addUri(uri: Uri) {
        announceList.add(uri)
        val mediaItem = MediaItem.fromUri(uri)
        player.addMediaItem(mediaItem)
        Log.d(tag, "Added announce: $uri (total: ${announceList.size})")
    }

    fun removeUri(index: Int): Boolean {
        if (index < 0 || index >= announceList.size) return false
        announceList.removeAt(index)
        player.removeMediaItem(index)
        Log.d(tag, "Removed announce at $index (total: ${announceList.size})")
        return true
    }

    fun clearList() {
        announceList.clear()
        player.clearMediaItems()
        currentAnnounceIndex = 0
        Log.d(tag, "Announce list cleared")
    }

    fun getList(): List<Uri> = announceList.toList()

    fun getCount(): Int = announceList.size

    fun hasContent(): Boolean = announceList.isNotEmpty()

    /**
     * Seek to a specific announce file index.
     * @param index The index of the announce file to seek to.
     */
    fun seekTo(index: Int) {
        if (index < 0 || index >= announceList.size) {
            Log.w(tag, "Invalid index: $index, size: ${announceList.size}")
            return
        }
        currentAnnounceIndex = index
        player.seekTo(index, 0)
        player.prepare()
        Log.d(tag, "Seeked to announce at index $index")
    }

    fun load(uri: Uri) {
        clearList()
        addUri(uri)
        player.prepare()
    }

    fun load(file: File) {
        load(Uri.fromFile(file))
    }

    fun loadFromList(uris: List<Uri>) {
        clearList()
        for (uri in uris) {
            addUri(uri)
        }
        currentAnnounceIndex = 0
        player.seekTo(0, 0)
        player.prepare()
        Log.d(tag, "Loaded ${uris.size} announce files")
    }

    /**
     * Play the loaded audio and suspend until playback completes.
     * Returns true if playback completed normally, false if error or cancelled.
     */
    suspend fun playAndWait(): Boolean = coroutineScope {
        if (!isActive) return@coroutineScope false

        Log.d(tag, "playAndWait started")

        // Calculate timeout: duration + 3s buffer, minimum 4s total
        val durationMs = if (player.duration > 0) player.duration else 1000L
        val timeoutMs = durationMs + 3000L

        val result = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Boolean> { continuation ->
                val playListener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_ENDED -> {
                                Log.d(tag, "Playback ended naturally")
                                if (continuation.isActive) {
                                    continuation.resume(true)
                                }
                            }
                            Player.STATE_READY -> {
                                // Start playing when ready
                                player.play()
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(tag, "Player error during playback", error)
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                }

                listener = playListener
                player.addListener(playListener)

                continuation.invokeOnCancellation {
                    Log.d(tag, "playAndWait cancelled")
                    player.removeListener(playListener)
                    listener = null
                    player.stop()
                }

                // If already ended, seek to beginning and prepare for replay
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0, 0)
                    player.prepare()
                } else if (player.playbackState == Player.STATE_READY) {
                    player.play()
                } else if (player.playbackState == Player.STATE_IDLE) {
                    Log.w(tag, "Player is idle, nothing to play")
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }
        } ?: false

        // Cleanup listener after completion
        if (listener != null) {
            player.removeListener(listener!!)
            listener = null
        }

        Log.d(tag, "playAndWait completed: $result")
        result
    }

    fun stop() {
        try {
            player.stop()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping player", e)
        }
    }

    fun release() {
        try {
            if (listener != null) {
                player.removeListener(listener!!)
                listener = null
            }
            player.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing player", e)
        }
    }
}