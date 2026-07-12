package com.maxkach.swipingcardssample.dating

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
fun DatingExampleScreen(onBack: () -> Unit) {
    val history = rememberEventHistoryState()
    ExampleScaffold(title = "Dating", onBack = onBack) { modifier ->
        Box(modifier, contentAlignment = Alignment.Center) {
            SwipingCards(
                cards = datingProfiles,
                key = { it.name },
                modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(2f / 3f),
                onSwipe = { result -> history.record(datingEventLabel(result.direction, result.card.name)) },
            ) { profile -> DatingCard(profile) }
        }
        EventHistoryView(history, Modifier.padding(bottom = 16.dp))
    }
}
