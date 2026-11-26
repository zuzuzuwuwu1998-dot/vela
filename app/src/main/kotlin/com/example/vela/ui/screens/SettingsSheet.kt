package com.example.vela.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vela.EngineStatus
import com.example.vela.model.ThemeMode
import com.example.vela.model.VelaSettings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    settings: VelaSettings,
    engineStatus: EngineStatus,
    isUpdatingEngine: Boolean,
    onUpdateSettings: ((VelaSettings) -> VelaSettings) -> Unit,
    onUpdateEngine: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Vela Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Engine Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Core Engine (yt-dlp + FFmpeg)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        val versionText = when (engineStatus) {
                            is EngineStatus.Ready -> "Active • ${engineStatus.ytDlpVersion ?: "bundled"}"
                            is EngineStatus.Initializing -> "Initializing..."
                            is EngineStatus.Error -> "Error: ${engineStatus.message}"
                        }
                        Text(versionText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = onUpdateEngine, enabled = !isUpdatingEngine) {
                        if (isUpdatingEngine) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Update")
                        }
                    }
                }
            }

            HorizontalDivider()

            Text("Appearance", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onUpdateSettings { it.copy(themeMode = mode) } },
                        label = { Text(mode.label) }
                    )
                }
            }

            SettingToggleRow(
                title = "Material You Dynamic Color",
                subtitle = "Follow wallpaper palette on Android 12+",
                checked = settings.dynamicColor,
                onCheckedChange = { v -> onUpdateSettings { it.copy(dynamicColor = v) } }
            )

            HorizontalDivider()

            Text("Download Defaults", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            SettingToggleRow(
                title = "Aria2c Multi-Thread Turbo",
                subtitle = "Enable 8x segmented connections by default",
                checked = settings.defaultUseAria2c,
                onCheckedChange = { v -> onUpdateSettings { it.copy(defaultUseAria2c = v) } }
            )
            SettingToggleRow(
                title = "SponsorBlock Auto-Skip",
                subtitle = "Strip sponsor, intro & outro segments",
                checked = settings.defaultSponsorBlock,
                onCheckedChange = { v -> onUpdateSettings { it.copy(defaultSponsorBlock = v) } }
            )
            SettingToggleRow(
                title = "Embed Cover Art",
                subtitle = "Attach video thumbnail to downloaded files",
                checked = settings.defaultEmbedThumbnail,
                onCheckedChange = { v -> onUpdateSettings { it.copy(defaultEmbedThumbnail = v) } }
            )

            HorizontalDivider()

            Text("Authentication Cookies (Instagram / Private)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = settings.cookiesTxt,
                onValueChange = { v -> onUpdateSettings { it.copy(cookiesTxt = v) } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste Netscape cookies.txt content here...") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

