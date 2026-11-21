package com.example.homepanel.ui.server

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homepanel.R
import com.example.homepanel.data.model.ServerStatus
import com.example.homepanel.data.model.ServiceStatus
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.components.ErrorState
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
    ServerContent(state, onRefresh = viewModel::load, modifier = modifier)
}

@Composable
fun ServerContent(state: UiState<ServerStatus>, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        TopStatusBar(title = stringResource(R.string.server_title)) {
            if (state is UiState.Success) {
                val ok = state.data.services.all { it.online }
                StatusBadge("●", state.data.serverName, if (ok) StatusKind.OK else StatusKind.WAIT)
            }
        }
        when (state) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(stringResource(state.message), onRefresh)
            is UiState.Success -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
            ) {
                DiskSection(state.data)
                Spacer(Modifier.height(20.dp))
                SectionTitle(stringResource(R.string.server_services))
                state.data.services.forEach { ServiceRow(it) }
                Spacer(Modifier.height(12.dp))
                TuiButton(stringResource(R.string.action_refresh), onClick = onRefresh)
            }
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
