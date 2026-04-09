package com.maxkach.swipingcards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal const val COMPOSED_CARDS = 4
private const val BACKGROUND_VERTICAL_REPULSION_FACTOR = 0.35f
private const val BACKGROUND_HORIZONTAL_REPULSION_FACTOR = 0.25f

private const val SETTLE_SPRING_DAMPING = 0.75f
private const val SETTLE_SPRING_STIFFNESS = 300f
private const val PROMOTE_SPRING_DAMPING = 0.7f
private const val PROMOTE_SPRING_STIFFNESS = 80f

// All visual properties for a card at a given stack position — single source of truth
internal data class StackPositionConfig(
    val scale: Float,
    val rotationZ: Float,
    val alpha: Float,
    val repulsionFactor: Float,
    val elevation: Dp,
)

internal fun stackPositionConfig(position: Int): StackPositionConfig = when (position) {
    0 -> StackPositionConfig(scale = 1.0f, rotationZ = 0f, alpha = 1f, repulsionFactor = 0f, elevation = 12.dp)
    1 -> StackPositionConfig(scale = 0.92f, rotationZ = -6f, alpha = 1f, repulsionFactor = 1.0f, elevation = 8.dp)
    2 -> StackPositionConfig(scale = 0.84f, rotationZ = 4f, alpha = 1f, repulsionFactor = 0.6f, elevation = 4.dp)
    3 -> StackPositionConfig(scale = 0.76f, rotationZ = -2f, alpha = 1f, repulsionFactor = 0.3f, elevation = 2.dp)
    else -> StackPositionConfig(scale = 0.7f, rotationZ = 2f, alpha = 0.8f, repulsionFactor = 0.3f, elevation = 2.dp)
}

// Compute X so background card's bottom corner aligns with card 0's bottom corner.
// Odd positions (1, 3) align left-bottom; even positions (2) align right-bottom.
internal fun idleTranslationXPx(
    position: Int,
    scale: Float,
    rotationZDeg: Float,
    cardWidthPx: Float,
): Float {
    if (position == 0) return 0f
    val rotRad = Math.toRadians(rotationZDeg.toDouble())
    val cosR = cos(rotRad).toFloat()
    val halfW = cardWidthPx / 2f
    return if (position % 2 == 1) {
        // Left-bottom corner of scaled+rotated card → card 0's left-bottom
        -halfW + (halfW * scale) * cosR
    } else {
        // Right-bottom corner of scaled+rotated card → card 0's right-bottom
        halfW - (halfW * scale) * cosR
    }
}

/**
 * Compute the Y offset needed to align a scaled+rotated card's bottom edge
 * with the unrotated top card's bottom edge. A rotated card has a corner
 * that dips below the unrotated bottom — this accounts for that.
 */
internal fun bottomAlignmentOffsetY(
    scale: Float,
    rotationZRad: Double,
    cardWidthPx: Float,
    cardHeightPx: Float,
): Float {
    val bottomExtent =
        (cardWidthPx * scale / 2f) * sin(rotationZRad).toFloat() +
        (cardHeightPx * scale / 2f) * cos(rotationZRad).toFloat()
    val topCardBottom = cardHeightPx / 2f
    return topCardBottom - bottomExtent
}

// Per-card animated properties — each item tracks its own visual state
@Stable
internal class CardAnimState(config: StackPositionConfig, initialXPx: Float) {
    val scale = Animatable(config.scale)
    val rotationZ = Animatable(config.rotationZ)
    val translationX = Animatable(initialXPx)
    val translationY = Animatable(0f)
    val alpha = Animatable(config.alpha)
}

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

    // Derived states
    val dragProgress by derivedStateOf {
        if (containerWidthPx > 0f)
            (dragOffsetX.value / (containerWidthPx * 0.5f)).coerceIn(-1f, 1f)
        else 0f
    }

    internal val stackRotationY by derivedStateOf { dragProgress * maxRotationY }

    internal val backgroundRepulsionX by derivedStateOf {
        -dragOffsetX.value * BACKGROUND_HORIZONTAL_REPULSION_FACTOR
    }

    internal val backgroundRepulsionY by derivedStateOf {
        -dragOffsetY.value * BACKGROUND_VERTICAL_REPULSION_FACTOR
    }

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
