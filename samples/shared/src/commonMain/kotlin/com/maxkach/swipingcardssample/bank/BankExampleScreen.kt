package com.maxkach.swipingcardssample.bank

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maxkach.swipingcards.SwipingCards
import com.maxkach.swipingcards.SwipingCardsDefaults
import com.maxkach.swipingcardssample.common.EventHistoryView
import com.maxkach.swipingcardssample.common.rememberEventHistoryState
import com.maxkach.swipingcardssample.gallery.ExampleScaffold

@Composable
fun BankExampleScreen(onBack: () -> Unit) {
    val history = rememberEventHistoryState()
    var wideFan by remember { mutableStateOf(false) }
    ExampleScaffold(title = "Bank cards", onBack = onBack) { modifier ->
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Custom fan (switching resets the deck)", Modifier.weight(1f))
            Switch(checked = wideFan, onCheckedChange = { wideFan = it })
        }
        Box(modifier, contentAlignment = Alignment.Center) {
            key(wideFan) {
                SwipingCards(
                    cards = bankCards,
                    key = { it.last4 },
                    cardGeometry = { position ->
                        val default = SwipingCardsDefaults.cardGeometry(position)
                        if (wideFan && position in 1..2) {
                            val side = if (position == 1) -1 else 1
                            default.copy(
                                scale = 0.86f,
                                rotationZ = side * 10f,
                                offsetX = (side * 12).dp,
                                offsetY = (-36).dp,
                            )
                        } else {
                            default
                        }
                    },
                    modifier = Modifier.fillMaxWidth(if (wideFan) 0.78f else 0.92f).aspectRatio(1.586f),
                    onSwipe = { result ->
                        history.record(bankEventLabel(result.direction, result.card.product, result.card.last4))
                    },
                ) { card -> BankCardView(card) }
            }
        }
        EventHistoryView(history, Modifier.padding(bottom = 16.dp))
    }
}
