package com.example.vela.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.vela.EngineStatus
import com.example.vela.model.AudioPreset
import com.example.vela.model.CommandTemplate
import com.example.vela.model.DownloadMode
import com.example.vela.model.DownloadOptions
import com.example.vela.model.PlatformType
import com.example.vela.model.VideoContainer
import com.example.vela.model.VideoQuality
import com.example.vela.model.formatDuration
import com.example.vela.ui.VelaUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GrabScreen(
    state: VelaUiState,
    onUrlChange: (String) -> Unit,
    onApplyClipboard: () -> Unit,
    onDismissClipboard: () -> Unit,
    onToggleBatch: (Boolean) -> Unit,
    onFetchInfo: () -> Unit,
    onUpdateOptions: ((DownloadOptions) -> DownloadOptions) -> Unit,
    onApplyTemplate: (CommandTemplate) -> Unit,
    onSaveTemplate: (String, String) -> Unit,
    onStartDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var showSaveTemplateDialog by remember { mutableStateOf(false) }

    val detectedPlatform = remember(state.urlInput) {
        val firstUrl = state.urlInput.lines().firstOrNull { it.trim().startsWith("http") }
        firstUrl?.let { PlatformType.detect(it) }
    }

    val linkCount = remember(state.urlInput) {
        state.urlInput.lines().count { it.trim().let { l -> l.startsWith("http://") || l.startsWith("https://") } }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Engine status pill if initializing or error
        if (state.engineStatus !is EngineStatus.Ready) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.engineStatus is EngineStatus.Initializing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Warming up yt-dlp + FFmpeg + Aria2c engine...", style = MaterialTheme.typography.bodySmall)
                    } else if (state.engineStatus is EngineStatus.Error) {
                        Text(
                            "Engine error: ${state.engineStatus.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Clipboard Auto-Detect Banner
        AnimatedVisibility(visible = state.clipboardSuggestion != null) {
            val clipUrl = state.clipboardSuggestion ?: ""
            val clipPlatform = PlatformType.detect(clipUrl)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onApplyClipboard() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentPaste,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Link ${clipPlatform.label} terdeteksi di clipboard",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            clipUrl,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    TextButton(onClick = onApplyClipboard) {
                        Text("Tempel")
                    }
                    IconButton(onClick = onDismissClipboard, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Hero URL Input Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (detectedPlatform != null) {
                            Surface(
                                shape = CircleShape,
                                color = Color(detectedPlatform.badgeHex).copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = detectedPlatform.label,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(detectedPlatform.badgeHex)
                                )
                            }
                        } else {
                            Text(
                                "Media Link",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (linkCount > 1) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    "$linkCount links",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = state.isBatchMode,
                            onClick = { onToggleBatch(!state.isBatchMode) },
                            label = { Text("Batch", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Rounded.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = state.urlInput,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            if (state.isBatchMode) {
                                "Paste multiple URLs (one per line)..."
                            } else {
                                "Paste YouTube, TikTok, IG, X, or SoundCloud link..."
                            }
                        )
                    },
                    minLines = if (state.isBatchMode) 3 else 1,
                    maxLines = if (state.isBatchMode) 6 else 3,
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        Row {
                            if (state.urlInput.isNotEmpty()) {
                                IconButton(onClick = { onUrlChange("") }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                                }
                            } else {
                                IconButton(onClick = {
                                    clipboard.getText()?.text?.let { onUrlChange(it) }
                                }) {
                                    Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste")
                                }
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onFetchInfo,
                        enabled = !state.isFetchingInfo && state.urlInput.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (state.isFetchingInfo) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Inspecting...")
                        } else {
                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Inspect Media")
                        }
                    }
                }

                if (state.fetchError != null) {
                    Text(
                        text = state.fetchError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Media Preview Card
        AnimatedVisibility(visible = state.preview != null) {
            state.preview?.let { preview ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 104.dp, height = 66.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (preview.thumbnailUrl != null) {
                                AsyncImage(
                                    model = preview.thumbnailUrl,
                                    contentDescription = preview.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Surface(
                                color = Color.Black.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = formatDuration(preview.durationSec),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = preview.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append(preview.uploader)
                                    preview.estimatedSizeMb?.let {
                                        append(" • ~%.1f MB".format(it))
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4-Way Mode Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DownloadMode.entries.forEach { mode ->
                val selected = state.options.mode == mode
                val icon = when (mode) {
                    DownloadMode.VIDEO -> Icons.Rounded.Movie
                    DownloadMode.AUDIO -> Icons.Rounded.Audiotrack
                    DownloadMode.CLIPPER -> Icons.Rounded.ContentCut
                    DownloadMode.COMMAND -> Icons.Rounded.Terminal
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { onUpdateOptions { it.copy(mode = mode) } }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = mode.label,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Mode-Specific Bento Controls
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (state.options.mode) {
                    DownloadMode.VIDEO -> {
                        Text("Video Quality", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VideoQuality.entries.forEach { q ->
                                FilterChip(
                                    selected = state.options.videoQuality == q,
                                    onClick = { onUpdateOptions { it.copy(videoQuality = q) } },
                                    label = { Text(q.label) }
                                )
                            }
                        }
                        Text("Container", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VideoContainer.entries.forEach { c ->
                                FilterChip(
                                    selected = state.options.videoContainer == c,
                                    onClick = { onUpdateOptions { it.copy(videoContainer = c) } },
                                    label = { Text(c.ext.uppercase()) }
                                )
                            }
                        }
                    }

                    DownloadMode.AUDIO -> {
                        Text("Audio Studio Preset", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AudioPreset.entries.forEach { preset ->
                                val selected = state.options.audioPreset == preset
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            width = if (selected) 1.5.dp else 0.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onUpdateOptions { it.copy(audioPreset = preset) } }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            preset.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            preset.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Text(
                                            preset.format.uppercase(),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DownloadMode.CLIPPER -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Text(
                                "Smart Segment Klip (Potong sebelum download)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = state.options.clipStart,
                                onValueChange = { v -> onUpdateOptions { it.copy(clipStart = v) } },
                                label = { Text("Start (MM:SS)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = state.options.clipEnd,
                                onValueChange = { v -> onUpdateOptions { it.copy(clipEnd = v) } },
                                label = { Text("End (MM:SS)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Extract as Audio Clip (MP3)", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = state.options.clipAudioOnly,
                                onCheckedChange = { checked -> onUpdateOptions { it.copy(clipAudioOnly = checked) } }
                            )
                        }
                    }

                    DownloadMode.COMMAND -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Custom yt-dlp Arguments", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { showSaveTemplateDialog = true }) {
                                Text("Save Preset")
                            }
                        }
                        OutlinedTextField(
                            value = state.options.customCommand,
                            onValueChange = { v -> onUpdateOptions { it.copy(customCommand = v) } },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        Text("Quick Templates", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.templates.forEach { tpl ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { onApplyTemplate(tpl) }
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(tpl.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text(tpl.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Universal Quick Toggles
                if (state.options.mode != DownloadMode.COMMAND) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = state.options.embedThumbnail,
                            onClick = { onUpdateOptions { it.copy(embedThumbnail = !it.embedThumbnail) } },
                            label = { Text("Cover Art") }
                        )
                        if (state.options.mode == DownloadMode.VIDEO) {
                            FilterChip(
                                selected = state.options.embedSubtitles,
                                onClick = { onUpdateOptions { it.copy(embedSubtitles = !it.embedSubtitles) } },
                                label = { Text("Subtitles") }
                            )
                        }
                        FilterChip(
                            selected = state.options.sponsorBlock,
                            onClick = { onUpdateOptions { it.copy(sponsorBlock = !it.sponsorBlock) } },
                            label = { Text("SponsorBlock") }
                        )
                        FilterChip(
                            selected = state.options.useAria2c,
                            onClick = { onUpdateOptions { it.copy(useAria2c = !it.useAria2c) } },
                            leadingIcon = { Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Aria2c Turbo") }
                        )
                    }
                }
            }
        }

        // Primary Harvest Button
        Button(
            onClick = onStartDownload,
            enabled = state.urlInput.isNotBlank() && state.engineStatus is EngineStatus.Ready,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (linkCount > 1) Icons.Rounded.Layers else Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = when {
                            linkCount > 1 -> "Harvest $linkCount Links"
                            state.options.mode == DownloadMode.CLIPPER -> "Klip & Download"
                            state.options.mode == DownloadMode.AUDIO -> "Extract Audio"
                            else -> "Harvest Media"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showSaveTemplateDialog) {
        SaveTemplateDialog(
            onDismiss = { showSaveTemplateDialog = false },
            onSave = { name, desc ->
                onSaveTemplate(name, desc)
                showSaveTemplateDialog = false
            }
        )
    }
}

@Composable
private fun SaveTemplateDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Command Preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Short Description") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, desc) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

