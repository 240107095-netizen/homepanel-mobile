package com.example.homepanel.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homepanel.R
import com.example.homepanel.data.model.Title
import com.example.homepanel.ui.UiState
import com.example.homepanel.ui.components.EmptyState
import com.example.homepanel.ui.components.ErrorState
import com.example.homepanel.ui.components.LoadingState
import com.example.homepanel.ui.components.TitleRow
import com.example.homepanel.ui.components.TopStatusBar
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
    LibraryContent(state = state, onRetry = viewModel::load, onTitleClick = onTitleClick, modifier = modifier)
}

// Только отображение: получает данные и лямбды, ничего не знает о сети
@Composable
fun LibraryContent(
    state: UiState<List<Title>>,
    onRetry: () -> Unit,
    onTitleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        TopStatusBar(title = stringResource(R.string.library_title)) {
            if (state is UiState.Success) {
                Text(
                    stringResource(R.string.library_count, state.data.size),
                    color = MaterialTheme.tui.muted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        when (state) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(stringResource(state.message), onRetry)
            is UiState.Success -> if (state.data.isEmpty()) {
                EmptyState(stringResource(R.string.library_empty), hint = stringResource(R.string.library_empty_hint))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.data, key = { it.id }) { title ->
                        TitleRow(title, onClick = { onTitleClick(title.id) })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LibraryLoadingPreview() {
    HomepanelTheme { LibraryContent(UiState.Loading, onRetry = {}, onTitleClick = {}) }
}
