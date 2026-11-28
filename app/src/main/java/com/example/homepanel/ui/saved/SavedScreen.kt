package com.example.homepanel.ui.saved

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homepanel.R
import com.example.homepanel.data.local.SavedTitle
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
import com.example.homepanel.ui.theme.HomepanelTheme
import com.example.homepanel.ui.theme.tui

@Composable
fun SavedScreen(
    onOpenSources: (SavedTitle) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedViewModel = viewModel(factory = SavedViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbar.showSnackbar(context.getString(msg.text, msg.arg))
            viewModel.onMessageShown()
        }
    }

    Box(modifier.fillMaxSize()) {
        SavedContent(
            state,
            onOpen = onOpenSources,
            onEdit = viewModel::startEdit,
            onToggleWatched = viewModel::toggleWatched,
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    state.editing?.let { item ->
        EditDialog(item, onSave = viewModel::saveEdit, onDismiss = viewModel::dismissEdit)
    }
}

@Composable
fun SavedContent(
    state: SavedUiState,
    onOpen: (SavedTitle) -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (SavedTitle) -> Unit = {},
    onToggleWatched: (SavedTitle) -> Unit = {},
) {
    Column(modifier.fillMaxSize()) {
        TopStatusBar(title = stringResource(R.string.saved_title)) {
            val count = (state.titles as? UiState.Success)?.data?.size ?: 0
            if (count > 0) {
                Text(
                    pluralStringResource(R.plurals.library_count, count, count),
                    color = MaterialTheme.tui.muted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        when (val titles = state.titles) {
            UiState.Loading -> LoadingState()
            // База локальная, ошибка почти невозможна; «Повторить» — просто заново открыть вкладку
            is UiState.Error -> ErrorState(stringResource(titles.message), onRetry = {})
            is UiState.Success -> if (titles.data.isEmpty()) {
                EmptyState(stringResource(R.string.saved_empty), hint = stringResource(R.string.saved_empty_hint))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(titles.data, key = { it.id }) { item ->
                        SavedRow(
                            item,
                            onClick = { onOpen(item) },
                            onEdit = { onEdit(item) },
                            onToggleWatched = { onToggleWatched(item) },
                        )
                    }
                }
            }
        }
    }
}

// Сохранённый тайтл:
//  [постер] Дюна: Пророчество
//           2024 · series
//           ○ to watch
//           «посмотреть с братом»
@Composable
private fun SavedRow(item: SavedTitle, onClick: () -> Unit, onEdit: () -> Unit, onToggleWatched: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            // Тап по отметке переключает её (обратимо — без вопросов)
            val badge = Modifier
                .clickable(onClick = onToggleWatched)
                .padding(vertical = 2.dp)
            if (item.watched) {
                StatusBadge("✓", stringResource(R.string.badge_watched), StatusKind.OK, badge)
            } else {
                StatusBadge("○", stringResource(R.string.status_to_watch), StatusKind.MUTED, badge)
            }
            if (item.note.isNotBlank()) {
                Text(
                    "«${item.note}»",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val editLabel = stringResource(R.string.action_edit)
        Text(
            "✎",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onEdit)
                .semantics { contentDescription = editLabel }
                .padding(12.dp),
        )
    }
}

// Изменение записи: заметка и «просмотрено». «Отмена» ничего не меняет.
@Composable
private fun EditDialog(item: SavedTitle, onSave: (note: String, watched: Boolean) -> Unit, onDismiss: () -> Unit) {
    var note by rememberSaveable(item.id) { mutableStateOf(item.note) }
    var watched by rememberSaveable(item.id) { mutableStateOf(item.watched) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = MaterialTheme.tui.boost,
        title = { Text(stringResource(R.string.saved_edit_title, item.name), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                Text(stringResource(R.string.saved_note), style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(stringResource(R.string.saved_note_hint), color = MaterialTheme.tui.muted) },
                    shape = RectangleShape,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.tui.line,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    (if (watched) "[✓] " else "[ ] ") + stringResource(R.string.badge_watched),
                    color = if (watched) MaterialTheme.tui.success else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { watched = !watched }
                        .padding(vertical = 8.dp),
                )
            }
        },
        confirmButton = { TuiButton(stringResource(R.string.action_save_changes), { onSave(note, watched) }) },
        dismissButton = { TuiButton(stringResource(R.string.cancel), onDismiss, color = MaterialTheme.tui.muted) },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SavedPreview() {
    HomepanelTheme {
        SavedContent(
            SavedUiState(
                UiState.Success(
                    listOf(
                        SavedTitle(1, 90228, TitleType.SERIES, "Дюна: Пророчество", 2024, null, note = "посмотреть с братом"),
                        SavedTitle(2, 438631, TitleType.MOVIE, "Дюна", 2021, null, watched = true),
                    ),
                ),
            ),
            onOpen = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SavedEmptyPreview() {
    HomepanelTheme { SavedContent(SavedUiState(UiState.Success(emptyList())), onOpen = {}) }
}
