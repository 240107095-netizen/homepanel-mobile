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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.example.homepanel.data.model.JobStatus
import com.example.homepanel.data.model.OnlineJob
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
import androidx.compose.ui.tooling.preview.Preview
import com.example.homepanel.ui.theme.HomepanelTheme
import com.example.homepanel.ui.PreviewData

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
        DownloadsContent(
            state,
            onToggle = viewModel::toggle,
            onRetry = viewModel::load,
            onRefresh = viewModel::pullRefresh,
            onRetryJob = viewModel::retryJob,
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsContent(
    state: DownloadsUiState,
    onToggle: (Torrent) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onRetryJob: (OnlineJob) -> Unit = {},
) {
    Column(modifier.fillMaxSize()) {
        TopStatusBar(title = stringResource(R.string.downloads_title)) {
            if (state.stale) {
                StatusBadge("⚠", stringResource(R.string.stale), StatusKind.WAIT)
            } else if (state.totalSpeed > 0) {
                Text("↓ " + formatSpeed(state.totalSpeed), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium)
            }
        }
        when (val torrents = state.torrents) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(stringResource(torrents.message), onRetry)
            is UiState.Success -> if (torrents.data.isEmpty() && state.jobs.isEmpty()) {
                EmptyState(stringResource(R.string.downloads_empty), hint = stringResource(R.string.downloads_empty_hint))
            } else {
                PullToRefreshBox(state.isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        // Две секции: торренты (qBittorrent) и онлайн-загрузки (MediaDL)
                        if (torrents.data.isNotEmpty()) {
                            item(key = "h-torrents") { SectionHeader("qBittorrent", torrents.data.size) }
                            items(torrents.data, key = { it.hash }) { TorrentRow(it, onToggle = { onToggle(it) }) }
                        }
                        if (state.jobs.isNotEmpty()) {
                            item(key = "h-jobs") { SectionHeader("MediaDL", state.jobs.size) }
                            items(state.jobs, key = { "job-" + it.id }) { JobRow(it, onRetry = { onRetryJob(it) }) }
                        }
                    }
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

@Composable
private fun SectionHeader(name: String, count: Int) {
    Text(
        "── $name · $count",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.tui.muted,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp),
    )
}

// Задача MediaDL:
//  Поднятие уровня в одиночку · S2 · AniLibria
//  ████████▌         42%
//  ▶ downloading · ep 5 of 13
//  ✗ сайт не отдал серию 4                     [ Retry ]
@Composable
private fun JobRow(job: OnlineJob, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        val head = buildList {
            add(job.title)
            if (!job.isFilm) add(stringResource(R.string.job_season, job.season))
            if (job.dub.isNotEmpty()) add(job.dub)
        }.joinToString(" · ")
        Text(head, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val kind = when (job.status) {
            JobStatus.RUNNING -> StatusKind.ACTIVE
            JobStatus.QUEUED -> StatusKind.WAIT
            JobStatus.DONE -> StatusKind.OK
            JobStatus.FAILED -> StatusKind.ERROR
        }
        TuiProgress(job.progress, color = kind.color(), modifier = Modifier.padding(vertical = 2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (icon, label) = when (job.status) {
                JobStatus.RUNNING -> "▶" to R.string.torrent_downloading
                JobStatus.QUEUED -> "◷" to R.string.job_queued
                JobStatus.DONE -> "✓" to R.string.torrent_done
                JobStatus.FAILED -> "✗" to R.string.torrent_error
            }
            StatusBadge(icon, stringResource(label), kind)
            Text(
                if (job.isFilm || job.totalEpisodes == 0) "" else
                    " · " + stringResource(R.string.job_episodes, job.doneEpisodes, job.totalEpisodes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.tui.muted,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (job.status == JobStatus.FAILED) TuiButton(stringResource(R.string.retry), onRetry)
        }
        if (job.error.isNotEmpty()) {
            Text(job.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DownloadsPreview() {
    HomepanelTheme {
        DownloadsContent(DownloadsUiState(UiState.Success(PreviewData.torrents)), onToggle = {}, onRetry = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DownloadsErrorPreview() {
    HomepanelTheme {
        DownloadsContent(DownloadsUiState(UiState.Error(R.string.error_no_connection)), onToggle = {}, onRetry = {})
    }
}
