package com.example.homepanel.ui.season

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
import com.example.homepanel.data.model.Episode
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.components.EmptyState
import com.example.homepanel.ui.components.ErrorState
import com.example.homepanel.ui.components.LoadingState
import com.example.homepanel.ui.components.StatusKind
import com.example.homepanel.ui.components.TopStatusBar
import com.example.homepanel.ui.components.color
import com.example.homepanel.ui.theme.tui
import java.util.Locale

@Composable
fun SeasonScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeasonViewModel = viewModel(factory = SeasonViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SeasonContent(state, onBack, viewModel::load, modifier)
}

@Composable
fun SeasonContent(
    state: UiState<List<Episode>>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        val number = (state as? UiState.Success)?.data?.firstOrNull()?.season
        TopStatusBar(
            title = if (number != null) stringResource(R.string.season_number, number) else "",
            onBack = onBack,
        )
        when (state) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(stringResource(state.message), onRetry)
            is UiState.Success -> if (state.data.isEmpty()) {
                EmptyState(stringResource(R.string.season_empty))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.data, key = { it.id }) { EpisodeRow(it) }
                }
            }
        }
    }
}

// «✓ S01E05  Название серии        24m»
@Composable
private fun EpisodeRow(episode: Episode) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val kind = if (episode.played) StatusKind.OK else StatusKind.MUTED
        Text(if (episode.played) "✓" else "○", color = kind.color())
        Spacer(Modifier.width(8.dp))
        Text(
            String.format(Locale.US, "S%02dE%02d", episode.season, episode.number),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.tui.muted,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            episode.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (episode.runtimeMinutes > 0) {
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.episode_minutes, episode.runtimeMinutes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.tui.muted,
            )
        }
    }
}
