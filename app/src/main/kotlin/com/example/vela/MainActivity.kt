package com.example.vela

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vela.model.QueueStatus
import com.example.vela.model.ThemeMode
import com.example.vela.ui.VelaViewModel
import com.example.vela.ui.screens.GrabScreen
import com.example.vela.ui.screens.QueueScreen
import com.example.vela.ui.screens.SettingsSheet
import com.example.vela.ui.screens.VaultScreen
import com.example.vela.ui.theme.VelaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: VelaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShareIntent(intent)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = when (state.settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            VelaTheme(
                darkTheme = darkTheme,
                dynamicColor = state.settings.dynamicColor
            ) {
                VelaRootApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipText = cm?.primaryClip?.getItemAt(0)?.text?.toString()
        viewModel.checkClipboardText(clipText)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { shared ->
                viewModel.handleSharedUrl(shared)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VelaRootApp(viewModel: VelaViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (perms.isNotEmpty()) {
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val activeQueueCount = state.queue.count {
        it.status == QueueStatus.RUNNING || it.status == QueueStatus.QUEUED
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                        Text(
                            text = "Vela",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.Download, contentDescription = "Grab") },
                    label = { Text("Grab") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeQueueCount > 0) {
                                    Badge { Text("$activeQueueCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Queue, contentDescription = "Queue")
                        }
                    },
                    label = { Text("Queue") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.FolderSpecial, contentDescription = "Vault") },
                    label = { Text("Vault") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> GrabScreen(
                    state = state,
                    onUrlChange = viewModel::onUrlInputChange,
                    onApplyClipboard = viewModel::applyClipboardSuggestion,
                    onDismissClipboard = viewModel::dismissClipboardSuggestion,
                    onToggleBatch = viewModel::toggleBatchMode,
                    onFetchInfo = viewModel::fetchPreview,
                    onUpdateOptions = viewModel::updateOptions,
                    onApplyTemplate = viewModel::applyTemplate,
                    onSaveTemplate = viewModel::saveCurrentAsTemplate,
                    onStartDownload = {
                        val count = viewModel.startDownload()
                        if (count > 0) selectedTab = 1
                    }
                )

                1 -> QueueScreen(
                    queue = state.queue,
                    onCancel = viewModel::cancelQueueItem,
                    onRetry = viewModel::retryQueueItem,
                    onClearFinished = viewModel::clearFinishedQueue
                )

                2 -> VaultScreen(
                    items = state.vault,
                    searchQuery = state.vaultSearch,
                    activeFilter = state.vaultFilter,
                    onSearchChange = viewModel::updateVaultSearch,
                    onFilterChange = viewModel::updateVaultFilter,
                    onOpenItem = { item ->
                        viewModel.buildOpenIntent(item)?.let { intent ->
                            runCatching { context.startActivity(intent) }
                        }
                    },
                    onShareItem = { item ->
                        viewModel.buildShareIntent(item)?.let { intent ->
                            runCatching { context.startActivity(Intent.createChooser(intent, "Share ${item.title}")) }
                        }
                    },
                    onDeleteItem = viewModel::deleteVaultItem
                )
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            settings = state.settings,
            engineStatus = state.engineStatus,
            isUpdatingEngine = state.isUpdatingEngine,
            onUpdateSettings = viewModel::updateSettings,
            onUpdateEngine = viewModel::updateYtDlpEngine,
            onDismiss = { showSettings = false }
        )
    }
}
