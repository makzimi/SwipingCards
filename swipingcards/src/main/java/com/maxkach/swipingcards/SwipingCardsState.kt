package com.maxkach.swipingcards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// NOTE: The key-orthogonal helpers formerly here (StackPositionConfig, stackPositionConfig,
// idleTranslationXPx, bottomAlignmentOffsetY, CardAnimState) moved verbatim to DeckState.kt as
// part of porting this machinery to the key-addressed DeckState holder. They are referenced
// unqualified below and resolve to the DeckState.kt copies (identical signatures, same package).
// This class itself remains until SwipingCards.kt is re-pointed at DeckState in a later task.

internal const val COMPOSED_CARDS = 4
private const val BACKGROUND_VERTICAL_REPULSION_FACTOR = 0.35f
private const val BACKGROUND_HORIZONTAL_REPULSION_FACTOR = 0.25f

private const val SETTLE_SPRING_DAMPING = 0.75f
private const val SETTLE_SPRING_STIFFNESS = 300f
private const val PROMOTE_SPRING_DAMPING = 0.7f
private const val PROMOTE_SPRING_STIFFNESS = 80f

@Stable
class SwipingCardsState internal constructor(
    itemCount: Int,
    val maxRotationY: Float,
    val swipeThresholdFraction: Float,
) {

    var indexOrder by mutableStateOf((0 until itemCount).toList())
        internal set

    var isAnimating by mutableStateOf(false)
        internal set

    var hasPassedThreshold by mutableStateOf(false)
        internal set

    internal val dragOffsetX = Animatable(0f)
    internal val dragOffsetY = Animatable(0f)
    internal val residualRotationY = Animatable(0f)

    // Layout-dependent values — set by the composable during composition
    internal var containerWidthPx by mutableFloatStateOf(0f)
    internal var cardWidthPx by mutableFloatStateOf(0f)

    internal val cardAnimStates = mutableMapOf<Int, CardAnimState>()

    val dragProgress: Float
        get() = if (containerWidthPx > 0f) {
            (dragOffsetX.value / (containerWidthPx * 0.5f)).coerceIn(-1f, 1f)
        } else {
            0f
        }

    internal val stackRotationY: Float
        get() = dragProgress * maxRotationY

    internal val backgroundRepulsionX: Float
        get() = -dragOffsetX.value * BACKGROUND_HORIZONTAL_REPULSION_FACTOR

    internal val backgroundRepulsionY: Float
        get() = -dragOffsetY.value * BACKGROUND_VERTICAL_REPULSION_FACTOR

    internal val visibleCount: Int
        get() = minOf(indexOrder.size, COMPOSED_CARDS)

    internal fun getOrCreateCardAnimState(
        cardIndex: Int,
        stackPosition: Int,
    ): CardAnimState = cardAnimStates.getOrPut(cardIndex) {
        val config = stackPositionConfig(stackPosition)
        val xPx = idleTranslationXPx(stackPosition, config.scale, config.rotationZ, cardWidthPx)
        CardAnimState(config, xPx)
    }

    internal suspend fun settleBack() {
        val settleSpring = spring<Float>(
            dampingRatio = SETTLE_SPRING_DAMPING,
            stiffness = SETTLE_SPRING_STIFFNESS
        )
        coroutineScope {
            launch { dragOffsetX.animateTo(0f, settleSpring) }
            launch { dragOffsetY.animateTo(0f, settleSpring) }
        }
        hasPassedThreshold = false
        isAnimating = false
    }

    internal suspend fun performDismiss(
        dismissedIndex: Int,
        coroutineScope: CoroutineScope,
        onGestureUnlock: () -> Unit,
    ) {
        // 1. Capture dismissed card's total visual X/Y (idle offset + drag offset)
        val dismissedState = cardAnimStates[dismissedIndex]!!
        dismissedState.translationX.snapTo(dismissedState.translationX.value + dragOffsetX.value)
        dismissedState.translationY.snapTo(dismissedState.translationY.value + dragOffsetY.value)

        // 2. Capture repulsion offsets for background cards
        for (pos in 1 until visibleCount) {
            val backgroundIndex = indexOrder[pos]
            val backgroundState = cardAnimStates[backgroundIndex] ?: continue
            val repulsionFactor = stackPositionConfig(pos).repulsionFactor
            backgroundState.translationX.snapTo(backgroundState.translationX.value + backgroundRepulsionX * repulsionFactor)
            backgroundState.translationY.snapTo(backgroundState.translationY.value + backgroundRepulsionY * repulsionFactor)
        }

        // 3. Transfer container rotation to residual so it unwinds smoothly
        residualRotationY.snapTo(stackRotationY)

        // 4. Reset shared drag state (derived rotation/repulsion snap to 0)
        dragOffsetX.snapTo(0f)
        dragOffsetY.snapTo(0f)

        // 5. Reorder the deck
        val newOrder = indexOrder.drop(1) + dismissedIndex
        indexOrder = newOrder

        // 6. Unlock gestures — drag offsets are independent from animState,
        //    so the user can start a new drag while the settle animation runs.
        onGestureUnlock()

        // 7. Animate ALL cards (including new top) to idle positions (non-blocking)
        val promoteSpring = spring<Float>(
            dampingRatio = PROMOTE_SPRING_DAMPING,
            stiffness = PROMOTE_SPRING_STIFFNESS
        )

        coroutineScope.launch {
            coroutineScope {
                launch { residualRotationY.animateTo(0f, promoteSpring) }

                for ((newPos, cardIndex) in newOrder.take(COMPOSED_CARDS).withIndex()) {
                    val state = cardAnimStates[cardIndex] ?: continue
                    val config = stackPositionConfig(newPos)
                    val targetXPx = idleTranslationXPx(
                        newPos, config.scale, config.rotationZ, cardWidthPx
                    )
                    launch { state.scale.animateTo(config.scale, promoteSpring) }
                    launch { state.rotationZ.animateTo(config.rotationZ, promoteSpring) }
                    launch { state.translationX.animateTo(targetXPx, promoteSpring) }
                    launch { state.translationY.animateTo(0f, promoteSpring) }
                    launch { state.alpha.animateTo(config.alpha, promoteSpring) }
                }
            }
        }
    }
}

@Composable
fun rememberSwipingCardsState(
    itemCount: Int,
    maxRotationY: Float = 38f,
    swipeThresholdFraction: Float = 0.20f,
): SwipingCardsState {
    return remember { SwipingCardsState(itemCount, maxRotationY, swipeThresholdFraction) }
}
