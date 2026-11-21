package com.example.homepanel.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homepanel.R
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.components.EmptyState
import com.example.homepanel.ui.components.ErrorState
import com.example.homepanel.ui.components.LoadingState
import com.example.homepanel.ui.components.TitleRow
import com.example.homepanel.ui.components.TopStatusBar
import com.example.homepanel.ui.components.TuiSearchField
import com.example.homepanel.ui.components.TuiTabs
import com.example.homepanel.ui.theme.HomepanelTheme
import com.example.homepanel.ui.theme.tui

// Обёртка с ViewModel: подписывается на state и передаёт его в «чистый» экран
@Composable
fun LibraryScreen(
    onTitleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onRetry = viewModel::load,
        onTitleClick = onTitleClick,
        modifier = modifier,
    )
}

// Только отображение: получает данные и лямбды, ничего не знает о сети
@Composable
fun LibraryContent(
    state: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
    onRetry: () -> Unit,
    onTitleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        TopStatusBar(title = stringResource(R.string.library_title)) {
            if (state.total > 0) {
                Text(
                    stringResource(R.string.library_count, state.total),
                    color = MaterialTheme.tui.muted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        TuiSearchField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.library_search_hint),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )
        TuiTabs(
            labels = listOf(
                stringResource(R.string.filter_all),
                stringResource(R.string.filter_movies),
                stringResource(R.string.filter_series),
            ),
            selected = state.filter.ordinal,
            onSelect = { onFilterChange(LibraryFilter.entries[it]) },
            modifier = Modifier.padding(vertical = 4.dp),
        )
        when (val titles = state.titles) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(stringResource(titles.message), onRetry)
            is UiState.Success -> when {
                titles.data.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                    items(titles.data, key = { it.id }) { title ->
                        TitleRow(title, onClick = { onTitleClick(title.id) })
                    }
                }
                state.total == 0 -> EmptyState(
                    stringResource(R.string.library_empty),
                    hint = stringResource(R.string.library_empty_hint),
                )
                else -> EmptyState(stringResource(R.string.library_not_found, state.query))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LibraryLoadingPreview() {
    HomepanelTheme {
        LibraryContent(LibraryUiState(), onQueryChange = {}, onFilterChange = {}, onRetry = {}, onTitleClick = {})
    }
}
