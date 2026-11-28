package com.example.homepanel.ui.upcoming

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homepanel.R
import com.example.homepanel.data.model.AirEpisode
import com.example.homepanel.data.model.ShowSchedule
import com.example.homepanel.data.model.ShowStatus
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.components.EmptyState
import com.example.homepanel.ui.components.ErrorState
import com.example.homepanel.ui.components.LoadingState
import com.example.homepanel.ui.components.Poster
import com.example.homepanel.ui.components.StatusBadge
import com.example.homepanel.ui.components.StatusKind
import com.example.homepanel.ui.components.TopStatusBar
import com.example.homepanel.ui.theme.HomepanelTheme
import com.example.homepanel.ui.theme.tui
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun UpcomingScreen(
    onBack: () -> Unit,
    onShowClick: (titleId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpcomingViewModel = viewModel(factory = UpcomingViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UpcomingContent(state, onBack = onBack, onRetry = viewModel::load, onShowClick = onShowClick, modifier = modifier)
}

@Composable
fun UpcomingContent(
    state: UiState<List<ShowSchedule>>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onShowClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        TopStatusBar(title = stringResource(R.string.upcoming_title), onBack = onBack) {
            // Откуда данные — честно пишем в шапке
            Text("TVmaze", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.tui.muted)
        }
        when (state) {
            UiState.Loading -> LoadingState(text = stringResource(R.string.upcoming_loading))
            is UiState.Error -> ErrorState(stringResource(state.message), onRetry)
            is UiState.Success -> if (state.data.isEmpty()) {
                EmptyState(stringResource(R.string.upcoming_empty), hint = stringResource(R.string.upcoming_empty_hint))
            } else {
                Text(
                    stringResource(
                        R.string.upcoming_summary,
                        state.data.count { it.isBehind },
                        state.data.count { it.next != null },
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.tui.muted,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.data, key = { it.titleId }) { show ->
                        ShowRow(show, onClick = { onShowClick(show.titleId) })
                    }
                }
            }
        }
    }
}

// Строка сериала:
//  [постер] Киберпанк: Бегущие по краю
//           Netflix · ◷ undecided
//           ⚠ 10 new · server has 10 of 20
//           last S02E10 · 2026-10-20
@Composable
private fun ShowRow(show: ShowSchedule, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Poster(name = show.name, url = show.posterUrl, width = 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(show.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (show.network.isNotEmpty()) {
                    Text("${show.network} · ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.tui.muted)
                }
                ShowStatusBadge(show.status)
            }
            if (show.missing > 0) {
                StatusBadge(
                    "⚠",
                    stringResource(R.string.upcoming_missing, show.missing, show.onServer, show.airedEpisodes),
                    StatusKind.WAIT,
                )
            } else if (show.airedEpisodes > 0) {
                StatusBadge("✓", stringResource(R.string.upcoming_all_here, show.airedEpisodes), StatusKind.OK)
            }
            if (show.newSeasons.isNotEmpty()) {
                val first = show.newSeasons.first()
                val last = show.newSeasons.last()
                val text = if (first == last) {
                    stringResource(R.string.upcoming_new_season, first, show.newSeasonEpisodes)
                } else {
                    stringResource(R.string.upcoming_new_seasons, first, last, show.newSeasonEpisodes)
                }
                StatusBadge("▲", text, StatusKind.WAIT)
            }
            show.next?.let { ep ->
                StatusBadge("▶", stringResource(R.string.upcoming_next, ep.code(), whenLabel(ep.airdate)), StatusKind.ACTIVE)
            }
            show.lastAired?.let { ep ->
                Text(
                    stringResource(R.string.upcoming_last, ep.code(), ep.airdate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.tui.muted,
                )
            }
        }
    }
}

@Composable
private fun ShowStatusBadge(status: ShowStatus) {
    val (icon, label, kind) = when (status) {
        ShowStatus.RUNNING -> Triple("▶", R.string.show_running, StatusKind.ACTIVE)
        ShowStatus.ENDED -> Triple("■", R.string.show_ended, StatusKind.MUTED)
        ShowStatus.TBD -> Triple("◷", R.string.show_tbd, StatusKind.WAIT)
        ShowStatus.UNKNOWN -> Triple("?", R.string.show_unknown, StatusKind.MUTED)
    }
    StatusBadge(icon, stringResource(label), kind)
}

// «S02E05»
private fun AirEpisode.code() = "S%02dE%02d".format(season, number)

// «сегодня», «завтра», «через 6 дней · 2026-12-04»
@Composable
private fun whenLabel(airdate: String): String {
    val days = daysUntil(airdate) ?: return airdate
    val relative = when {
        days <= 0L -> stringResource(R.string.upcoming_today)
        days == 1L -> stringResource(R.string.upcoming_tomorrow)
        else -> pluralStringResource(R.plurals.upcoming_in_days, days.toInt(), days.toInt())
    }
    return "$relative · $airdate"
}

private fun daysUntil(airdate: String): Long? {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val date = runCatching { format.parse(airdate) }.getOrNull() ?: return null
    val today = format.parse(format.format(Date())) ?: return null
    return TimeUnit.MILLISECONDS.toDays(date.time - today.time)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun UpcomingPreview() {
    HomepanelTheme {
        UpcomingContent(
            UiState.Success(
                listOf(
                    ShowSchedule("s1", "Киберпанк: Бегущие по краю", 2022, null, "Netflix", ShowStatus.TBD,
                        airedEpisodes = 10, onServer = 2, missing = 8, newSeasons = listOf(2), newSeasonEpisodes = 10,
                        lastAired = AirEpisode(2, 10, "", "2026-10-20"), next = null),
                    ShowSchedule("s3", "Шогун", 2024, null, "Hulu", ShowStatus.RUNNING,
                        airedEpisodes = 10, onServer = 10, missing = 0, newSeasons = emptyList(), newSeasonEpisodes = 0,
                        lastAired = AirEpisode(1, 10, "", "2024-04-23"),
                        next = AirEpisode(2, 1, "", "2026-12-04")),
                ),
            ),
            onBack = {}, onRetry = {}, onShowClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun UpcomingErrorPreview() {
    HomepanelTheme {
        UpcomingContent(UiState.Error(R.string.error_tvmaze), onBack = {}, onRetry = {}, onShowClick = {})
    }
}
