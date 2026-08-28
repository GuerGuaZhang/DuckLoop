package com.Eason.DuckLoop

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.Eason.DuckLoop.databinding.ActivityMainBinding
import com.google.android.material.slider.Slider
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"
    private lateinit var binding: ActivityMainBinding

    // Multi-file lists
    private val musicUris = mutableListOf<Uri>()
    private val announceUris = mutableListOf<Uri>()

    private var isRecording = false
    private var isServiceRunning = false

    // Managers
    private lateinit var audioRecorderManager: AudioRecorderManager
    private var previewPlayer: AnnouncePlayer? = null

    // Status receiver for service updates
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val statusText = intent.getStringExtra(EXTRA_STATUS_TEXT) ?: return
            runOnUiThread {
                binding.tvStatus.text = statusText
                if (statusText == "已停止") {
                    isServiceRunning = false
                    updateButtonStates()
                }
            }
        }
    }

    // SAF launchers for multiple files
    private val pickMusicLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        for (uri in uris) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.w(tag, "Could not take persistable permission", e)
            }
            musicUris.add(uri)
            Log.d(tag, "Added music: $uri")
        }
        updateMusicDisplay()
    }

    private val pickAnnounceLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        for (uri in uris) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.w(tag, "Could not take persistable permission", e)
            }
            announceUris.add(uri)
            Log.d(tag, "Added announce: $uri")
        }
        updateAnnounceDisplay()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            toggleRecording()
        } else {
            Toast.makeText(this, "需要录音权限", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioRecorderManager = AudioRecorderManager(this)

        setupClickListeners()
        setupSliders()
        registerStatusReceiver()

        // Check if recording file already exists
        if (audioRecorderManager.hasRecording()) {
            updateAnnounceDisplay()
        }
    }

    private fun updateMusicDisplay() {
        val count = musicUris.size
        binding.tvMusicCount.text = "${count}首"
        if (count == 0) {
            binding.tvMusicFileName.text = "未添加"
        } else {
            val names = musicUris.mapNotNull { getFileName(it) }
                .mapIndexed { i, name -> "${i + 1}. $name" }
            binding.tvMusicFileName.text = names.joinToString("\n")
        }
    }

    private fun updateAnnounceDisplay() {
        val count = announceUris.size
        val hasRecording = audioRecorderManager.hasRecording()
        binding.tvAnnounceCount.text = "${count + if (hasRecording) 1 else 0}个"
        val parts = announceUris.mapNotNull { getFileName(it) }
            .mapIndexed { i, name -> "${i + 1}. $name" }
            .toMutableList()
        if (hasRecording) {
            parts.add("录音：announcement.m4a")
        }
        binding.tvAnnounceFileName.text = if (parts.isEmpty()) "未添加" else parts.joinToString("\n")
    }

    private fun setupClickListeners() {
        // Background music selection (multiple)
        binding.btnSelectMusic.setOnClickListener {
            pickMusicLauncher.launch(arrayOf("audio/*", "audio/mpeg", "audio/wav", "audio/flac", "audio/ogg"))
        }

        // Clear music list
        binding.btnClearMusic.setOnClickListener {
            if (musicUris.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("清空背景音乐")
                    .setMessage("确定要清空所有背景音乐吗？")
                    .setPositiveButton("确定") { _, _ ->
                        musicUris.clear()
                        updateMusicDisplay()
                        Toast.makeText(this, "已清空背景音乐", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else {
                Toast.makeText(this, "没有背景音乐", Toast.LENGTH_SHORT).show()
            }
        }

        // Recording toggle (single button)
        binding.btnToggleRecord.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                checkRecordPermissionAndStart()
            }
        }

        // Import announce (multiple)
        binding.btnImportAnnounce.setOnClickListener {
            pickAnnounceLauncher.launch(arrayOf("audio/*"))
        }

        // Clear announce list
        binding.btnClearAnnounce.setOnClickListener {
            val hasRecording = audioRecorderManager.hasRecording()
            if (announceUris.isNotEmpty() || hasRecording) {
                AlertDialog.Builder(this)
                    .setTitle("清空播报音频")
                    .setMessage("确定要清空所有播报音频吗？" + if (hasRecording) "\n注意：录音文件也会被删除。" else "")
                    .setPositiveButton("确定") { _, _ ->
                        announceUris.clear()
                        if (hasRecording) {
                            audioRecorderManager.getRecordingFile().delete()
                        }
                        updateAnnounceDisplay()
                        Toast.makeText(this, "已清空播报音频", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else {
                Toast.makeText(this, "没有播报音频", Toast.LENGTH_SHORT).show()
            }
        }

        // Preview
        binding.btnPreviewAnnounce.setOnClickListener {
            previewAnnounce()
        }

        // Start / Stop service
        binding.btnStart.setOnClickListener {
            startLoop()
        }

        binding.btnStop.setOnClickListener {
            stopLoop()
        }
    }

    private fun setupSliders() {
        binding.sliderInterval.addOnChangeListener { _: Slider, value: Float, _: Boolean ->
            val seconds = 0.5f + value * 0.5f
            binding.tvIntervalValue.text = String.format("%.1fs", seconds)
        }

        binding.sliderDuck.addOnChangeListener { _: Slider, value: Float, _: Boolean ->
            val percent = (value * 5).toInt()
            binding.tvDuckValue.text = "$percent%"
        }
    }

    private fun registerStatusReceiver() {
        val filter = IntentFilter(ACTION_UPDATE_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(statusReceiver, filter)
        }
    }

    // ===== Recording =====

    private fun checkRecordPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                toggleRecording()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                AlertDialog.Builder(this)
                    .setTitle("需要录音权限")
                    .setMessage("循环播报需要录音权限来录制播报音频")
                    .setPositiveButton("确定") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        try {
            audioRecorderManager.startRecording()
            isRecording = true
            binding.btnToggleRecord.text = "停止录音"
            binding.btnToggleRecord.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#4CAF50")
            )
            binding.tvStatus.text = "录音中..."
            Toast.makeText(this, "开始录音", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(tag, "Failed to start recording", e)
            Toast.makeText(this, "录音初始化失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        val file = audioRecorderManager.stopRecording()
        isRecording = false
        binding.btnToggleRecord.text = "录音作为播报"
        binding.btnToggleRecord.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#D32F2F")
        )

        if (file != null && file.exists()) {
            binding.tvStatus.text = "录音完成"
            updateAnnounceDisplay()
            Toast.makeText(this, "录音完成", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "录音失败", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== Preview =====

    private fun previewAnnounce() {
        val allUris = announceUris.toMutableList()
        if (audioRecorderManager.hasRecording()) {
            allUris.add(Uri.fromFile(audioRecorderManager.getRecordingFile()))
        }

        if (allUris.isEmpty()) {
            Toast.makeText(this, "没有可试听的播报音频", Toast.LENGTH_SHORT).show()
            return
        }

        // If service is running, stop it first
        if (isServiceRunning) {
            stopLoop()
            binding.btnPreviewAnnounce.postDelayed({
                doPreview(allUris)
            }, 500)
        } else {
            doPreview(allUris)
        }
    }

    private fun doPreview(uris: List<Uri>) {
        if (previewPlayer == null) {
            previewPlayer = AnnouncePlayer(this)
        }

        previewPlayer?.loadFromList(uris)
        binding.tvStatus.text = "试听中..."
        Toast.makeText(this, "试听中...", Toast.LENGTH_SHORT).show()

        MainScope().launch {
            var allOk = true
            for (i in uris.indices) {
                val result = previewPlayer?.playAndWait() ?: false
                if (!result) allOk = false
            }
            runOnUiThread {
                binding.tvStatus.text = if (allOk) "试听完成" else "就绪"
            }
        }
    }

    // ===== Loop Control =====

    private fun startLoop() {
        if (musicUris.isEmpty()) {
            Toast.makeText(this, "请先添加背景音乐", Toast.LENGTH_SHORT).show()
            return
        }

        val allAnnounce = announceUris.toMutableList()
        if (audioRecorderManager.hasRecording()) {
            allAnnounce.add(Uri.fromFile(audioRecorderManager.getRecordingFile()))
        }

        if (allAnnounce.isEmpty()) {
            Toast.makeText(this, "请先录制或导入播报音频", Toast.LENGTH_SHORT).show()
            return
        }

        // Check notification permission on API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncherForNotification()
                return
            }
        }

        // Get parameters from Material Slider
        val intervalSec = 0.5f + binding.sliderInterval.value * 0.5f
        val duckPercent = (binding.sliderDuck.value * 5).toInt()
        val duckLevel = duckPercent / 100f

        // Ensure all URIs have persistable permission
        for (uri in musicUris) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.w(tag, "Could not take persistable permission for music", e)
            }
        }

        LoopService.start(
            this,
            musicUris.toList(),
            allAnnounce,
            intervalSec,
            duckLevel
        )

        isServiceRunning = true
        updateButtonStates()
        binding.tvStatus.text = "启动中..."
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startLoop()
        } else {
            Toast.makeText(this, "需要通知权限以显示后台状态", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermissionLauncherForNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startLoop()
        }
    }

    private fun stopLoop() {
        LoopService.stop(this)
        isServiceRunning = false
        updateButtonStates()
        binding.tvStatus.text = "正在停止..."
    }

    private fun updateButtonStates() {
        binding.btnStart.isEnabled = !isServiceRunning
        binding.btnStop.isEnabled = isServiceRunning
    }

    // ===== Utilities =====

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            Log.e(tag, "Error unregistering receiver", e)
        }
        previewPlayer?.release()
        previewPlayer = null
        super.onDestroy()
    }
}