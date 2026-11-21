package com.example.homepanel.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.ImeAction
import com.example.homepanel.ui.theme.tui

// Поле поиска в рамке, как в TUI. Справа «✗» очищает текст.
@Composable
fun TuiSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        shape = RectangleShape,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(placeholder, color = MaterialTheme.tui.muted) },
        trailingIcon = if (value.isNotEmpty()) {
            { TuiButton("✗", onClick = { onValueChange("") }, color = MaterialTheme.tui.muted) }
        } else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.tui.line,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}
