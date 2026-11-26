package com.example.vela.engine

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.example.vela.model.AudioPreset
import com.example.vela.model.DownloadMode
import com.example.vela.model.DownloadOptions
import com.example.vela.model.MediaPreview
import com.example.vela.model.PlatformType
import com.example.vela.model.VideoQuality
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class DownloadResult(
    val outputFile: File?,
    val title: String,
    val uploader: String,
    val durationSec: Long,
    val thumbnailUrl: String?
)

class VelaDownloadEngine(private val context: Context) {

    fun getOutputDirectory(mode: DownloadMode): File {
        val base = when (mode) {
            DownloadMode.AUDIO -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        }
        val velaDir = File(base, "Vela")
        if (!velaDir.exists() && !velaDir.mkdirs()) {
            return File(context.getExternalFilesDir(null) ?: context.filesDir, "Vela").apply { mkdirs() }
        }
        return velaDir
    }

    suspend fun fetchMediaInfo(url: String): Result<MediaPreview> = withContext(Dispatchers.IO) {
        runCatching {
            val request = YoutubeDLRequest(url).apply {
                addOption("--no-playlist")
                addOption("--socket-timeout", "15")
            }
            val info = YoutubeDL.getInstance().getInfo(request)
            val approxBytes = info.formats
                ?.mapNotNull { it.fileSizeApproximate.takeIf { s -> s > 0 } ?: it.fileSize.takeIf { s -> s > 0 } }
                ?.maxOrNull()
            MediaPreview(
                url = url,
                title = info.title ?: "Untitled Media",
                uploader = info.uploader ?: info.uploaderId ?: "Unknown Creator",
                durationSec = info.duration.toLong(),
                thumbnailUrl = info.thumbnail,
                platform = PlatformType.detect(url),
                estimatedSizeMb = approxBytes?.let { it / (1024.0 * 1024.0) }
            )
        }
    }

