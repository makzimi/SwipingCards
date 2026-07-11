package com.maxkach.swipingcardssample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxkach.swipingcards.SwipingCards

private val cardCountOptions = listOf(2, 4, 10)

@Composable
fun SwipingCardsExampleScreen() {
    var cardCount by remember { mutableIntStateOf(4) }
    var lastSwipe by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CardCountSelector(
            selected = cardCount,
            onSelect = {
                cardCount = it
                lastSwipe = null
            },
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            SwipingCards(
                cards = sampleIdeas.take(cardCount),
                key = { idea -> idea.title },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(3f / 4f),
                maxVisibleCards = 4,
                onSwipe = { result ->
                    val nextTop = result.resultingOrder.firstOrNull()?.title ?: "—"
                    lastSwipe = "${result.direction} → \"$nextTop\""
                },
            ) { idea ->
                IdeaCardView(idea)
            }
        }

        Text(
            text = lastSwipe?.let { "Last swipe: $it" } ?: "Swipe a card to begin",
            color = Color(0xFF1A1A2E),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun CardCountSelector(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cardCountOptions.forEach { count ->
            val label = "$count cards"
            if (count == selected) {
                Button(onClick = { onSelect(count) }) { Text(label) }
            } else {
                OutlinedButton(onClick = { onSelect(count) }) { Text(label) }
            }
        }
    }
}
