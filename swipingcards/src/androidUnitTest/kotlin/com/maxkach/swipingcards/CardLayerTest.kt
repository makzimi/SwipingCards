package com.maxkach.swipingcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CardLayerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `layer state WHEN changed SHOULD preserve base transforms without recomposing caller or card content`() {
        var progress by mutableFloatStateOf(0f)
        var callerCompositions = 0
        var cardContentCompositions = 0
        val scales = mutableListOf<Float>()
        val alphas = mutableSetOf<Float>()
        compose.setContent {
            SideEffect { callerCompositions++ }
            Box(Modifier.size(280.dp).background(Color.Black).testTag("canvas")) {
                SwipingCards(
                    cards = listOf(1),
                    key = { it },
                    cardGeometry = { StackCardGeometry(scale = 0.8f) },
                    modifier = Modifier.fillMaxSize(),
                    cardLayer = {
                        scales += scaleX
                        scaleX *= progress
                        scaleY *= progress
                        alpha *= progress
                        alphas += alpha
                    },
                ) {
                    SideEffect { cardContentCompositions++ }
                    Box(Modifier.fillMaxSize().background(Color.Red))
                }
            }
        }
        compose.waitForIdle()
        assertEquals(setOf(0f), alphas)
        assertTrue(callerCompositions > 0)
        assertTrue(cardContentCompositions > 0)
        val initialCallerCompositions = callerCompositions
        val initialCardContentCompositions = cardContentCompositions

        compose.runOnIdle { progress = 0.5f }
        compose.waitForIdle()
        compose.runOnIdle { progress = 1f }
        compose.waitForIdle()

        assertEquals(setOf(0f, 0.5f, 1f), alphas)
        assertEquals(initialCallerCompositions, callerCompositions)
        assertEquals(initialCardContentCompositions, cardContentCompositions)
        assertTrue(scales.isNotEmpty() && scales.all { it == 0.8f })
    }

    @Test
    fun `swipe WHEN caller order lags SHOULD report internal positions and exiting identity`() {
        val seen = mutableSetOf<CardInfo>()
        compose.setContent {
            SwipingCards(
                cards = listOf(1, 2, 3, 4),
                key = { it },
                maxVisibleCards = 3,
                modifier = Modifier.size(280.dp).testTag("deck"),
                cardLayer = { seen += it },
            ) { Box(Modifier.fillMaxSize()) }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false

        compose.onNodeWithTag("deck").performTouchInput { swipeRight() }
        compose.mainClock.advanceTimeBy(64)
        compose.waitForIdle()

        assertTrue(CardInfo(key = 2, position = 0) in seen)
        assertTrue(CardInfo(key = 4, position = 2) in seen)
        assertTrue(seen.any { it.key == 1 && it.isExiting })
    }

    @Test
    fun `cardLayer WHEN callback is replaced SHOULD use latest closure without resetting order`() {
        var replacement by mutableStateOf(false)
        val latest = mutableSetOf<CardInfo>()
        compose.setContent {
            val callback = replacement
            SwipingCards(
                cards = listOf(1, 2, 3),
                key = { it },
                modifier = Modifier.size(280.dp).testTag("deck"),
                cardLayer = { if (callback) latest += it },
            ) { Box(Modifier.fillMaxSize()) }
        }
        compose.onNodeWithTag("deck").performTouchInput { swipeRight() }
        compose.waitForIdle()

        compose.runOnIdle { replacement = true }
        compose.waitForIdle()

        assertTrue(CardInfo(key = 2, position = 0) in latest)
    }

    @Test
    fun `swipe WHEN disabled then enabled SHOULD commit only the enabled gesture`() {
        var enabled by mutableStateOf(false)
        val swipes = mutableListOf<Int>()
        compose.setContent {
            SwipingCards(
                cards = listOf(1, 2, 3),
                key = { it },
                modifier = Modifier.size(280.dp).testTag("deck"),
                isSwipeEnabled = enabled,
                onSwipe = { swipes += it.card },
            ) { Box(Modifier.fillMaxSize()) }
        }

        compose.onNodeWithTag("deck").performTouchInput { swipeRight() }
        compose.runOnIdle { assertTrue(swipes.isEmpty()); enabled = true }
        compose.onNodeWithTag("deck").performTouchInput { swipeRight() }
        compose.waitForIdle()

        assertEquals(listOf(1), swipes)
    }

    @Test
    fun `disable WHEN dragging SHOULD settle without committing and allow another swipe`() {
        var enabled by mutableStateOf(true)
        var frontX = Float.NaN
        val swipes = mutableListOf<Int>()
        compose.setContent {
            SwipingCards(
                cards = listOf(1, 2, 3),
                key = { it },
                modifier = Modifier.size(280.dp).testTag("deck"),
                isSwipeEnabled = enabled,
                onSwipe = { swipes += it.card },
                cardLayer = { if (it.position == 0) frontX = translationX },
            ) { Box(Modifier.fillMaxSize()) }
        }
        compose.onNodeWithTag("deck").performTouchInput {
            down(center)
            moveBy(Offset(80f, 0f), delayMillis = 100)
        }
        compose.waitForIdle()
        assertTrue(frontX > 0f)

        compose.runOnIdle { enabled = false }
        compose.onNodeWithTag("deck").performTouchInput { up() }
        compose.waitForIdle()

        assertTrue(swipes.isEmpty())
        assertEquals(0f, frontX, 0.1f)
        compose.runOnIdle { enabled = true }
        compose.onNodeWithTag("deck").performTouchInput { swipeRight() }
        compose.waitForIdle()
        assertEquals(listOf(1), swipes)
    }

}
