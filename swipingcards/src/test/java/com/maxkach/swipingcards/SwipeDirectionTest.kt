package com.maxkach.swipingcards

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeDirectionTest {

    @Test
    fun horizontalDominantResolvesLeftRight() {
        assertEquals(SwipeDirection.Right, resolveSwipeDirection(dx = 100f, dy = 10f))
        assertEquals(SwipeDirection.Left, resolveSwipeDirection(dx = -100f, dy = 10f))
    }

    @Test
    fun verticalDominantResolvesUpDown() {
        assertEquals(SwipeDirection.Down, resolveSwipeDirection(dx = 5f, dy = 80f))
        assertEquals(SwipeDirection.Up, resolveSwipeDirection(dx = 5f, dy = -80f))
    }

    @Test
    fun equalMagnitudeTieResolvesHorizontal() {
        assertEquals(SwipeDirection.Right, resolveSwipeDirection(dx = 50f, dy = 50f))
        assertEquals(SwipeDirection.Left, resolveSwipeDirection(dx = -50f, dy = -50f))
    }
}
