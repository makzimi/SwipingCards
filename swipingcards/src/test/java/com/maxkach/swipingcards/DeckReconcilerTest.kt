package com.maxkach.swipingcards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckReconcilerTest {

    @Test
    fun rotate_movesFrontToBack() {
        assertEquals(listOf<Any>("B", "C", "A"), DeckReconciler.rotate(listOf("A", "B", "C")))
    }

    @Test
    fun rotate_fullLoopReturnsToStart() {
        var order: List<Any> = listOf("A", "B", "C")
        order = DeckReconciler.rotate(order); assertEquals(listOf<Any>("B", "C", "A"), order)
        order = DeckReconciler.rotate(order); assertEquals(listOf<Any>("C", "A", "B"), order)
        order = DeckReconciler.rotate(order); assertEquals(listOf<Any>("A", "B", "C"), order)
    }

    @Test
    fun rotate_emptyAndSingleAreUnchanged() {
        assertEquals(emptyList<Any>(), DeckReconciler.rotate(emptyList()))
        assertEquals(listOf<Any>("A"), DeckReconciler.rotate(listOf("A")))
    }

    @Test
    fun reconcile_equalOrderIsConfirmationWithNoChange() {
        val r = DeckReconciler.reconcile(listOf("B", "C", "A"), listOf("B", "C", "A"))
        assertEquals(listOf<Any>("B", "C", "A"), r.newOrder)
        assertTrue(r.added.isEmpty())
        assertTrue(r.removed.isEmpty())
    }

    @Test
    fun reconcile_partialChangeAdoptsExternalAndTracksDelta() {
        val r = DeckReconciler.reconcile(listOf("B", "C", "A"), listOf("B", "C", "D"))
        assertEquals(listOf<Any>("B", "C", "D"), r.newOrder)
        assertEquals(setOf<Any>("D"), r.added)
        assertEquals(setOf<Any>("A"), r.removed)
    }

    @Test
    fun reconcile_replacementAdoptsExternalEntirely() {
        val r = DeckReconciler.reconcile(listOf("B", "C", "A"), listOf("X", "Y", "Z"))
        assertEquals(listOf<Any>("X", "Y", "Z"), r.newOrder)
        assertEquals(setOf<Any>("X", "Y", "Z"), r.added)
        assertEquals(setOf<Any>("B", "C", "A"), r.removed)
    }

    @Test
    fun reconcile_emptyExternalRemovesEverything() {
        val r = DeckReconciler.reconcile(listOf("A", "B"), emptyList())
        assertEquals(emptyList<Any>(), r.newOrder)
        assertEquals(setOf<Any>("A", "B"), r.removed)
        assertTrue(r.added.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun requireUniqueKeys_throwsOnDuplicate() {
        DeckReconciler.requireUniqueKeys(listOf("A", "B", "A"))
    }

    @Test
    fun requireUniqueKeys_passesWhenUnique() {
        DeckReconciler.requireUniqueKeys(listOf("A", "B", "C"))
    }

    @Test
    fun visibleCount_isMinOfSizeAndMax() {
        assertEquals(0, DeckReconciler.visibleCount(size = 0, maxVisibleCards = 4))
        assertEquals(2, DeckReconciler.visibleCount(size = 2, maxVisibleCards = 4))
        assertEquals(4, DeckReconciler.visibleCount(size = 4, maxVisibleCards = 4))
        assertEquals(4, DeckReconciler.visibleCount(size = 6, maxVisibleCards = 4))
        assertEquals(2, DeckReconciler.visibleCount(size = 6, maxVisibleCards = 2))
    }
}
