package com.maxkach.swipingcardssample.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val MAX_HISTORY = 5

/** Prepend [entry] (newest-first) and cap the list at [max], dropping the oldest. */
fun appendCapped(current: List<String>, entry: String, max: Int = MAX_HISTORY): List<String> =
    (listOf(entry) + current).take(max)

/** Holds the recent swipe descriptions for one example screen. Not persisted. */
@Stable
class EventHistoryState {
    var entries by mutableStateOf<List<String>>(emptyList())
        private set

    fun record(entry: String) {
        entries = appendCapped(entries, entry)
    }
}

@Composable
fun rememberEventHistoryState(): EventHistoryState = remember { EventHistoryState() }

/** Secondary, muted list of the most recent swipe outcomes. */
@Composable
fun EventHistoryView(
    state: EventHistoryState,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        if (state.entries.isEmpty()) {
            Text(
                text = "Swipe a card to begin",
                style = MaterialTheme.typography.bodySmall,
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            state.entries.forEachIndexed { index, entry ->
                Text(
                    text = entry,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = if (index == 0) 1f else 0.6f),
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}
