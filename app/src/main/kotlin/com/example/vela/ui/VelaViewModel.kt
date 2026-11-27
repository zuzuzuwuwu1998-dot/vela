package com.example.vela.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vela.EngineStatus
import com.example.vela.VelaApplication
import com.example.vela.data.VelaRepository
import com.example.vela.engine.VelaDownloadEngine
import com.example.vela.model.CommandTemplate
import com.example.vela.model.DownloadMode
import com.example.vela.model.DownloadOptions
import com.example.vela.model.MediaPreview
import com.example.vela.model.PlatformType
import com.example.vela.model.QueueItem
import com.example.vela.model.QueueStatus
import com.example.vela.model.VaultItem
import com.example.vela.model.VaultMediaType
import com.example.vela.model.VelaSettings
import com.example.vela.model.formatDuration
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class VaultFilter(val label: String) {
    ALL("All"),
    VIDEO("Video"),
    AUDIO("Audio"),
    CLIP("Klip")
}

data class VelaUiState(
    val urlInput: String = "",
    val clipboardSuggestion: String? = null,
    val isBatchMode: Boolean = false,
    val isFetchingInfo: Boolean = false,
    val fetchError: String? = null,
    val preview: MediaPreview? = null,
    val options: DownloadOptions = DownloadOptions(),
    val queue: List<QueueItem> = emptyList(),
    val vault: List<VaultItem> = emptyList(),
    val vaultSearch: String = "",
    val vaultFilter: VaultFilter = VaultFilter.ALL,
    val templates: List<CommandTemplate> = emptyList(),
    val settings: VelaSettings = VelaSettings(),
    val engineStatus: EngineStatus = EngineStatus.Initializing,
    val isUpdatingEngine: Boolean = false,
    val snackbarMessage: String? = null
)

class VelaViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as VelaApplication
    private val repository = VelaRepository(application)
    private val engine = VelaDownloadEngine(application)
    private val activeJobs = mutableMapOf<String, Job>()

    private val _uiState = MutableStateFlow(
        VelaUiState(
            vault = repository.loadVault(),
            templates = repository.loadTemplates(),
            settings = repository.loadSettings()
        )
    )
    val uiState: StateFlow<VelaUiState> = _uiState.asStateFlow()

    init {
        val loadedSettings = _uiState.value.settings
        _uiState.update {
            it.copy(
                options = it.options.copy(
                    useAria2c = loadedSettings.defaultUseAria2c,
                    sponsorBlock = loadedSettings.defaultSponsorBlock,
                    embedThumbnail = loadedSettings.defaultEmbedThumbnail
                )
            )
        }
        viewModelScope.launch {
            app.engineStatus.collect { status ->
                _uiState.update { it.copy(engineStatus = status) }
            }
        }
    }

    fun onUrlInputChange(value: String) {
        val multiline = value.lines().count { it.trim().startsWith("http") } > 1
        _uiState.update {
            it.copy(
                urlInput = value,
                isBatchMode = multiline || it.isBatchMode,
                fetchError = null,
                preview = if (value.isBlank()) null else it.preview
            )
        }
    }

    fun checkClipboardText(clipText: String?) {
        val trimmed = clipText?.trim() ?: return
        if ((trimmed.startsWith("http://") || trimmed.startsWith("https://")) &&
            trimmed != _uiState.value.urlInput.trim()
        ) {
            _uiState.update { it.copy(clipboardSuggestion = trimmed) }
        }
    }

    fun applyClipboardSuggestion() {
        val suggested = _uiState.value.clipboardSuggestion ?: return
        _uiState.update {
            it.copy(
                urlInput = suggested,
                clipboardSuggestion = null,
                fetchError = null
            )
        }
        fetchPreview()
    }

    fun dismissClipboardSuggestion() {
        _uiState.update { it.copy(clipboardSuggestion = null) }
    }

    fun handleSharedUrl(sharedText: String) {
        val extracted = Regex("""https?://\S+""").find(sharedText)?.value ?: return
        _uiState.update {
            it.copy(
                urlInput = extracted,
                clipboardSuggestion = null,
                fetchError = null
            )
        }
        fetchPreview()
    }

    fun toggleBatchMode(enabled: Boolean) {
        _uiState.update { it.copy(isBatchMode = enabled) }
    }

    fun updateOptions(transform: (DownloadOptions) -> DownloadOptions) {
        _uiState.update { it.copy(options = transform(it.options)) }
    }

    fun applyTemplate(template: CommandTemplate) {
        _uiState.update {
            it.copy(
                options = it.options.copy(
                    mode = DownloadMode.COMMAND,
                    customCommand = template.args
                ),
                snackbarMessage = "Applied template: ${template.name}"
            )
        }
    }

    fun saveCurrentAsTemplate(name: String, description: String) {
        if (name.isBlank()) return
        val newTpl = CommandTemplate(
            id = "tpl_${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            description = description.trim().ifBlank { "Custom user preset" },
            args = _uiState.value.options.customCommand.trim()
        )
        val updated = listOf(newTpl) + _uiState.value.templates
        repository.saveTemplates(updated)
        _uiState.update {
            it.copy(
                templates = updated,
                snackbarMessage = "Saved template: ${newTpl.name}"
            )
        }
    }

    fun fetchPreview() {
        val rawUrl = _uiState.value.urlInput.lines().firstOrNull { it.trim().startsWith("http") }?.trim()
        if (rawUrl.isNullOrBlank()) {
            _uiState.update { it.copy(fetchError = "Enter a valid http/https link first") }
            return
        }
        if (_uiState.value.engineStatus !is EngineStatus.Ready) {
            _uiState.update { it.copy(fetchError = "Downloader engine is still initializing...") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingInfo = true, fetchError = null) }
            val result = engine.fetchMediaInfo(rawUrl, _uiState.value.settings.cookiesTxt)
            result.onSuccess { preview ->
                _uiState.update {
                    it.copy(
                        isFetchingInfo = false,
                        preview = preview,
                        fetchError = null
                    )
                }
            }.onFailure { err ->
                val rawMsg = err.message ?: "Failed to inspect media link"
                val cleanMsg = rawMsg.lines()
                    .firstOrNull { it.contains("ERROR:", ignoreCase = true) }
                    ?: rawMsg.lines().lastOrNull { it.isNotBlank() }
                    ?: rawMsg
                _uiState.update {
                    it.copy(
                        isFetchingInfo = false,
                        fetchError = cleanMsg
                    )
                }
            }
        }
    }

    fun startDownload(): Int {
        val urls = _uiState.value.urlInput
            .lines()
            .map { it.trim() }
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()

        if (urls.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Paste at least one valid URL") }
            return 0
        }
        if (_uiState.value.engineStatus !is EngineStatus.Ready) {
            _uiState.update { it.copy(snackbarMessage = "Engine is still starting up, wait a moment") }
            return 0
        }

        val currentOptions = _uiState.value.options
        val currentPreview = _uiState.value.preview

        urls.forEachIndexed { index, url ->
            val previewForItem = if (index == 0 && currentPreview?.url == url) currentPreview else null
            enqueueSingle(url, currentOptions, previewForItem)
        }

        _uiState.update {
            it.copy(
                snackbarMessage = if (urls.size == 1) "Added to Queue" else "Added ${urls.size} links to Batch Queue"
            )
        }
        return urls.size
    }

    private fun enqueueSingle(
        url: String,
        options: DownloadOptions,
        preview: MediaPreview?
    ) {
        val taskId = UUID.randomUUID().toString()
        val platform = PlatformType.detect(url)
        val initialItem = QueueItem(
            id = taskId,
            url = url,
            title = preview?.title ?: "${platform.label} Media",
            uploader = preview?.uploader ?: platform.label,
            platform = platform,
            mode = options.mode,
            status = QueueStatus.RUNNING,
            progress = 0f,
            statusLine = "Starting ${options.mode.label.lowercase()} pipeline..."
        )

        _uiState.update { it.copy(queue = listOf(initialItem) + it.queue) }

        val job = viewModelScope.launch {
            val result = engine.executeDownload(
                taskId = taskId,
                url = url,
                options = options,
                cookiesTxt = _uiState.value.settings.cookiesTxt
            ) { progress, eta, line ->
                _uiState.update { state ->
                    state.copy(
                        queue = state.queue.map { item ->
                            if (item.id == taskId && item.status == QueueStatus.RUNNING) {
                                item.copy(
                                    progress = if (progress > 0f) progress else item.progress,
                                    etaSeconds = eta,
                                    statusLine = line.ifBlank { item.statusLine }
                                )
                            } else item
                        }
                    )
                }
            }

            result.onSuccess { dl ->
                val outFile = dl.outputFile
                _uiState.update { state ->
                    state.copy(
                        queue = state.queue.map { item ->
                            if (item.id == taskId) {
                                item.copy(
                                    title = preview?.title ?: dl.title,
                                    status = QueueStatus.COMPLETED,
                                    progress = 100f,
                                    statusLine = "Saved to ${outFile?.parentFile?.name ?: "Vela"}",
                                    outputPath = outFile?.absolutePath
                                )
                            } else item
                        }
                    )
                }

                if (outFile != null && outFile.exists()) {
                    val mediaType = when {
                        options.mode == DownloadMode.CLIPPER -> VaultMediaType.CLIP
                        options.mode == DownloadMode.AUDIO -> VaultMediaType.AUDIO
                        outFile.extension.lowercase() in setOf("mp3", "m4a", "flac", "opus", "wav") -> VaultMediaType.AUDIO
                        else -> VaultMediaType.VIDEO
                    }
                    val vaultEntry = VaultItem(
                        id = taskId,
                        title = preview?.title ?: dl.title,
                        uploader = preview?.uploader ?: platform.label,
                        platform = platform,
                        mediaType = mediaType,
                        filePath = outFile.absolutePath,
                        fileName = outFile.name,
                        fileSizeBytes = outFile.length(),
                        durationText = preview?.durationSec?.let { formatDuration(it) } ?: "--:--",
                        thumbnailUrl = preview?.thumbnailUrl,
                        sourceUrl = url
                    )
                    val updatedVault = listOf(vaultEntry) + _uiState.value.vault
                    repository.saveVault(updatedVault)
                    _uiState.update { it.copy(vault = updatedVault) }
                }
            }.onFailure { err ->
                _uiState.update { state ->
                    state.copy(
                        queue = state.queue.map { item ->
                            if (item.id == taskId && item.status != QueueStatus.CANCELLED) {
                                item.copy(
                                    status = QueueStatus.FAILED,
                                    statusLine = "Download failed",
                                    errorMessage = err.message ?: "Unknown error"
                                )
                            } else item
                        }
                    )
                }
            }
            activeJobs.remove(taskId)
        }
        activeJobs[taskId] = job
    }

    fun cancelQueueItem(taskId: String) {
        engine.cancelDownload(taskId)
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        _uiState.update { state ->
            state.copy(
                queue = state.queue.map {
                    if (it.id == taskId) {
                        it.copy(status = QueueStatus.CANCELLED, statusLine = "Cancelled by user")
                    } else it
                }
            )
        }
    }

    fun retryQueueItem(item: QueueItem) {
        _uiState.update { state ->
            state.copy(queue = state.queue.filterNot { it.id == item.id })
        }
        enqueueSingle(item.url, _uiState.value.options.copy(mode = item.mode), null)
    }

    fun clearFinishedQueue() {
        _uiState.update { state ->
            state.copy(
                queue = state.queue.filter {
                    it.status == QueueStatus.RUNNING || it.status == QueueStatus.QUEUED
                }
            )
        }
    }

    fun updateVaultSearch(query: String) {
        _uiState.update { it.copy(vaultSearch = query) }
    }

    fun updateVaultFilter(filter: VaultFilter) {
        _uiState.update { it.copy(vaultFilter = filter) }
    }

    fun deleteVaultItem(item: VaultItem, deleteFileFromDisk: Boolean) {
        if (deleteFileFromDisk) {
            runCatching { File(item.filePath).delete() }
        }
        val updated = _uiState.value.vault.filterNot { it.id == item.id }
        repository.saveVault(updated)
        _uiState.update {
            it.copy(
                vault = updated,
                snackbarMessage = if (deleteFileFromDisk) "Deleted file & history" else "Removed from Vault"
            )
        }
    }

    fun buildOpenIntent(item: VaultItem): Intent? {
        val file = File(item.filePath)
        if (!file.exists()) {
            _uiState.update { it.copy(snackbarMessage = "File not found on storage") }
            return null
        }
        val uri: Uri = FileProvider.getUriForFile(
            app,
            "${app.packageName}.fileprovider",
            file
        )
        val mime = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase())
            ?: if (item.mediaType == VaultMediaType.AUDIO) "audio/*" else "video/*"

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun buildShareIntent(item: VaultItem): Intent? {
        val file = File(item.filePath)
        if (!file.exists()) {
            _uiState.update { it.copy(snackbarMessage = "File not found on storage") }
            return null
        }
        val uri: Uri = FileProvider.getUriForFile(
            app,
            "${app.packageName}.fileprovider",
            file
        )
        val mime = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase())
            ?: "*/*"

        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun updateSettings(transform: (VelaSettings) -> VelaSettings) {
        val updated = transform(_uiState.value.settings)
        repository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun updateYtDlpEngine() {
        if (_uiState.value.isUpdatingEngine) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingEngine = true) }
            val result = app.updateYtDlp()
            _uiState.update { state ->
                state.copy(
                    isUpdatingEngine = false,
                    snackbarMessage = result.fold(
                        onSuccess = { "yt-dlp updated: $it" },
                        onFailure = { "Update failed: ${it.message}" }
                    )
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}

