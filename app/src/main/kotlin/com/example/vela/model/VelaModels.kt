package com.example.vela.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class PlatformType(val label: String, val badgeHex: Long) {
    YOUTUBE("YouTube", 0xFFFF3D57),
    TIKTOK("TikTok", 0xFF25F4EE),
    INSTAGRAM("Instagram", 0xFFE1306C),
    X_TWITTER("X / Twitter", 0xFF1DA1F2),
    SOUNDCLOUD("SoundCloud", 0xFFFF7700),
    REDDIT("Reddit", 0xFFFF4500),
    GENERIC("Web Media", 0xFF7C5CFF);

    companion object {
        fun detect(url: String): PlatformType {
            val lower = url.lowercase()
            return when {
                "youtube.com" in lower || "youtu.be" in lower -> YOUTUBE
                "tiktok.com" in lower || "vt.tiktok.com" in lower -> TIKTOK
                "instagram.com" in lower || "instagr.am" in lower -> INSTAGRAM
                "twitter.com" in lower || "x.com" in lower -> X_TWITTER
                "soundcloud.com" in lower -> SOUNDCLOUD
                "reddit.com" in lower || "v.redd.it" in lower -> REDDIT
                else -> GENERIC
            }
        }
    }
}

enum class DownloadMode(val label: String) {
    VIDEO("Video"),
    AUDIO("Audio"),
    CLIPPER("Clipper"),
    COMMAND("Command")
}

enum class VideoQuality(val label: String, val maxHeight: Int?) {
    BEST("Best", null),
    Q_1080P("1080p", 1080),
    Q_720P("720p", 720),
    Q_480P("480p", 480)
}

enum class VideoContainer(val ext: String) {
    MP4("mp4"),
    MKV("mkv"),
    WEBM("webm")
}

enum class AudioPreset(
    val label: String,
    val format: String,
    val subtitle: String
) {
    MP3_320("MP3 320k", "mp3", "Universal high quality"),
    M4A_AAC("M4A AAC", "m4a", "Compact Apple/Android"),
    FLAC_HIFI("FLAC Hi-Fi", "flac", "Lossless master"),
    PODCAST_VOICE("Podcast 64k", "mp3", "Mono speech optimized"),
    RINGTONE_30S("Ringtone 30s", "mp3", "First 30s fade-ready")
}

data class DownloadOptions(
    val mode: DownloadMode = DownloadMode.VIDEO,
    val videoQuality: VideoQuality = VideoQuality.Q_1080P,
    val videoContainer: VideoContainer = VideoContainer.MP4,
    val audioPreset: AudioPreset = AudioPreset.MP3_320,
    val clipStart: String = "00:00",
    val clipEnd: String = "00:30",
    val clipAudioOnly: Boolean = false,
    val embedThumbnail: Boolean = true,
    val embedSubtitles: Boolean = false,
    val sponsorBlock: Boolean = false,
    val useAria2c: Boolean = false,
    val noWatermark: Boolean = true,
    val customCommand: String = "-f \"bv*+ba/b\" --merge-output-format mp4"
)

data class MediaPreview(
    val url: String,
    val title: String,
    val uploader: String,
    val durationSec: Long,
    val thumbnailUrl: String?,
    val platform: PlatformType,
    val estimatedSizeMb: Double?
)

@Serializable
enum class QueueStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class QueueItem(
    val id: String,
    val url: String,
    val title: String,
    val uploader: String,
    val platform: PlatformType,
    val mode: DownloadMode,
    val status: QueueStatus = QueueStatus.QUEUED,
    val progress: Float = 0f,
    val etaSeconds: Long = 0L,
    val statusLine: String = "Waiting in queue...",
    val outputPath: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class VaultMediaType {
    VIDEO,
    AUDIO,
    CLIP
}

@Serializable
data class VaultItem(
    val id: String,
    val title: String,
    val uploader: String,
    val platform: PlatformType,
    val mediaType: VaultMediaType,
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val durationText: String,
    val thumbnailUrl: String? = null,
    val sourceUrl: String,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Serializable
data class CommandTemplate(
    val id: String,
    val name: String,
    val description: String,
    val args: String
)

@Serializable
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Obsidian Dark"),
    LIGHT("Daylight")
}

@Serializable
data class VelaSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val defaultUseAria2c: Boolean = false,
    val defaultSponsorBlock: Boolean = false,
    val defaultEmbedThumbnail: Boolean = true
)

fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return "--:--"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

