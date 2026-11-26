package com.example.vela.data

import android.content.Context
import com.example.vela.model.CommandTemplate
import com.example.vela.model.VaultItem
import com.example.vela.model.VelaSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class VelaRepository(context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val dataDir = File(context.filesDir, "vela_store").apply { mkdirs() }
    private val vaultFile = File(dataDir, "vault.json")
    private val templatesFile = File(dataDir, "templates.json")
    private val settingsFile = File(dataDir, "settings.json")

    fun loadVault(): List<VaultItem> {
        if (!vaultFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<VaultItem>>(vaultFile.readText())
                .sortedByDescending { it.downloadedAt }
        }.getOrDefault(emptyList())
    }

    fun saveVault(items: List<VaultItem>) {
        runCatching {
            vaultFile.writeText(json.encodeToString(items))
        }
    }

    fun loadTemplates(): List<CommandTemplate> {
        if (!templatesFile.exists()) {
            val defaults = defaultTemplates()
            saveTemplates(defaults)
            return defaults
        }
        return runCatching {
            json.decodeFromString<List<CommandTemplate>>(templatesFile.readText())
        }.getOrElse { defaultTemplates() }
    }

    fun saveTemplates(templates: List<CommandTemplate>) {
        runCatching {
            templatesFile.writeText(json.encodeToString(templates))
        }
    }

    fun loadSettings(): VelaSettings {
        if (!settingsFile.exists()) return VelaSettings()
        return runCatching {
            json.decodeFromString<VelaSettings>(settingsFile.readText())
        }.getOrDefault(VelaSettings())
    }

    fun saveSettings(settings: VelaSettings) {
        runCatching {
            settingsFile.writeText(json.encodeToString(settings))
        }
    }

    private fun defaultTemplates(): List<CommandTemplate> = listOf(
        CommandTemplate(
            id = "tpl_tiktok_clean",
            name = "TikTok Clean HD",
            description = "Best MP4 without watermark metadata",
            args = "-f \"bv*+ba/b\" --merge-output-format mp4 --add-metadata"
        ),
        CommandTemplate(
            id = "tpl_yt_1080",
            name = "YouTube 1080p MP4",
            description = "H264 + AAC compatible everywhere",
            args = "-f \"bv*[height<=1080][ext=mp4]+ba[ext=m4a]/b[height<=1080]\" --merge-output-format mp4"
        ),
        CommandTemplate(
            id = "tpl_audio_art",
            name = "MP3 + Cover Art",
            description = "Extract audio with embedded thumbnail",
            args = "-x --audio-format mp3 --audio-quality 0 --embed-thumbnail --add-metadata"
        ),
        CommandTemplate(
            id = "tpl_subs_only",
            name = "Subtitles SRT",
            description = "Download auto/manual subtitles without video",
            args = "--skip-download --write-subs --write-auto-subs --sub-format srt"
        )
    )
}

