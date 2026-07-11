package com.maxkach.swipingcardssample.common

import org.junit.Assert.assertEquals
import org.junit.Test

class EventHistoryTest {
    @Test fun newest_entry_is_first() {
        val r = appendCapped(listOf("a"), "b")
        assertEquals(listOf("b", "a"), r)
    }

    @Test fun caps_at_max_dropping_oldest() {
        val start = listOf("d", "c", "b", "a", "z") // already 5, newest-first
        val r = appendCapped(start, "e", max = 5)
        assertEquals(listOf("e", "d", "c", "b", "a"), r)
    }

    @Test fun empty_start_yields_single_entry() {
        assertEquals(listOf("only"), appendCapped(emptyList(), "only"))
    }
}
