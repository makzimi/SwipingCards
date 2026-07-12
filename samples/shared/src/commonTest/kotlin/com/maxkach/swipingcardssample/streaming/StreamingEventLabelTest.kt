package com.maxkach.swipingcardssample.streaming

import com.maxkach.swipingcards.SwipeDirection
import kotlin.test.assertEquals
import kotlin.test.Test

class StreamingEventLabelTest {
    @Test fun left_swipe() =
        assertEquals("Swiped left", streamingEventLabel(SwipeDirection.Left))

    @Test fun right_swipe() =
        assertEquals("Swiped right", streamingEventLabel(SwipeDirection.Right))

    @Test fun up_swipe() =
        assertEquals("Swiped up", streamingEventLabel(SwipeDirection.Up))

    @Test fun down_swipe() =
        assertEquals("Swiped down", streamingEventLabel(SwipeDirection.Down))
}
