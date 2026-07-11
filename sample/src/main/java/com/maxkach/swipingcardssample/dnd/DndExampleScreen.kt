package com.maxkach.swipingcardssample.dnd

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maxkach.swipingcards.SwipingCards
import com.maxkach.swipingcardssample.common.EventHistoryView
import com.maxkach.swipingcardssample.common.rememberEventHistoryState
import com.maxkach.swipingcardssample.gallery.ExampleScaffold

@Composable
fun DndExampleScreen(onBack: () -> Unit) {
    val history = rememberEventHistoryState()
    ExampleScaffold(title = "D&D", onBack = onBack) { modifier ->
        Box(modifier, contentAlignment = Alignment.Center) {
            SwipingCards(
                cards = dndCards,
                key = { it.name },
                modifier = Modifier.fillMaxWidth(0.82f).aspectRatio(4f / 5f),
                onSwipe = { result -> history.record(dndEventLabel(result.direction, result.card.name)) },
            ) { card -> DndCardView(card) }
        }
        EventHistoryView(history, Modifier.padding(bottom = 16.dp))
    }
}
