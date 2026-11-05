package com.maxkach.swipingcards

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StackGeometryTest {
    @Test
    fun defaults_preserveOriginalGeometryIncludingDeepPositions() {
        val expected = listOf(
            StackCardGeometry(1f, 0f),
            StackCardGeometry(0.92f, -6f),
            StackCardGeometry(0.84f, 4f),
            StackCardGeometry(0.76f, -2f),
            StackCardGeometry(0.7f, 2f),
            StackCardGeometry(0.7f, 2f),
        )
        expected.forEachIndexed { position, geometry ->
            assertEquals(geometry, SwipingCardsDefaults.cardGeometry(position))
        }
        assertFailsWith<IllegalArgumentException> { SwipingCardsDefaults.cardGeometry(-1) }
    }

    @Test
    fun defaultDeck_preservesOriginalTranslationsAndAppearance() {
        val deck = deckWithCards((0..5).toList(), maxVisible = 6)
        (0..5).forEach { position ->
            val original = stackPositionConfig(position)
            val state = deck.getOrCreateCardAnimState(position, position)
            assertEquals(original, deck.configForPosition(position))
            assertEquals(original.scale, state.scale.value)
            assertEquals(original.rotationZ, state.rotationZ.value)
            assertEquals(original.alpha, state.alpha.value)
            assertEquals(
                idleTranslationXPx(position, original.scale, original.rotationZ, deck.cardWidthPx),
                state.translationX.value,
            )
            assertEquals(0f, state.translationY.value)
        }
    }

    @Test
    fun offsets_areDensityIndependentAndAddedToBottomAlignment() {
        val deck = DeckState {
            StackCardGeometry(scale = 0.5f, offsetX = (-12).dp, offsetY = (-20).dp)
        }.apply {
            density = 2f
            cardWidthPx = 200f
            cardHeightPx = 300f
        }
        val state = deck.getOrCreateCardAnimState("rear", 1)
        assertEquals(-74f, state.translationX.value) // -50 px alignment, -24 px offset
        assertEquals(-40f, state.translationY.value)
        val renderedY = bottomAlignmentOffsetY(0.5f, 0.0, 200f, 300f) + state.translationY.value
        assertEquals(35f, renderedY) // 75 px alignment, -40 px offset
    }

    @Test
    fun invalidGeometry_failsBeforeItReachesAnimationState() {
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { scale ->
            assertFailsWith<IllegalArgumentException> { StackCardGeometry(scale = scale) }
        }
        assertFailsWith<IllegalArgumentException> { StackCardGeometry(rotationZ = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { StackCardGeometry(offsetX = Dp.Unspecified) }
        assertFailsWith<IllegalArgumentException> { StackCardGeometry(offsetY = Dp.Infinity) }
    }

    @Test
    fun customGeometry_survivesAFullCycleAndEmitsOneCallbackPerSwipe() = runTest {
        val deck = deckWithCards(listOf("A", "B", "C"), geometry = ::customGeometry)
        val callbacks = mutableListOf<List<Any>>()
        repeat(3) {
            deck.dragOffsetX.snapTo(80f)
            deck.dragOffsetY.snapTo(-25f)
            dismissAndAwait(deck) { callbacks += deck.internalOrder }
            assertRestingGeometry(deck)
        }
        assertEquals<List<List<Any>>>(
            listOf(listOf("B", "C", "A"), listOf("C", "A", "B"), listOf("A", "B", "C")),
            callbacks,
        )
    }

    @Test
    fun hiddenQueue_usesCustomGeometryForEntryPromotionAndExit() = runTest {
        val deck = deckWithCards(listOf("A", "B", "C", "D", "E"), geometry = ::customGeometry)
        val dismissedState = assertNotNull(deck.animStateOf("A"))
        var callbacks = 0
        dismissAndAwait(deck) {
            callbacks++
            // Before promotion starts, the newly visible card is seeded in the deep slot.
            assertAtPosition(deck, assertNotNull(deck.animStateOf("D")), 3)
            assertEquals(listOf<Any>("A"), deck.exitingKeys.toList())
        }
        assertEquals(1, callbacks)
        assertEquals(listOf<Any>("B", "C", "D", "E", "A"), deck.internalOrder)
        assertRestingGeometry(deck)
        assertAtPosition(deck, dismissedState, 3)
        assertEquals(0f, dismissedState.alpha.value)
        assertNull(deck.animStateOf("A"))
        assertTrue(deck.exitingKeys.isEmpty())
    }

    @Test
    fun oneVisibleCard_stillUsesCustomDeepSlot() = runTest {
        val deck = deckWithCards(listOf("A", "B"), maxVisible = 1, geometry = ::customGeometry)
        dismissAndAwait(deck) {
            assertAtPosition(deck, assertNotNull(deck.animStateOf("B")), 1)
        }
        assertEquals(listOf<Any>("B", "A"), deck.internalOrder)
        assertRestingGeometry(deck)
    }

    @Test
    fun externalReorder_reseatsCardsUsingCustomGeometry() {
        val deck = deckWithCards(listOf("A", "B", "C"), geometry = ::customGeometry)
        deck.reconcile(listOf("C", "A", "B"))
        deck.internalOrder.forEachIndexed { position, key ->
            assertAtPosition(deck, deck.getOrCreateCardAnimState(key, position), position)
        }
    }

    @Test
    fun cancelledDrag_keepsCustomRestingOffsets() = runTest {
        val deck = deckWithCards(listOf("A", "B", "C"), geometry = ::customGeometry)
        deck.dragOffsetX.snapTo(20f)
        deck.dragOffsetY.snapTo(-10f)
        deck.isAnimating = true
        withContext(TestFrameClock(testScheduler)) { deck.settleBack() }
        assertEquals(0f, deck.dragOffsetX.value)
        assertEquals(0f, deck.dragOffsetY.value)
        assertRestingGeometry(deck)
    }

    private fun customGeometry(position: Int): StackCardGeometry = when (position) {
        0 -> StackCardGeometry(offsetX = 3.dp, offsetY = 5.dp)
        1 -> StackCardGeometry(0.86f, -10f, (-16).dp, (-40).dp)
        2 -> StackCardGeometry(0.86f, 10f, 16.dp, (-40).dp)
        else -> StackCardGeometry(0.7f, -12f, 20.dp, (-60).dp)
    }

    private fun deckWithCards(
        cards: List<Any>,
        maxVisible: Int = 3,
        geometry: (Int) -> StackCardGeometry = SwipingCardsDefaults::cardGeometry,
    ): DeckState = DeckState(geometry).apply {
        maxVisibleCards = maxVisible
        density = 2f
        containerWidthPx = 560f
        cardWidthPx = 560f
        cardHeightPx = 756f
        reconcile(cards)
        internalOrder.take(visibleCount).forEachIndexed { position, key ->
            getOrCreateCardAnimState(key, position)
        }
    }

    private suspend fun TestScope.dismissAndAwait(deck: DeckState, onCommit: () -> Unit) {
        withContext(TestFrameClock(testScheduler)) {
            deck.performDismiss(deck.internalOrder.first(), this, onCommit)
        }
    }

    private fun assertRestingGeometry(deck: DeckState) {
        deck.internalOrder.take(deck.visibleCount).forEachIndexed { position, key ->
            val state = assertNotNull(deck.animStateOf(key))
            assertAtPosition(deck, state, position)
            assertEquals(stackPositionConfig(position).alpha, state.alpha.value)
        }
    }

    private fun assertAtPosition(deck: DeckState, state: CardAnimState, position: Int) {
        val geometry = customGeometry(position)
        assertEquals(geometry.scale, state.scale.value, 0.0001f)
        assertEquals(geometry.rotationZ, state.rotationZ.value, 0.0001f)
        assertEquals(
            idleTranslationXPx(position, geometry.scale, geometry.rotationZ, deck.cardWidthPx) +
                geometry.offsetX.value * deck.density,
            state.translationX.value,
            0.0001f,
        )
        assertEquals(geometry.offsetY.value * deck.density, state.translationY.value, 0.0001f)
    }

    private class TestFrameClock(private val scheduler: TestCoroutineScheduler) : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            delay(16)
            return onFrame(scheduler.currentTime * 1_000_000L)
        }
    }
}
