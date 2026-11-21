package com.example.homepanel.ui.server

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homepanel.R
import com.example.homepanel.data.model.ServerStatus
import com.example.homepanel.data.model.ServiceStatus
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.components.ErrorState
import com.example.homepanel.ui.components.LanguageSwitcher
import com.example.homepanel.ui.components.LoadingState
import com.example.homepanel.ui.components.StatusBadge
import com.example.homepanel.ui.components.StatusKind
import com.example.homepanel.ui.components.TopStatusBar
import com.example.homepanel.ui.components.TuiButton
import com.example.homepanel.ui.components.TuiProgress
import com.example.homepanel.ui.components.color
import com.example.homepanel.ui.formatBytes
import com.example.homepanel.ui.theme.tui

@Composable
fun ServerScreen(
    modifier: Modifier = Modifier,
    viewModel: ServerViewModel = viewModel(factory = ServerViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbar.showSnackbar(context.getString(msg.text, msg.arg))
            viewModel.onMessageShown()
        }
    }

    Box(modifier.fillMaxSize()) {
        ServerContent(state, onRefresh = viewModel::load, onRescan = viewModel::rescanLibrary)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun ServerContent(
    state: ServerUiState,
    onRefresh: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        TopStatusBar(title = stringResource(R.string.server_title)) {
            val status = state.status
            if (status is UiState.Success) {
                val ok = status.data.services.all { it.online }
                StatusBadge("●", status.data.serverName, if (ok) StatusKind.OK else StatusKind.WAIT)
            }
        }
        Box(Modifier.weight(1f)) {
            when (val status = state.status) {
                UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(stringResource(status.message), onRefresh)
                is UiState.Success -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                ) {
                    DiskSection(status.data)
                    Spacer(Modifier.height(20.dp))
                    SectionTitle(stringResource(R.string.server_services))
                    status.data.services.forEach { ServiceRow(it) }
                    Spacer(Modifier.height(12.dp))
                    Row {
                        TuiButton(stringResource(R.string.action_refresh), onClick = onRefresh)
                        TuiButton(stringResource(R.string.action_rescan), onClick = onRescan, enabled = !state.rescanning)
                    }
                }
            }
        }
        // Язык доступен всегда — даже когда сервер не отвечает
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            SectionTitle(stringResource(R.string.server_language))
            LanguageSwitcher()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 6.dp))
}

// Диск: полоска + «136 GB of 289 GB · free 155 GB». Больше 90% — красным.
@Composable
private fun DiskSection(status: ServerStatus) {
    SectionTitle(stringResource(R.string.server_disk))
    val fraction = if (status.diskTotalBytes > 0) status.diskUsedBytes.toFloat() / status.diskTotalBytes else 0f
    val kind = if (fraction > 0.9f) StatusKind.ERROR else StatusKind.ACTIVE
    TuiProgress(fraction, width = 220.dp, cells = 28, color = kind.color())
    Text(
        stringResource(
            R.string.server_disk_usage,
            formatBytes(status.diskUsedBytes),
            formatBytes(status.diskTotalBytes),
            formatBytes(status.diskTotalBytes - status.diskUsedBytes),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.tui.muted,
    )
}

// «● Jellyfin   12.3.0        ✓ online»
@Composable
private fun ServiceRow(service: ServiceStatus) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val kind = if (service.online) StatusKind.OK else StatusKind.ERROR
        Text("●", color = kind.color())
        Spacer(Modifier.width(8.dp))
        Text(service.name, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(8.dp))
        Text(
            service.version.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.tui.muted,
            modifier = Modifier.weight(1f),
        )
        if (service.online) {
            StatusBadge("✓", stringResource(R.string.service_online), StatusKind.OK)
        } else {
            StatusBadge("✗", stringResource(R.string.service_offline), StatusKind.ERROR)
        }
    }
}
