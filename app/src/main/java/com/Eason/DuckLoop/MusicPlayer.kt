package com.Eason.DuckLoop

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MusicPlayer(context: Context) {

    private val tag = "MusicPlayer"

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val playlist = mutableListOf<Uri>()
    private var currentIndex = 0

    init {
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.volume = 1.0f
    }

    var volume: Float
        get() = player.volume
        set(value) {
            player.volume = value.coerceIn(0f, 1f)
        }

    fun addUri(uri: Uri) {
        playlist.add(uri)
        val mediaItem = MediaItem.fromUri(uri)
        player.addMediaItem(mediaItem)
        Log.d(tag, "Added to playlist: $uri (total: ${playlist.size})")
    }

    fun removeUri(index: Int): Boolean {
        if (index < 0 || index >= playlist.size) return false
        playlist.removeAt(index)
        player.removeMediaItem(index)
        Log.d(tag, "Removed from playlist at $index (total: ${playlist.size})")
        return true
    }

    fun clearPlaylist() {
        playlist.clear()
        player.clearMediaItems()
        currentIndex = 0
        Log.d(tag, "Playlist cleared")
    }

    fun getPlaylist(): List<Uri> = playlist.toList()

    fun getPlaylistCount(): Int = playlist.size

    fun load() {
        if (playlist.isEmpty()) return
        currentIndex = 0
        player.seekTo(currentIndex, 0)
        player.prepare()
        Log.d(tag, "Loaded playlist with ${playlist.size} tracks")
    }

    fun play() {
        if (playlist.isEmpty()) return
        if (!player.isPlaying) {
            player.play()
        }
    }

    fun nextTrack() {
        if (playlist.isEmpty()) return
        currentIndex = (currentIndex + 1) % playlist.size
        player.seekTo(currentIndex, 0)
        Log.d(tag, "Switched to next track: $currentIndex")
    }

    fun previousTrack() {
        if (playlist.isEmpty()) return
        currentIndex = if (currentIndex > 0) currentIndex - 1 else playlist.size - 1
        player.seekTo(currentIndex, 0)
    }

    fun stop() {
        player.stop()
    }

    fun release() {
        player.release()
    }

    fun isPlaying(): Boolean = player.isPlaying
}