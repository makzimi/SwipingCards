package com.maxkach.swipingcardssample.dating

import com.maxkach.swipingcards.SwipeDirection
import kotlin.test.assertEquals
import kotlin.test.Test

class DatingEventLabelTest {
    @Test fun right_likes() =
        assertEquals("Liked Mira", datingEventLabel(SwipeDirection.Right, "Mira"))

    @Test fun up_likes() =
        assertEquals("Liked Mira", datingEventLabel(SwipeDirection.Up, "Mira"))

    @Test fun left_passes() =
        assertEquals("Passed Rowan", datingEventLabel(SwipeDirection.Left, "Rowan"))

    @Test fun down_passes() =
        assertEquals("Passed Rowan", datingEventLabel(SwipeDirection.Down, "Rowan"))
}
