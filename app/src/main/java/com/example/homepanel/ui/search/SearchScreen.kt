package com.example.homepanel.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homepanel.R
import com.example.homepanel.data.model.MediaStatus
import com.example.homepanel.data.model.SearchResult
import com.example.homepanel.data.model.TitleType
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.components.EmptyState
import com.example.homepanel.ui.components.ErrorState
import com.example.homepanel.ui.components.LoadingState
import com.example.homepanel.ui.components.Poster
import com.example.homepanel.ui.components.StatusBadge
import com.example.homepanel.ui.components.StatusKind
import com.example.homepanel.ui.components.TopStatusBar
import com.example.homepanel.ui.components.TuiButton
import com.example.homepanel.ui.components.TuiSearchField
import com.example.homepanel.ui.theme.tui
import androidx.compose.ui.tooling.preview.Preview
import com.example.homepanel.ui.theme.HomepanelTheme
import com.example.homepanel.ui.PreviewData

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onOpenSources: (SearchResult) -> Unit = {},
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Текущий язык интерфейса (после переключения экран пересоздаётся с новым)
    val language = LocalConfiguration.current.locales[0].language
    LaunchedEffect(language) { viewModel.onLanguageChange(language) }

    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbar.showSnackbar(context.getString(msg.text, msg.arg))
            viewModel.onMessageShown()
        }
    }

    Box(modifier.fillMaxSize()) {
        SearchContent(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onRetry = viewModel::retry,
            onRequest = viewModel::askRequest,
            onOpen = onOpenSources,
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    state.confirm?.let { item ->
        RequestDialog(item, onConfirm = viewModel::confirmRequest, onDismiss = viewModel::dismissRequest)
    }
}

@Composable
fun SearchContent(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onRequest: (SearchResult) -> Unit,
    modifier: Modifier = Modifier,
    onOpen: (SearchResult) -> Unit = {},
) {
    Column(modifier.fillMaxSize()) {
        TopStatusBar(title = stringResource(R.string.search_title))
        TuiSearchField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.search_hint),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(8.dp))
        when (val results = state.results) {
            null -> EmptyState(stringResource(R.string.search_idle), hint = stringResource(R.string.search_idle_hint))
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(stringResource(results.message), onRetry)
            is UiState.Success -> if (results.data.isEmpty()) {
                EmptyState(stringResource(R.string.search_nothing, state.query.trim()))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(results.data, key = { "${it.type}-${it.tmdbId}" }) { item ->
                        SearchResultRow(item, onRequest = { onRequest(item) }, onOpen = { onOpen(item) })
                    }
                }
            }
        }
    }
}

// Постер там, где выбирают: маленький постер + название, мета и статус
@Composable
private fun SearchResultRow(item: SearchResult, onRequest: () -> Unit, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Poster(name = item.name, url = item.posterUrl, width = 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val type = stringResource(if (item.type == TitleType.MOVIE) R.string.type_movie else R.string.type_series)
            Text(
                listOfNotNull(item.year?.toString(), type).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.tui.muted,
            )
            Spacer(Modifier.height(2.dp))
            when (item.status) {
                MediaStatus.AVAILABLE -> StatusBadge("✓", stringResource(R.string.status_available), StatusKind.OK)
                MediaStatus.REQUESTED -> StatusBadge("…", stringResource(R.string.status_requested), StatusKind.WAIT)
                MediaStatus.NONE -> TuiButton(stringResource(R.string.action_request), onRequest)
            }
            Text(stringResource(R.string.search_sources_hint), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.tui.muted)
        }
    }
}

// Подтверждение с последствиями, а не «Вы уверены?». Безопасный вариант — «Отмена».
@Composable
private fun RequestDialog(item: SearchResult, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = MaterialTheme.tui.boost,
        title = { Text(stringResource(R.string.request_confirm_title, item.name), style = MaterialTheme.typography.titleMedium) },
        text = { Text(stringResource(R.string.request_confirm_text)) },
        confirmButton = { TuiButton(stringResource(R.string.action_request), onConfirm) },
        dismissButton = { TuiButton(stringResource(R.string.cancel), onDismiss, color = MaterialTheme.tui.muted) },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SearchPreview() {
    HomepanelTheme {
        SearchContent(
            SearchUiState(query = "dune", results = UiState.Success(PreviewData.searchResults)),
            onQueryChange = {}, onRetry = {}, onRequest = {},
        )
    }
}
