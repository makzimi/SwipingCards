package com.maxkach.swipingcardssample.streaming

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxkach.swipingcards.SwipingCards
import com.maxkach.swipingcardssample.common.EventHistoryView
import com.maxkach.swipingcardssample.common.rememberEventHistoryState
import com.maxkach.swipingcardssample.gallery.ExampleScaffold

@Composable
fun StreamingExampleScreen(onBack: () -> Unit) {
    val history = rememberEventHistoryState()
    ExampleScaffold(
        title = "Shows",
        onBack = onBack,
        containerColor = Color.Black,
        contentColor = Color.White,
        leading = { NetflixMark() },
    ) { modifier ->
        Box(modifier, contentAlignment = Alignment.Center) {
            SwipingCards(
                cards = streamingTitles,
                key = { it.title },
                modifier = Modifier.fillMaxWidth(0.86f).aspectRatio(2f / 3f),
                onSwipe = { result ->
                    history.record(streamingEventLabel(result.direction))
                },
            ) { show -> StreamingCard(show) }
        }
        EventHistoryView(
            history,
            Modifier.padding(bottom = 16.dp),
            color = Color.White.copy(alpha = 0.7f),
            emptyText = "Swipe cards to select a show",
        )
    }
}

/** A stylized red "N" — a clear homage, not the actual Netflix wordmark. */
@Composable
private fun NetflixMark() {
    Text(
        "N",
        color = Color(0xFFE50914),
        fontWeight = FontWeight.Black,
        fontSize = 26.sp,
        modifier = Modifier.padding(end = 10.dp),
    )
}
