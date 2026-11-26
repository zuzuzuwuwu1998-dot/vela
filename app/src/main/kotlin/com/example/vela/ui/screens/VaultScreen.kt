package com.example.vela.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.vela.model.VaultItem
import com.example.vela.model.VaultMediaType
import com.example.vela.model.formatBytes
import com.example.vela.ui.VaultFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    items: List<VaultItem>,
    searchQuery: String,
    activeFilter: VaultFilter,
    onSearchChange: (String) -> Unit,
    onFilterChange: (VaultFilter) -> Unit,
    onOpenItem: (VaultItem) -> Unit,
    onShareItem: (VaultItem) -> Unit,
    onDeleteItem: (VaultItem, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedItem by remember { mutableStateOf<VaultItem?>(null) }
    val clipboard = LocalClipboardManager.current

    val filtered = remember(items, searchQuery, activeFilter) {
        items.filter { item ->
            val matchesFilter = when (activeFilter) {
                VaultFilter.ALL -> true
                VaultFilter.VIDEO -> item.mediaType == VaultMediaType.VIDEO
                VaultFilter.AUDIO -> item.mediaType == VaultMediaType.AUDIO
                VaultFilter.CLIP -> item.mediaType == VaultMediaType.CLIP
            }
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.uploader.contains(searchQuery, ignoreCase = true) ||
                item.platform.label.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search downloaded media...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VaultFilter.entries.forEach { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label) }
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        "Vault is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Completed downloads appear here for instant playback & sharing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    VaultCard(
                        item = item,
                        onClick = { selectedItem = item }
                    )
                }
            }
        }
    }

    selectedItem?.let { item ->
        ModalBottomSheet(onDismissRequest = { selectedItem = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                ListItem(
                    headlineContent = { Text("Open / Play") },
                    leadingContent = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                    modifier = Modifier.clickable {
                        selectedItem = null
                        onOpenItem(item)
                    }
                )
                ListItem(
                    headlineContent = { Text("Share File") },
                    leadingContent = { Icon(Icons.Rounded.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        selectedItem = null
                        onShareItem(item)
                    }
                )
                ListItem(
                    headlineContent = { Text("Copy Source URL") },
                    leadingContent = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                    modifier = Modifier.clickable {
                        clipboard.setText(AnnotatedString(item.sourceUrl))
                        selectedItem = null
                    }
                )
                ListItem(
                    headlineContent = { Text("Delete File & Entry", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        selectedItem = null
                        onDeleteItem(item, true)
                    }
                )
            }
        }
    }
}

@Composable
private fun VaultCard(
    item: VaultItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnailUrl != null) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val icon = when (item.mediaType) {
                        VaultMediaType.AUDIO -> Icons.Rounded.Audiotrack
                        VaultMediaType.CLIP -> Icons.Rounded.ContentCut
                        VaultMediaType.VIDEO -> Icons.Rounded.Movie
                    }
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(item.platform.badgeHex).copy(alpha = 0.16f)
                    ) {
                        Text(
                            item.platform.label,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(item.platform.badgeHex),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "${formatBytes(item.fileSizeBytes)} • ${item.mediaType.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

