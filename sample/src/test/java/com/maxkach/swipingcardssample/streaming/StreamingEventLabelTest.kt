package com.maxkach.swipingcardssample.streaming

import com.maxkach.swipingcards.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamingEventLabelTest {
    @Test fun right_adds_to_my_list() =
        assertEquals("Added Stranger Things to My List",
            streamingEventLabel(SwipeDirection.Right, "Stranger Things"))

    @Test fun up_adds_to_my_list() =
        assertEquals("Added Stranger Things to My List",
            streamingEventLabel(SwipeDirection.Up, "Stranger Things"))

    @Test fun left_skips() =
        assertEquals("Skipped The Crown",
            streamingEventLabel(SwipeDirection.Left, "The Crown"))

    @Test fun down_skips() =
        assertEquals("Skipped The Crown",
            streamingEventLabel(SwipeDirection.Down, "The Crown"))
}
