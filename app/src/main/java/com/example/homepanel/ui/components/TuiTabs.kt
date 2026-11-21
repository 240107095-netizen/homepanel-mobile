package com.example.homepanel.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.homepanel.ui.theme.tui

// Переключатель в строку: «[All] Movies · Series». Выбранное — в скобках и акцентом.
@Composable
fun TuiTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.padding(horizontal = 8.dp)) {
        labels.forEachIndexed { i, label ->
            val isSelected = i == selected
            Text(
                text = if (isSelected) "[$label]" else " $label ",
                style = if (isSelected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.tui.muted,
                modifier = Modifier
                    .semantics { this.selected = isSelected }
                    .clickable(role = Role.Tab) { onSelect(i) }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
    }
}
