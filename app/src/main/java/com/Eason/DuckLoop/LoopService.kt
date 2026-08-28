package com.Eason.DuckLoop

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val CHANNEL_ID = "duckloop_channel"
const val NOTIFICATION_ID = 1

const val ACTION_START = "com.Eason.DuckLoop.ACTION_START"
const val ACTION_STOP = "com.Eason.DuckLoop.ACTION_STOP"
const val ACTION_UPDATE_STATUS = "com.Eason.DuckLoop.ACTION_UPDATE_STATUS"
const val EXTRA_STATUS_TEXT = "status_text"

const val EXTRA_MUSIC_URIS = "music_uris"
const val EXTRA_ANNOUNCE_URIS = "announce_uris"
const val EXTRA_INTERVAL = "interval_sec"
const val EXTRA_DUCK_LEVEL = "duck_level"

class LoopService : Service() {

    private val tag = "LoopService"

    private lateinit var musicPlayer: MusicPlayer
    private lateinit var announcePlayer: AnnouncePlayer
    private lateinit var audioDucker: AudioDucker

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var loopJob: Job? = null

    var isRunning = false
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "onCreate")

        createNotificationChannel()

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        musicPlayer = MusicPlayer(this)
        announcePlayer = AnnouncePlayer(this)
        audioDucker = AudioDucker(audioManager, musicPlayer)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_STOP -> handleStop()
        }

        return START_STICKY
    }

    private fun handleStart(intent: Intent) {
        if (isRunning) {
            Log.w(tag, "Already running, ignoring start command")
            return
        }

        val musicUriStrings = intent.getStringArrayListExtra(EXTRA_MUSIC_URIS)
        val announceUriStrings = intent.getStringArrayListExtra(EXTRA_ANNOUNCE_URIS)
        val intervalSec = intent.getFloatExtra(EXTRA_INTERVAL, 2.0f)
        val duckLevel = intent.getFloatExtra(EXTRA_DUCK_LEVEL, 0.2f)

        // Validate music URIs
        val musicUris = musicUriStrings?.mapNotNull { str ->
            try { Uri.parse(str) } catch (e: Exception) { null }
        } ?: emptyList()

        if (musicUris.isEmpty()) {
            Log.e(tag, "No music URIs provided")
            stopSelf()
            return
        }

        // Validate announce URIs
        var announceUris = announceUriStrings?.mapNotNull { str ->
            try { Uri.parse(str) } catch (e: Exception) { null }
        } ?: emptyList()

        // If no explicit announce URIs, fall back to recording file
        if (announceUris.isEmpty()) {
            val recordingFile = AudioRecorderManager(this).getRecordingFile()
            if (recordingFile.exists()) {
                announceUris = listOf(Uri.fromFile(recordingFile))
            }
        }

        if (announceUris.isEmpty()) {
            Log.e(tag, "No announce audio available, cannot start")
            broadcastStatus("播报音频不存在")
            stopSelf()
            return
        }

        startForeground(NOTIFICATION_ID, buildNotification("正在启动..."))

        loopJob = serviceScope.launch {
            isRunning = true

            try {
                // 1. Load background music playlist
                for (uri in musicUris) {
                    musicPlayer.addUri(uri)
                }
                musicPlayer.volume = 1.0f
                musicPlayer.load()
                musicPlayer.play()

                // 2. Load announce playlist
                announcePlayer.loadFromList(announceUris)

                // Update notification
                updateNotification("循环播报运行中")

                // 3. First interval before first announcement
                delay((intervalSec * 1000).toLong())

                var cycle = 1
                while (isRunning && isActive) {
                    // Announce phase
                    val announceText = "第${cycle}轮：播报中"
                    broadcastStatus(announceText)
                    updateNotification(announceText)

                    // Duck music volume + request audio focus
                    audioDucker.requestAudioFocus()
                    audioDucker.smoothVolumeTo(duckLevel, 300)

                    // Play all announce files in sequence
                    var allAnnouncesOk = true
                    for (i in 0 until announcePlayer.getCount()) {
                        if (!isRunning || !isActive) break
                        
                        // Seek to the current announce file and play
                        announcePlayer.seekTo(i)
                        val ok = announcePlayer.playAndWait()
                        if (!ok) {
                            Log.w(tag, "Announce playback had issues at index $i")
                            allAnnouncesOk = false
                        }
                    }
                    
                    if (!isRunning || !isActive) break

                    if (!allAnnouncesOk) {
                        Log.w(tag, "Some announce playbacks had issues, continuing cycle")
                    }

                    // Interval phase
                    val intervalText = "第${cycle}轮：间隔中"
                    broadcastStatus(intervalText)
                    updateNotification(intervalText)

                    // Restore music volume + release audio focus
                    audioDucker.smoothVolumeTo(1.0f, 300)
                    audioDucker.abandonAudioFocus()

                    // Wait for interval
                    delay((intervalSec * 1000).toLong())
                    if (!isRunning || !isActive) break

                    cycle++
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(tag, "Loop cancelled")
            } catch (e: Exception) {
                Log.e(tag, "Unexpected error in loop", e)
                broadcastStatus("发生错误: ${e.message}")
            } finally {
                cleanup()
            }
        }
    }

    private fun handleStop() {
        Log.d(tag, "handleStop")
        isRunning = false
        loopJob?.cancel()
        loopJob = null
    }

    private suspend fun cleanup() {
        Log.d(tag, "cleanup")
        isRunning = false
        audioDucker.cancelAnimation()
        audioDucker.abandonAudioFocus()
        announcePlayer.stop()
        musicPlayer.stop()
        broadcastStatus("已停止")
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.e(tag, "Error stopping foreground", e)
        }
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(tag, "onDestroy")
        isRunning = false
        loopJob?.cancel()
        loopJob = null
        audioDucker.cancelAnimation()
        audioDucker.abandonAudioFocus()
        announcePlayer.release()
        musicPlayer.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "DuckLoop 循环播报",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "循环播报器运行状态"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(content: String): Notification {
        val stopIntent = Intent(this, LoopService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DuckLoop 循环播报器")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_notification, "停止", stopPendingIntent)
            .build()
    }

    private fun updateNotification(content: String) {
        try {
            val notification = buildNotification(content)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(tag, "Error updating notification", e)
        }
    }

    private fun broadcastStatus(status: String) {
        val intent = Intent(ACTION_UPDATE_STATUS).apply {
            putExtra(EXTRA_STATUS_TEXT, status)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                sendBroadcast(intent)
            } else {
                @Suppress("DEPRECATION")
                sendBroadcast(intent)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error broadcasting status", e)
        }
    }

    companion object {
        fun start(
            context: Context,
            musicUris: List<Uri>,
            announceUris: List<Uri>,
            intervalSec: Float,
            duckLevel: Float
        ) {
            val intent = Intent(context, LoopService::class.java).apply {
                action = ACTION_START
                putStringArrayListExtra(EXTRA_MUSIC_URIS, ArrayList(musicUris.map { it.toString() }))
                if (announceUris.isNotEmpty()) {
                    putStringArrayListExtra(EXTRA_ANNOUNCE_URIS, ArrayList(announceUris.map { it.toString() }))
                }
                putExtra(EXTRA_INTERVAL, intervalSec)
                putExtra(EXTRA_DUCK_LEVEL, duckLevel)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                @Suppress("DEPRECATION")
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LoopService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}