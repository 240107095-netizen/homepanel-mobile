package com.example.homepanel.ui.sources

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homepanel.R
import com.example.homepanel.data.model.OnlineResult
import com.example.homepanel.data.model.Release
import com.example.homepanel.data.model.Voice
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.components.EmptyState
import com.example.homepanel.ui.components.ErrorState
import com.example.homepanel.ui.components.LoadingState
import com.example.homepanel.ui.components.Poster
import com.example.homepanel.ui.components.StatusKind
import com.example.homepanel.ui.components.TopStatusBar
import com.example.homepanel.ui.components.TuiTabs
import com.example.homepanel.ui.components.color
import com.example.homepanel.ui.formatBytes
import com.example.homepanel.ui.theme.tui

@Composable
fun SourcesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SourcesViewModel = viewModel(factory = SourcesViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SourcesContent(
        state = state,
        onBack = onBack,
        onRetryInfo = viewModel::load,
        onRetryReleases = { viewModel.retryReleases() },
        onRetryOnline = { viewModel.retryOnline() },
        onTabChange = viewModel::onTabChange,
        modifier = modifier,
    )
}

@Composable
fun SourcesContent(
    state: SourcesUiState,
    onBack: () -> Unit,
    onRetryInfo: () -> Unit,
    onRetryReleases: () -> Unit,
    onRetryOnline: () -> Unit,
    onTabChange: (SourceTab) -> Unit,
    modifier: Modifier = Modifier,
    onReleaseClick: (Release) -> Unit = {},
    onOnlineClick: (OnlineResult) -> Unit = {},
) {
    Column(modifier.fillMaxSize()) {
        TopStatusBar(title = state.heading, onBack = onBack)
        when (val info = state.info) {
            UiState.Loading -> LoadingState(text = stringResource(R.string.sources_title_loading))
            is UiState.Error -> ErrorState(stringResource(info.message), onRetryInfo)
            is UiState.Success -> {
                TuiTabs(
                    labels = listOf(
                        tabLabel(stringResource(R.string.sources_trackers), state.releases),
                        tabLabel(stringResource(R.string.sources_online), state.online),
                    ),
                    selected = state.tab.ordinal,
                    onSelect = { onTabChange(SourceTab.entries[it]) },
                )
                when (state.tab) {
                    SourceTab.TRACKERS -> ReleasesTab(state.releases, onRetryReleases, onReleaseClick)
                    SourceTab.ONLINE -> OnlineTab(state.online, onRetryOnline, onOnlineClick)
                }
            }
        }
    }
}

// «Trackers · 42» — сколько нашлось, «…» — ещё ищет
private fun tabLabel(name: String, state: UiState<List<*>>) = when (state) {
    UiState.Loading -> "$name …"
    is UiState.Error -> "$name ✗"
    is UiState.Success -> "$name · ${state.data.size}"
}

@Composable
private fun ReleasesTab(state: UiState<List<Release>>, onRetry: () -> Unit, onClick: (Release) -> Unit) {
    when (state) {
        UiState.Loading -> LoadingState(text = stringResource(R.string.sources_searching_trackers))
        is UiState.Error -> ErrorState(stringResource(state.message), onRetry)
        is UiState.Success -> if (state.data.isEmpty()) {
            EmptyState(stringResource(R.string.sources_no_releases), hint = stringResource(R.string.sources_no_releases_hint))
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.data, key = { it.guid }) { ReleaseRow(it, onClick = { onClick(it) }) }
            }
        }
    }
}

// Раздача:
//  WEB-DL 2160p · HDR                         58.3 GB
//  dub + original · RuTracker.org · ▲ 214
//  Dune (2021) WEB-DL 2160p HDR Dub RUS Eng   (тусклым — как на трекере)
@Composable
private fun ReleaseRow(release: Release, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(releaseHeadline(release), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(formatBytes(release.sizeBytes), style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val voices = release.voices.map { voiceLabel(it) }.joinToString(" + ")
            if (voices.isNotEmpty()) {
                Text(voices, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(" · ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.tui.muted)
            }
            Text(release.indexer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.tui.muted)
            release.seasons.firstOrNull()?.let {
                Text(" · " + stringResource(R.string.release_season, it), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.tui.muted)
            }
            Spacer(Modifier.weight(1f))
            // Живая раздача скачается, мёртвая — нет: 0 сидов красным
            val seedKind = when {
                release.seeders == 0 -> StatusKind.ERROR
                release.seeders < 5 -> StatusKind.WAIT
                else -> StatusKind.OK
            }
            Text("▲ ${release.seeders}", style = MaterialTheme.typography.bodySmall, color = seedKind.color())
        }
        Text(
            release.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.tui.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// «WEB-DL 2160p · HDR»
@Composable
fun releaseHeadline(r: Release): String {
    val video = listOf(r.source, r.resolution).filter { it.isNotEmpty() }.joinToString(" ")
        .ifEmpty { stringResource(R.string.quality_unknown) }
    return (listOf(video) + r.extras.take(1)).joinToString(" · ")
}

@Composable
fun voiceLabel(v: Voice): String = stringResource(
    when (v) {
        Voice.DUB -> R.string.voice_dub
        Voice.MULTI -> R.string.voice_multi
        Voice.DUAL -> R.string.voice_dual
        Voice.AUTHOR -> R.string.voice_author
        Voice.RUSSIAN -> R.string.voice_russian
        Voice.ORIGINAL -> R.string.voice_original
        Voice.SUBS -> R.string.voice_subs
    },
)

@Composable
private fun OnlineTab(state: UiState<List<OnlineResult>>, onRetry: () -> Unit, onClick: (OnlineResult) -> Unit) {
    when (state) {
        UiState.Loading -> LoadingState(text = stringResource(R.string.sources_searching_online))
        is UiState.Error -> ErrorState(stringResource(state.message), onRetry)
        is UiState.Success -> if (state.data.isEmpty()) {
            EmptyState(stringResource(R.string.sources_no_online))
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.data, key = { "${it.site}-${it.idx}" }) { OnlineRow(it, onClick = { onClick(it) }) }
            }
        }
    }
}

@Composable
private fun OnlineRow(item: OnlineResult, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Poster(name = item.title, url = item.posterUrl, width = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(item.year, item.site.label).filter { it.isNotEmpty() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.tui.muted,
            )
        }
        Text("▸", color = MaterialTheme.tui.muted)
    }
}
