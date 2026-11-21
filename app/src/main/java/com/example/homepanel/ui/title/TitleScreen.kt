package com.example.homepanel.ui.title

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homepanel.R
import com.example.homepanel.data.model.Season
import com.example.homepanel.data.model.Title
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.components.ErrorState
import com.example.homepanel.ui.components.LoadingState
import com.example.homepanel.ui.components.Poster
import com.example.homepanel.ui.components.StatusBadge
import com.example.homepanel.ui.components.StatusKind
import com.example.homepanel.ui.components.TitleBadge
import com.example.homepanel.ui.components.TopStatusBar
import com.example.homepanel.ui.components.titleMeta
import com.example.homepanel.ui.theme.tui
import java.util.Locale

@Composable
fun TitleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSeasonClick: (Season) -> Unit = {},
    viewModel: TitleViewModel = viewModel(factory = TitleViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TitleContent(state, onBack, viewModel::load, onSeasonClick, modifier)
}

@Composable
fun TitleContent(
    state: UiState<TitleDetails>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSeasonClick: (Season) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        val heading = (state as? UiState.Success)?.data?.title?.name.orEmpty()
        TopStatusBar(title = heading, onBack = onBack)
        when (state) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(stringResource(state.message), onRetry)
            is UiState.Success -> LazyColumn(Modifier.fillMaxSize()) {
                item { TitleHeader(state.data.title) }
                item { Overview(state.data.title.overview) }
                if (state.data.seasons.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.title_seasons),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(state.data.seasons, key = { it.id }) { season ->
                        SeasonRow(season, onClick = { onSeasonClick(season) })
                    }
                }
            }
        }
    }
}

// Постер слева, справа название, мета, оценка и прогресс просмотра
@Composable
private fun TitleHeader(title: Title) {
    Row(Modifier.padding(12.dp)) {
        Poster(name = title.name, width = 110.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(titleMeta(title), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.tui.muted)
            Spacer(Modifier.height(8.dp))
            title.rating?.let {
                Text(stringResource(R.string.title_rating, String.format(Locale.US, "%.1f", it)))
                Spacer(Modifier.height(4.dp))
            }
            TitleBadge(title)
            if (title.episodeCount > 0) {
                Text(
                    pluralStringResource(R.plurals.episodes, title.episodeCount, title.episodeCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.tui.muted,
                )
            }
        }
    }
}

// Описание в 3 строки, тап раскрывает целиком
@Composable
private fun Overview(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .animateContentSize(),
    ) {
        Text(
            text.ifBlank { stringResource(R.string.title_no_overview) },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (text.length > 120) {
            Text(
                stringResource(if (expanded) R.string.title_less else R.string.title_more),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// «▸ Season 1   8 episodes   ✓ watched»
@Composable
private fun SeasonRow(season: Season, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("▸ ", color = MaterialTheme.tui.muted)
        Text(season.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            pluralStringResource(R.plurals.episodes, season.episodeCount, season.episodeCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.tui.muted,
        )
        Spacer(Modifier.width(12.dp))
        val watched = season.episodeCount - season.unplayedCount
        when {
            season.unplayedCount == 0 ->
                StatusBadge("✓", stringResource(R.string.season_watched), StatusKind.OK)
            watched > 0 ->
                StatusBadge("▶", stringResource(R.string.season_progress, watched, season.episodeCount), StatusKind.ACTIVE)
            else ->
                StatusBadge("○", stringResource(R.string.season_progress, 0, season.episodeCount), StatusKind.MUTED)
        }
    }
}
