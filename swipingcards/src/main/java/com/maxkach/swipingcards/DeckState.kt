package com.maxkach.swipingcards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val BACKGROUND_VERTICAL_REPULSION_FACTOR = 0.35f
private const val BACKGROUND_HORIZONTAL_REPULSION_FACTOR = 0.25f

private const val SETTLE_SPRING_DAMPING = 0.75f
private const val SETTLE_SPRING_STIFFNESS = 300f
private const val PROMOTE_SPRING_DAMPING = 0.7f
private const val PROMOTE_SPRING_STIFFNESS = 80f

private const val DEG_TO_RAD = PI / 180.0

// All visual properties for a card at a given stack position — single source of truth.
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

// Compute X so a background card's bottom corner aligns with card 0's bottom corner.
// Odd positions (1, 3) align left-bottom; even positions (2) align right-bottom.
internal fun idleTranslationXPx(
    position: Int,
    scale: Float,
    rotationZDeg: Float,
    cardWidthPx: Float,
): Float {
    if (position == 0) return 0f
    val cosR = cos(rotationZDeg * DEG_TO_RAD).toFloat()
    val halfW = cardWidthPx / 2f
    return if (position % 2 == 1) {
        -halfW + (halfW * scale) * cosR
    } else {
        halfW - (halfW * scale) * cosR
    }
}

/**
 * Compute the Y offset needed to align a scaled+rotated card's bottom edge with the
 * unrotated top card's bottom edge. A rotated card has a corner that dips below the
 * unrotated bottom — this accounts for that.
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

// Per-card animated properties — each key tracks its own visual state.
@Stable
internal class CardAnimState(config: StackPositionConfig, initialXPx: Float) {
    val scale = Animatable(config.scale)
    val rotationZ = Animatable(config.rotationZ)
    val translationX = Animatable(initialXPx)
    val translationY = Animatable(0f)
    val alpha = Animatable(config.alpha)
}

/**
 * Optimistic, key-addressed state for the deck. Holds the internal order (which may
 * be ahead of the caller's list after a swipe) and the per-card animation state.
 */
@Stable
internal class DeckState {

    var internalOrder by mutableStateOf<List<Any>>(emptyList())
        private set

    var isAnimating by mutableStateOf(false)
    var hasPassedThreshold by mutableStateOf(false)

    // Config mirrored from the composable each composition.
    var maxVisibleCards by mutableStateOf(4)
    var maxRotationY by mutableFloatStateOf(38f)
    var swipeThresholdFraction by mutableFloatStateOf(0.20f)

    // Layout-dependent values — set by the composable from measured constraints.
    var containerWidthPx by mutableFloatStateOf(0f)
    var cardWidthPx by mutableFloatStateOf(0f)
    var cardHeightPx by mutableFloatStateOf(0f)

    val dragOffsetX = Animatable(0f)
    val dragOffsetY = Animatable(0f)
    val residualRotationY = Animatable(0f)

    private val cardAnimStates = mutableMapOf<Any, CardAnimState>()

    val visibleCount: Int
        get() = DeckReconciler.visibleCount(internalOrder.size, maxVisibleCards)

    val dragProgress: Float
        get() = if (containerWidthPx > 0f) {
            (dragOffsetX.value / (containerWidthPx * 0.5f)).coerceIn(-1f, 1f)
        } else {
            0f
        }

    val stackRotationY: Float
        get() = dragProgress * maxRotationY

    val backgroundRepulsionX: Float
        get() = -dragOffsetX.value * BACKGROUND_HORIZONTAL_REPULSION_FACTOR

    val backgroundRepulsionY: Float
        get() = -dragOffsetY.value * BACKGROUND_VERTICAL_REPULSION_FACTOR

    fun getOrCreateCardAnimState(key: Any, stackPosition: Int): CardAnimState =
        cardAnimStates.getOrPut(key) {
            val config = stackPositionConfig(stackPosition)
            val xPx = idleTranslationXPx(stackPosition, config.scale, config.rotationZ, cardWidthPx)
            CardAnimState(config, xPx)
        }

    /**
     * Reconciles the incoming external key order into the internal order. Equal orders
     * are a confirmation and leave in-flight animations untouched; a genuinely different
     * order is adopted, and animation state for removed keys is pruned.
     */
    fun reconcile(externalKeys: List<Any>) {
        val result = DeckReconciler.reconcile(internalOrder, externalKeys)
        if (result.removed.isNotEmpty()) {
            result.removed.forEach { cardAnimStates.remove(it) }
        }
        internalOrder = result.newOrder
    }

    suspend fun settleBack() {
        val settleSpring = spring<Float>(
            dampingRatio = SETTLE_SPRING_DAMPING,
            stiffness = SETTLE_SPRING_STIFFNESS,
        )
        coroutineScope {
            launch { dragOffsetX.animateTo(0f, settleSpring) }
            launch { dragOffsetY.animateTo(0f, settleSpring) }
        }
        hasPassedThreshold = false
        isAnimating = false
    }

    suspend fun performDismiss(
        dismissedKey: Any,
        coroutineScope: CoroutineScope,
        onGestureUnlock: () -> Unit,
    ) {
        // 1. Fold the dismissed card's drag offset into its own translation.
        val dismissedState = cardAnimStates[dismissedKey] ?: run {
            // The card vanished (e.g. removed externally mid-gesture) — settle safely.
            dragOffsetX.snapTo(0f)
            dragOffsetY.snapTo(0f)
            residualRotationY.snapTo(0f)
            onGestureUnlock()
            return
        }
        dismissedState.translationX.snapTo(dismissedState.translationX.value + dragOffsetX.value)
        dismissedState.translationY.snapTo(dismissedState.translationY.value + dragOffsetY.value)

        // 2. Fold repulsion offsets into background cards.
        for (pos in 1 until visibleCount) {
            val backgroundKey = internalOrder[pos]
            val backgroundState = cardAnimStates[backgroundKey] ?: continue
            val repulsionFactor = stackPositionConfig(pos).repulsionFactor
            backgroundState.translationX.snapTo(backgroundState.translationX.value + backgroundRepulsionX * repulsionFactor)
            backgroundState.translationY.snapTo(backgroundState.translationY.value + backgroundRepulsionY * repulsionFactor)
        }

        // 3. Transfer container rotation to residual so it unwinds smoothly.
        residualRotationY.snapTo(stackRotationY)

        // 4. Reset shared drag state (derived rotation/repulsion snap to 0).
        dragOffsetX.snapTo(0f)
        dragOffsetY.snapTo(0f)

        // 5. Rotate the deck: front card to the back.
        val newOrder = DeckReconciler.rotate(internalOrder)
        internalOrder = newOrder

        // 6. Unlock gestures + emit the committed-swipe callback (exactly once).
        onGestureUnlock()

        // 7. Animate all visible cards (including the promoted top) to idle (non-blocking).
        val promoteSpring = spring<Float>(
            dampingRatio = PROMOTE_SPRING_DAMPING,
            stiffness = PROMOTE_SPRING_STIFFNESS,
        )
        coroutineScope.launch {
            coroutineScope {
                launch { residualRotationY.animateTo(0f, promoteSpring) }

                val promoteCount = DeckReconciler.visibleCount(newOrder.size, maxVisibleCards)
                for ((newPos, cardKey) in newOrder.take(promoteCount).withIndex()) {
                    val state = cardAnimStates[cardKey] ?: continue
                    val config = stackPositionConfig(newPos)
                    val targetXPx = idleTranslationXPx(newPos, config.scale, config.rotationZ, cardWidthPx)
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
