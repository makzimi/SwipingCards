package com.maxkach.swipingcardssample.common

import com.maxkach.swipingcards.SwipeDirection
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class SwipeSemanticsTest {
    @Test fun right_is_positive() = assertTrue(isPositiveSwipe(SwipeDirection.Right))
    @Test fun up_is_positive() = assertTrue(isPositiveSwipe(SwipeDirection.Up))
    @Test fun left_is_negative() = assertFalse(isPositiveSwipe(SwipeDirection.Left))
    @Test fun down_is_negative() = assertFalse(isPositiveSwipe(SwipeDirection.Down))
}
