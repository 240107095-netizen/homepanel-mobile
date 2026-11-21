package com.example.homepanel.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.homepanel.R
import com.example.homepanel.data.model.Title
import com.example.homepanel.data.model.TitleType
import com.example.homepanel.ui.theme.tui

// Строка тайтла в списке:
//  ▸ Киберпанк: Бегущие по краю        ✓ 10/10
//    2022 · Cyberpunk: Edgerunners · series
@Composable
fun TitleRow(title: Title, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("▸", color = MaterialTheme.tui.muted)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                titleMeta(title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.tui.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        TitleBadge(title)
    }
}

// «2022 · Cyberpunk: Edgerunners · series» — чтобы одноимённые различались
@Composable
fun titleMeta(title: Title): String {
    val type = stringResource(if (title.type == TitleType.MOVIE) R.string.type_movie else R.string.type_series)
    return listOfNotNull(
        title.year.takeIf { it > 0 }?.toString(),
        title.originalName.takeIf { it.isNotBlank() && it != title.name },
        type,
    ).joinToString(" · ")
}

// Прогресс просмотра: ✓ досмотрено, ▶ в процессе, ○ не начато
@Composable
fun TitleBadge(title: Title, modifier: Modifier = Modifier) {
    if (title.type == TitleType.MOVIE) {
        when {
            title.played -> StatusBadge("✓", stringResource(R.string.badge_watched), StatusKind.OK, modifier)
            title.playedPercent > 0 -> StatusBadge("▶", "${title.playedPercent}%", StatusKind.ACTIVE, modifier)
            else -> StatusBadge("○", stringResource(R.string.badge_new), StatusKind.MUTED, modifier)
        }
    } else {
        val watched = title.episodeCount - title.unplayedCount
        val text = "$watched/${title.episodeCount}"
        when {
            title.episodeCount > 0 && title.unplayedCount == 0 -> StatusBadge("✓", text, StatusKind.OK, modifier)
            watched > 0 -> StatusBadge("▶", text, StatusKind.ACTIVE, modifier)
            else -> StatusBadge("○", text, StatusKind.MUTED, modifier)
        }
    }
}
