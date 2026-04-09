package com.maxkach.swipingcardssample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.maxkach.swipingcards.SwipingCards
import com.maxkach.swipingcards.rememberSwipingCardsState

@Composable
fun SwipingCardsExampleScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        val state = rememberSwipingCardsState(itemCount = sampleIdeas.size)
        SwipingCards(
            state = state,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            IdeaCardView(sampleIdeas[index])
        }
    }
}
