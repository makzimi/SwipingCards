package com.maxkach.swipingcardssample.dnd

import com.maxkach.swipingcards.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class DndEventLabelTest {
    @Test fun right_recruits() =
        assertEquals("Recruited Lila Underbough",
            dndEventLabel(SwipeDirection.Right, "Lila Underbough"))

    @Test fun up_recruits() =
        assertEquals("Recruited Lila Underbough",
            dndEventLabel(SwipeDirection.Up, "Lila Underbough"))

    @Test fun left_rejects() =
        assertEquals("Rejected Gruk the Unbroken",
            dndEventLabel(SwipeDirection.Left, "Gruk the Unbroken"))

    @Test fun down_rejects() =
        assertEquals("Rejected Gruk the Unbroken",
            dndEventLabel(SwipeDirection.Down, "Gruk the Unbroken"))
}
