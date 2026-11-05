package com.maxkach.swipingcards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
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

// Internal appearance also includes gesture and visibility behavior, which the
// geometry API deliberately leaves unchanged.
internal data class StackPositionConfig(
    val scale: Float,
    val rotationZ: Float,
    val alpha: Float,
    val repulsionFactor: Float,
    val elevation: Dp,
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = 0.dp,
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
internal class CardAnimState(config: StackPositionConfig, initialXDp: Float, initialYDp: Float = 0f) {
    val scale = Animatable(config.scale)
    val rotationZ = Animatable(config.rotationZ)
    // Convert to pixels when drawing so density changes do not invalidate animation state.
    val translationXDp = Animatable(initialXDp)
    val translationYDp = Animatable(initialYDp)
    val alpha = Animatable(config.alpha)

    // Pointer deltas are local to the scaled/rotated card; translations use deck axes.
    fun toDeckOffset(offset: Offset): Offset {
        val radians = rotationZ.value * DEG_TO_RAD
        val cosR = cos(radians).toFloat()
        val sinR = sin(radians).toFloat()
        return Offset(
            x = (offset.x * cosR - offset.y * sinR) * scale.value,
            y = (offset.x * sinR + offset.y * cosR) * scale.value,
        )
    }
}

/**
 * Optimistic, key-addressed state for the deck. Holds the internal order (which may
 * be ahead of the caller's list after a swipe) and the per-card animation state.
 */
@Stable
internal class DeckState(
    private val cardGeometry: (Int) -> StackCardGeometry = SwipingCardsDefaults::cardGeometry,
) {

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
    var density by mutableFloatStateOf(1f)

    val dragOffsetX = Animatable(0f)
    val dragOffsetY = Animatable(0f)
    val residualRotationY = Animatable(0f)

    private val cardAnimStates = mutableMapOf<Any, CardAnimState>()

    // Cards that have rotated out of the visible window and are animating their exit
    // (sinking to the back + fading). Rendered as extra layers behind the stack until
    // their exit animation finishes. A generation token per key lets a stale exit's
    // cleanup no-op if the same card was reclaimed back into the visible window.
    val exitingKeys = mutableStateListOf<Any>()
    private val exitTokens = mutableMapOf<Any, Int>()
    private var nextExitToken = 0

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

    // Resolve every position through the same provider, including hidden entry/exit slots.
    fun configForPosition(position: Int): StackPositionConfig {
        val geometry = cardGeometry(position)
        return stackPositionConfig(position).copy(
            scale = geometry.scale,
            rotationZ = geometry.rotationZ,
            offsetX = geometry.offsetX,
            offsetY = geometry.offsetY,
        )
    }

    private fun idleXDp(position: Int, config: StackPositionConfig): Float =
        idleTranslationXPx(position, config.scale, config.rotationZ, cardWidthPx) / density +
            config.offsetX.value

    private fun idleYDp(config: StackPositionConfig): Float = config.offsetY.value

    fun getOrCreateCardAnimState(key: Any, stackPosition: Int): CardAnimState =
        cardAnimStates.getOrPut(key) {
            val config = configForPosition(stackPosition)
            val xDp = idleXDp(stackPosition, config)
            CardAnimState(config, xDp, idleYDp(config))
        }

    /** Animation state for a card currently rendered as an exiting layer, or null. */
    fun animStateOf(key: Any): CardAnimState? = cardAnimStates[key]

    // Marks [key] as exiting and returns a generation token for its cleanup to match.
    private fun startExit(key: Any): Int {
        val token = ++nextExitToken
        exitTokens[key] = token
        if (key !in exitingKeys) exitingKeys.add(key)
        return token
    }

    // Cancels an in-progress exit for [key] (it is re-entering the visible window),
    // so the running exit's cleanup will no-op and its anim state is reused.
    private fun reclaimIfExiting(key: Any) {
        if (exitTokens.remove(key) != null) exitingKeys.remove(key)
    }

    // Drops the exiting layer once its animation settles — unless a newer exit or a
    // reclaim has already superseded this generation.
    private fun endExit(key: Any, token: Int) {
        if (exitTokens[key] == token) {
            exitTokens.remove(key)
            exitingKeys.remove(key)
            cardAnimStates.remove(key)
        }
    }

    /**
     * Reconciles the incoming external key order into the internal order. Equal orders
     * are a confirmation and leave in-flight animations untouched; a genuinely different
     * order is adopted, and animation state for removed keys is pruned.
     */
    fun reconcile(externalKeys: List<Any>) {
        val previousOrder = internalOrder
        val result = DeckReconciler.reconcile(previousOrder, externalKeys)
        if (result.removed.isNotEmpty()) {
            result.removed.forEach { key ->
                cardAnimStates.remove(key)
                exitTokens.remove(key)
                exitingKeys.remove(key)
            }
        }
        internalOrder = result.newOrder

        // A genuinely different order re-lays-out the deck. A same-order confirmation
        // (newOrder == previousOrder) is skipped so an in-flight swipe animation is
        // never reset.
        if (result.newOrder != previousOrder) {
            reseatAfterReorder(previousOrder)
        }
    }

    // After an external reorder, drop stale per-card visual state for any visible card
    // whose stack position changed; getOrCreateCardAnimState re-creates it at the new
    // position (with the measured card width) during the following render. Cards that
    // kept their position retain their state, so nothing resets unnecessarily.
    private fun reseatAfterReorder(previousOrder: List<Any>) {
        // An external reorder supersedes any in-progress exit animations.
        if (exitingKeys.isNotEmpty()) {
            exitingKeys.toList().forEach { cardAnimStates.remove(it) }
            exitingKeys.clear()
            exitTokens.clear()
        }
        val count = visibleCount
        internalOrder.take(count).forEachIndexed { position, key ->
            if (previousOrder.indexOf(key) != position) {
                cardAnimStates.remove(key)
            }
        }
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
        dismissedState.translationXDp.snapTo(
            dismissedState.translationXDp.value + dragOffsetX.value / density,
        )
        dismissedState.translationYDp.snapTo(
            dismissedState.translationYDp.value + dragOffsetY.value / density,
        )

        // 2. Fold repulsion offsets into background cards.
        for (pos in 1 until visibleCount) {
            val backgroundKey = internalOrder[pos]
            val backgroundState = cardAnimStates[backgroundKey] ?: continue
            val repulsionFactor = configForPosition(pos).repulsionFactor
            backgroundState.translationXDp.snapTo(
                backgroundState.translationXDp.value + backgroundRepulsionX * repulsionFactor / density,
            )
            backgroundState.translationYDp.snapTo(
                backgroundState.translationYDp.value + backgroundRepulsionY * repulsionFactor / density,
            )
        }

        // 3. Transfer container rotation to residual so it unwinds smoothly.
        residualRotationY.snapTo(stackRotationY)

        // 4. Reset shared drag state (derived rotation/repulsion snap to 0).
        dragOffsetX.snapTo(0f)
        dragOffsetY.snapTo(0f)

        // 5. Rotate the deck: front card to the back. When there are more cards than
        //    the visible window, the dismissed card leaves the window and must keep
        //    animating as an "exiting" layer rather than being unmounted immediately.
        val hasHiddenQueue = internalOrder.size > maxVisibleCards
        val newOrder = DeckReconciler.rotate(internalOrder)
        val promoteCount = DeckReconciler.visibleCount(newOrder.size, maxVisibleCards)
        val visibleKeys = newOrder.take(promoteCount)

        // Any card re-entering the visible window cannot also be mid-exit — reclaim it
        // so its (possibly fading) state is reused and the stale exit's cleanup no-ops.
        visibleKeys.forEach(::reclaimIfExiting)

        var exitToken = -1
        if (hasHiddenQueue) {
            exitToken = startExit(dismissedKey)
            // Seed the newly revealed rear card one slot deeper so it rises into place.
            val enteringKey = visibleKeys.last()
            if (cardAnimStates[enteringKey] == null) {
                val deep = configForPosition(promoteCount)
                val deepXDp = idleXDp(promoteCount, deep)
                cardAnimStates[enteringKey] = CardAnimState(deep, deepXDp, idleYDp(deep))
            }
        }

        internalOrder = newOrder

        // 6. Unlock gestures + emit the committed-swipe callback (exactly once).
        onGestureUnlock()

        // 7. Animate everything to rest (non-blocking).
        val promoteSpring = spring<Float>(
            dampingRatio = PROMOTE_SPRING_DAMPING,
            stiffness = PROMOTE_SPRING_STIFFNESS,
        )

        // The exiting card sinks toward the rear and fades — in its own scope, touching
        // only its own animatables, so a rapid follow-up swipe can't cancel it. The
        // finally clause always drops the layer, even if a reclaim supersedes it.
        if (hasHiddenQueue) {
            coroutineScope.launch {
                try {
                    val exitConfig = configForPosition(promoteCount)
                    val exitXDp = idleXDp(promoteCount, exitConfig)
                    coroutineScope {
                        launch { dismissedState.scale.animateTo(exitConfig.scale, promoteSpring) }
                        launch { dismissedState.rotationZ.animateTo(exitConfig.rotationZ, promoteSpring) }
                        launch { dismissedState.translationXDp.animateTo(exitXDp, promoteSpring) }
                        launch { dismissedState.translationYDp.animateTo(idleYDp(exitConfig), promoteSpring) }
                        launch { dismissedState.alpha.animateTo(0f, promoteSpring) }
                    }
                } finally {
                    endExit(dismissedKey, exitToken)
                }
            }
        }

        // The visible window (and container rotation) promote to idle. Shared state here
        // may be cancelled and restarted by the next swipe — that is expected.
        coroutineScope.launch {
            coroutineScope {
                launch { residualRotationY.animateTo(0f, promoteSpring) }

                for ((newPos, cardKey) in visibleKeys.withIndex()) {
                    val state = cardAnimStates[cardKey] ?: continue
                    val config = configForPosition(newPos)
                    val targetXDp = idleXDp(newPos, config)
                    launch { state.scale.animateTo(config.scale, promoteSpring) }
                    launch { state.rotationZ.animateTo(config.rotationZ, promoteSpring) }
                    launch { state.translationXDp.animateTo(targetXDp, promoteSpring) }
                    launch { state.translationYDp.animateTo(idleYDp(config), promoteSpring) }
                    launch { state.alpha.animateTo(config.alpha, promoteSpring) }
                }
            }
        }
    }
}
