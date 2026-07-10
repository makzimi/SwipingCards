package com.maxkach.swipingcardssample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.maxkach.swipingcards.SwipingCards
import androidx.compose.foundation.layout.Box

@Composable
fun SwipingCardsExampleScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        SwipingCards(
            cards = sampleIdeas,
            key = { idea -> idea.title },
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .aspectRatio(3f / 4f),
            maxVisibleCards = 4,
        ) { idea ->
            IdeaCardView(idea)
        }
    }
}
