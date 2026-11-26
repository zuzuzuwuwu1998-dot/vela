package com.example.vela

import android.app.Application
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EngineStatus {
    data object Initializing : EngineStatus
    data class Ready(val ytDlpVersion: String?) : EngineStatus
    data class Error(val message: String) : EngineStatus
}

class VelaApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _engineStatus = MutableStateFlow<EngineStatus>(EngineStatus.Initializing)
    val engineStatus: StateFlow<EngineStatus> = _engineStatus.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        initEngines()
    }

    fun initEngines() {
        _engineStatus.value = EngineStatus.Initializing
        appScope.launch {
            try {
                YoutubeDL.getInstance().init(this@VelaApplication)
                FFmpeg.getInstance().init(this@VelaApplication)
                Aria2c.getInstance().init(this@VelaApplication)
                val version = YoutubeDL.getInstance().version(this@VelaApplication)
                _engineStatus.value = EngineStatus.Ready(version)
                runCatching {
                    YoutubeDL.getInstance().updateYoutubeDL(
                        this@VelaApplication,
                        YoutubeDL.UpdateChannel.STABLE
                    )
                    val updatedVersion = YoutubeDL.getInstance().version(this@VelaApplication)
                    _engineStatus.value = EngineStatus.Ready(updatedVersion)
                }
            } catch (e: Throwable) {
                _engineStatus.value = EngineStatus.Error(e.message ?: "Engine initialization failed")
            }
        }
    }

    suspend fun updateYtDlp(): Result<String> = runCatching {
        val status = YoutubeDL.getInstance().updateYoutubeDL(
            this@VelaApplication,
            YoutubeDL.UpdateChannel.STABLE
        )
        val version = YoutubeDL.getInstance().version(this@VelaApplication)
        _engineStatus.value = EngineStatus.Ready(version)
        "${status?.name ?: "DONE"} (${version ?: "latest"})"
    }
}

