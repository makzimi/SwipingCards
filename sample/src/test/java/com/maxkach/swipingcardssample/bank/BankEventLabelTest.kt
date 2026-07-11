package com.maxkach.swipingcardssample.bank

import com.maxkach.swipingcards.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class BankEventLabelTest {
    @Test fun right_selects_by_last4() =
        assertEquals("Selected card ending 4242",
            bankEventLabel(SwipeDirection.Right, "Aurora Everyday", "4242"))

    @Test fun up_selects_by_last4() =
        assertEquals("Selected card ending 4242",
            bankEventLabel(SwipeDirection.Up, "Aurora Everyday", "4242"))

    @Test fun left_skips_by_product() =
        assertEquals("Skipped Zephyr Travel",
            bankEventLabel(SwipeDirection.Left, "Zephyr Travel", "8817"))

    @Test fun down_skips_by_product() =
        assertEquals("Skipped Zephyr Travel",
            bankEventLabel(SwipeDirection.Down, "Zephyr Travel", "8817"))
}
