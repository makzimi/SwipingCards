package com.maxkach.swipingcardssample.bank

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
fun BankExampleScreen(onBack: () -> Unit) {
    val history = rememberEventHistoryState()
    ExampleScaffold(title = "Bank cards", onBack = onBack) { modifier ->
        Box(modifier, contentAlignment = Alignment.Center) {
            SwipingCards(
                cards = bankCards,
                key = { it.last4 },
                modifier = Modifier.fillMaxWidth(0.92f).aspectRatio(1.586f),
                onSwipe = { result ->
                    history.record(bankEventLabel(result.direction, result.card.product, result.card.last4))
                },
            ) { card -> BankCardView(card) }
        }
        EventHistoryView(history, Modifier.padding(bottom = 16.dp))
    }
}
