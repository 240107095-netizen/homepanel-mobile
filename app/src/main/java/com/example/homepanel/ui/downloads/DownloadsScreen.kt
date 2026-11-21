package com.example.homepanel.ui.downloads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homepanel.R
import com.example.homepanel.data.model.Torrent
import com.example.homepanel.data.model.TorrentState
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.components.EmptyState
import com.example.homepanel.ui.components.ErrorState
import com.example.homepanel.ui.components.LoadingState
import com.example.homepanel.ui.components.StatusBadge
import com.example.homepanel.ui.components.StatusKind
import com.example.homepanel.ui.components.TopStatusBar
import com.example.homepanel.ui.components.TuiButton
import com.example.homepanel.ui.components.TuiProgress
import com.example.homepanel.ui.components.color
import com.example.homepanel.ui.formatBytes
import com.example.homepanel.ui.formatEta
import com.example.homepanel.ui.formatSpeed
import com.example.homepanel.ui.theme.tui

@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = viewModel(factory = DownloadsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Сообщение из ViewModel показываем один раз и сообщаем, что показали
    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbar.showSnackbar(context.getString(msg.text, msg.arg))
            viewModel.onMessageShown()
        }
    }

    Box(modifier.fillMaxSize()) {
        DownloadsContent(state, onToggle = viewModel::toggle, onRetry = viewModel::load)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun DownloadsContent(
    state: DownloadsUiState,
    onToggle: (Torrent) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        TopStatusBar(title = stringResource(R.string.downloads_title)) {
            if (state.totalSpeed > 0) {
                Text("↓ " + formatSpeed(state.totalSpeed), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium)
            }
        }
        when (val torrents = state.torrents) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(stringResource(torrents.message), onRetry)
            is UiState.Success -> if (torrents.data.isEmpty()) {
                EmptyState(stringResource(R.string.downloads_empty), hint = stringResource(R.string.downloads_empty_hint))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(torrents.data, key = { it.hash }) { TorrentRow(it, onToggle = { onToggle(it) }) }
                }
            }
        }
    }
}

// Раздача:
//  Spider-Man.Brand.New.Day.2026.2160p
//  ████████▌         54%
//  ▶ downloading · 3.1 MB/s · left 48m        [ Pause ]
@Composable
private fun TorrentRow(torrent: Torrent, onToggle: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(torrent.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val kind = torrent.kind()
        TuiProgress(torrent.progress, color = kind.color(), modifier = Modifier.padding(vertical = 2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TorrentBadge(torrent.state, kind)
            Text(
                torrentDetails(torrent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.tui.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(4.dp))
            when (torrent.state) {
                TorrentState.DOWNLOADING -> TuiButton(stringResource(R.string.action_pause), onToggle)
                TorrentState.PAUSED -> TuiButton(stringResource(R.string.action_resume), onToggle)
                else -> {}
            }
        }
    }
}

private fun Torrent.kind() = when (state) {
    TorrentState.DOWNLOADING -> StatusKind.ACTIVE
    TorrentState.PAUSED -> StatusKind.WAIT
    TorrentState.DONE -> StatusKind.OK
    TorrentState.ERROR -> StatusKind.ERROR
}

@Composable
private fun TorrentBadge(state: TorrentState, kind: StatusKind) {
    val (icon, label) = when (state) {
        TorrentState.DOWNLOADING -> "▶" to R.string.torrent_downloading
        TorrentState.PAUSED -> "⏸\uFE0E" to R.string.torrent_paused
        TorrentState.DONE -> "✓" to R.string.torrent_done
        TorrentState.ERROR -> "✗" to R.string.torrent_error
    }
    StatusBadge(icon, stringResource(label), kind)
}

// « · 3.1 MB/s · left 48m» или « · 9.3 GB» для готовых
@Composable
private fun torrentDetails(t: Torrent): String = buildString {
    if (t.state == TorrentState.DOWNLOADING) {
        append(" · ").append(formatSpeed(t.downloadSpeed))
        if (t.etaSeconds > 0) append(" · ").append(stringResource(R.string.torrent_eta, formatEta(t.etaSeconds)))
    } else {
        append(" · ").append(formatBytes(t.sizeBytes))
    }
}