    suspend fun executeDownload(
        taskId: String,
        url: String,
        options: DownloadOptions,
        onProgress: (progress: Float, etaSeconds: Long, line: String) -> Unit
    ): Result<DownloadResult> = withContext(Dispatchers.IO) {
        runCatching {
            val outDir = getOutputDirectory(options.mode)
            val markerPrefix = "VELA_${taskId.take(8)}_"
            val request = buildRequest(url, options, outDir, markerPrefix)

            val response = YoutubeDL.getInstance().execute(request, taskId) { progress, etaInSeconds, line ->
                onProgress(progress.coerceIn(0f, 100f), etaInSeconds, line ?: "")
            }

            val outputFile = findDownloadedFile(outDir, markerPrefix)
                ?: parseFilePathFromOutput(response.out)
                ?: outDir.listFiles()?.maxByOrNull { it.lastModified() }

            if (outputFile != null && outputFile.exists()) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFile.absolutePath),
                    null,
                    null
                )
            }

            val cleanName = outputFile?.nameWithoutExtension
                ?.removePrefix(markerPrefix)
                ?.ifBlank { null }
                ?: "Vela Download"

            DownloadResult(
                outputFile = outputFile,
                title = cleanName,
                uploader = PlatformType.detect(url).label,
                durationSec = 0L,
                thumbnailUrl = null
            )
        }
    }

    fun cancelDownload(taskId: String) {
        runCatching {
            YoutubeDL.getInstance().destroyProcessById(taskId)
        }
    }

    private fun buildRequest(
        url: String,
        options: DownloadOptions,
        outDir: File,
        markerPrefix: String
    ): YoutubeDLRequest {
        val request = YoutubeDLRequest(url)
        val template = File(outDir, "${markerPrefix}%(title).80s.%(ext)s").absolutePath
        request.addOption("-o", template)
        request.addOption("--no-mtime")
        request.addOption("--no-playlist")
        request.addOption("--no-check-certificates")
        request.addOption("--geo-bypass")

        if (options.useAria2c) {
            request.addOption("--external-downloader", "aria2c")
            request.addOption("--external-downloader-args", "aria2c:-x 8 -s 8 -k 1M")
        }

        if (options.sponsorBlock && options.mode != DownloadMode.COMMAND) {
            request.addOption("--sponsorblock-remove", "sponsor,intro,outro")
        }

        when (options.mode) {
            DownloadMode.VIDEO -> {
                val formatExpr = when (options.videoQuality) {
                    VideoQuality.BEST -> "bv*+ba/b"
                    VideoQuality.Q_1080P -> "bv*[height<=1080]+ba/b[height<=1080]/b"
                    VideoQuality.Q_720P -> "bv*[height<=720]+ba/b[height<=720]/b"
                    VideoQuality.Q_480P -> "bv*[height<=480]+ba/b[height<=480]/b"
                }
                request.addOption("-f", formatExpr)
                request.addOption("--merge-output-format", options.videoContainer.ext)
                if (options.embedThumbnail) {
                    request.addOption("--embed-thumbnail")
                }
                if (options.embedSubtitles) {
                    request.addOption("--write-auto-subs")
                    request.addOption("--embed-subs")
                }
                request.addOption("--add-metadata")
            }

            DownloadMode.AUDIO -> {
                request.addOption("-x")
                request.addOption("--audio-format", options.audioPreset.format)
                when (options.audioPreset) {
                    AudioPreset.MP3_320 -> request.addOption("--audio-quality", "0")
                    AudioPreset.M4A_AAC -> request.addOption("--audio-quality", "0")
                    AudioPreset.FLAC_HIFI -> request.addOption("--audio-quality", "0")
                    AudioPreset.PODCAST_VOICE -> {
                        request.addOption("--audio-quality", "64K")
                        request.addOption("--postprocessor-args", "ffmpeg:-ac 1")
                    }
                    AudioPreset.RINGTONE_30S -> {
                        request.addOption("--audio-quality", "192K")
                        request.addOption("--download-sections", "*00:00-00:30")
                    }
                }
                if (options.embedThumbnail) {
                    request.addOption("--embed-thumbnail")
                }
                request.addOption("--add-metadata")
            }

            DownloadMode.CLIPPER -> {
                val start = options.clipStart.trim().ifBlank { "00:00" }
                val end = options.clipEnd.trim().ifBlank { "00:30" }
                request.addOption("--download-sections", "*$start-$end")
                request.addOption("--force-keyframes-at-cuts")
                if (options.clipAudioOnly) {
                    request.addOption("-x")
                    request.addOption("--audio-format", "mp3")
                } else {
                    request.addOption("-f", "bv*[height<=1080]+ba/b[height<=1080]/b")
                    request.addOption("--merge-output-format", "mp4")
                }
                request.addOption("--add-metadata")
            }

            DownloadMode.COMMAND -> {
                val tokens = splitCommandArgs(options.customCommand)
                var i = 0
                while (i < tokens.size) {
                    val token = tokens[i]
                    if (token.startsWith("-") && i + 1 < tokens.size && !tokens[i + 1].startsWith("-")) {
                        request.addOption(token, tokens[i + 1])
                        i += 2
                    } else {
                        request.addOption(token)
                        i += 1
                    }
                }
            }
        }
        return request
    }

    private fun findDownloadedFile(dir: File, markerPrefix: String): File? {
        return dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(markerPrefix) }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun parseFilePathFromOutput(output: String?): File? {
        if (output.isNullOrBlank()) return null
        val regex = Regex("""Destination:\s+(.+)""")
        val match = regex.findAll(output).lastOrNull()?.groupValues?.getOrNull(1)?.trim()
        return match?.let { File(it) }?.takeIf { it.exists() }
    }

    private fun splitCommandArgs(command: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var quoteChar = '"'
        for (ch in command) {
            when {
                (ch == '"' || ch == '\'') && !inQuotes -> {
                    inQuotes = true
                    quoteChar = ch
                }
                ch == quoteChar && inQuotes -> {
                    inQuotes = false
                }
                ch.isWhitespace() && !inQuotes -> {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) result.add(current.toString())
        return result
    }
}

