package com.maxkach.swipingcards

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

private const val FLING_VELOCITY = 2000f
private const val CAMERA_DISTANCE = 12f
private const val DEG_TO_RAD = PI / 180.0

private interface SwipingCardsScope {
    val cardWidth: Dp
    val cardHeight: Dp
}

private class SwipingCardsScopeImpl(
    override val cardWidth: Dp,
    override val cardHeight: Dp,
) : SwipingCardsScope

/**
 * A swipe-to-cycle card stack of arbitrary size. Cards form an infinite circular
 * queue: swiping the front card sends it to the back. The deck is driven by an
 * external [cards] list reconciled by stable [key]; optimistic swipes are confirmed
 * (not reset) when a matching external update arrives.
 *
 * The deck fills the constraints given by [modifier] — the caller controls dimensions
 * (e.g. `Modifier.fillMaxWidth(0.8f).aspectRatio(3f / 4f)`); no size is hardcoded.
 *
 * @param cards the current external list. May be empty.
 * @param key stable identity for each card; must be unique within [cards].
 * @param maxVisibleCards maximum cards rendered at once (must be >= 1; default 4).
 * @param onSwipe invoked exactly once when a swipe crosses the threshold.
 * @param cardContent renders a single card.
 */
@Composable
fun <T> SwipingCards(
    cards: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    maxVisibleCards: Int = 4,
    maxRotationY: Float = 38f,
    swipeThresholdFraction: Float = 0.20f,
    onSwipe: (SwipeResult<T>) -> Unit = {},
    cardContent: @Composable (T) -> Unit,
) {
    require(maxVisibleCards >= 1) {
        "SwipingCards: maxVisibleCards must be >= 1 but was $maxVisibleCards."
    }

    val externalKeys = remember(cards) {
        cards.map(key).also(DeckReconciler::requireUniqueKeys)
    }
    val cardsByKey = remember(cards) { cards.associateBy(key) }

    val deck = remember { DeckState() }
    deck.maxVisibleCards = maxVisibleCards
    deck.maxRotationY = maxRotationY
    deck.swipeThresholdFraction = swipeThresholdFraction

    // Runs only when the external key order actually changes — a same-order
    // recomposition never reconciles, so optimistic state is never reset.
    remember(externalKeys) { deck.reconcile(externalKeys) }

    if (deck.internalOrder.isEmpty()) return

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val cardWidth = maxWidth
        val cardHeight = maxHeight
        with(LocalDensity.current) {
            deck.containerWidthPx = constraints.maxWidth.toFloat()
            deck.cardWidthPx = cardWidth.toPx()
            deck.cardHeightPx = cardHeight.toPx()
        }

        val scope = remember(cardWidth, cardHeight) {
            SwipingCardsScopeImpl(cardWidth, cardHeight)
        }

        with(scope) {
            RotationContainer(
                deck = deck,
                cardsByKey = cardsByKey,
                onSwipe = onSwipe,
                cardContent = cardContent,
            )
        }
    }
}

@Composable
private fun <T> SwipingCardsScope.RotationContainer(
    deck: DeckState,
    cardsByKey: Map<Any, T>,
    modifier: Modifier = Modifier,
    onSwipe: (SwipeResult<T>) -> Unit,
    cardContent: @Composable (T) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .size(cardWidth, cardHeight)
            .graphicsLayer {
                rotationY = deck.stackRotationY + deck.residualRotationY.value
                cameraDistance = CAMERA_DISTANCE * density
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            },
        contentAlignment = Alignment.Center,
    ) {
        for (i in (deck.visibleCount - 1) downTo 0) {
            val cardKey = deck.internalOrder[i]

            key(cardKey) {
                SwipingCard(
                    stackPosition = i,
                    cardKey = cardKey,
                    deck = deck,
                    cardsByKey = cardsByKey,
                    coroutineScope = coroutineScope,
                    onSwipe = onSwipe,
                    cardContent = cardContent,
                )
            }
        }
    }
}

@Composable
private fun <T> SwipingCardsScope.SwipingCard(
    stackPosition: Int,
    cardKey: Any,
    deck: DeckState,
    cardsByKey: Map<Any, T>,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    onSwipe: (SwipeResult<T>) -> Unit,
    cardContent: @Composable (T) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val animState = deck.getOrCreateCardAnimState(cardKey, stackPosition)
    val positionConfig = stackPositionConfig(stackPosition)
    val card = cardsByKey.getValue(cardKey)

    Box(
        modifier = modifier
            .size(cardWidth, cardHeight)
            .zIndex((deck.visibleCount - stackPosition).toFloat())
            .graphicsLayer {
                scaleX = animState.scale.value
                scaleY = animState.scale.value
                rotationZ = animState.rotationZ.value
                this.alpha = animState.alpha.value

                val bottomAlignY = bottomAlignmentOffsetY(
                    scale = animState.scale.value,
                    rotationZRad = abs(animState.rotationZ.value) * DEG_TO_RAD,
                    cardWidthPx = cardWidth.toPx(),
                    cardHeightPx = cardHeight.toPx(),
                )

                translationX = animState.translationX.value +
                    if (stackPosition == 0) deck.dragOffsetX.value
                    else deck.backgroundRepulsionX * positionConfig.repulsionFactor
                translationY = bottomAlignY +
                    animState.translationY.value +
                    if (stackPosition == 0) deck.dragOffsetY.value
                    else deck.backgroundRepulsionY * positionConfig.repulsionFactor
            }
            .conditionalDragGesture(
                deck = deck,
                cardsByKey = cardsByKey,
                hapticFeedback = hapticFeedback,
                coroutineScope = coroutineScope,
                needToDrag = { stackPosition == 0 },
                onSwipe = onSwipe,
            ),
    ) {
        cardContent(card)
    }
}

private fun <T> Modifier.conditionalDragGesture(
    deck: DeckState,
    cardsByKey: Map<Any, T>,
    hapticFeedback: HapticFeedback,
    coroutineScope: CoroutineScope,
    needToDrag: () -> Boolean,
    onSwipe: (SwipeResult<T>) -> Unit,
): Modifier = then(
    if (needToDrag()) {
        Modifier.cardDragGesture(
            deck = deck,
            cardsByKey = cardsByKey,
            hapticFeedback = hapticFeedback,
            coroutineScope = coroutineScope,
            onSwipe = onSwipe,
        )
    } else {
        Modifier
    },
)

private fun <T> Modifier.cardDragGesture(
    deck: DeckState,
    cardsByKey: Map<Any, T>,
    hapticFeedback: HapticFeedback,
    coroutineScope: CoroutineScope,
    onSwipe: (SwipeResult<T>) -> Unit,
): Modifier = pointerInput(Unit) {
    val velocityTracker = VelocityTracker()
    var gestureBlocked = false

    detectDragGestures(
        onDragStart = {
            gestureBlocked = deck.isAnimating
            if (!gestureBlocked) {
                velocityTracker.resetTracking()
                deck.hasPassedThreshold = false
            }
        },
        onDrag = { change, dragAmount ->
            if (gestureBlocked) return@detectDragGestures
            change.consume()
            velocityTracker.addPosition(change.uptimeMillis, change.position)
            coroutineScope.launch {
                deck.dragOffsetX.snapTo(deck.dragOffsetX.value + dragAmount.x)
            }
            coroutineScope.launch {
                deck.dragOffsetY.snapTo(deck.dragOffsetY.value + dragAmount.y)
            }
            val threshold = deck.containerWidthPx * deck.swipeThresholdFraction
            val distanceFromCenter = sqrt(
                deck.dragOffsetX.value * deck.dragOffsetX.value +
                    deck.dragOffsetY.value * deck.dragOffsetY.value,
            )
            if (distanceFromCenter > threshold && !deck.hasPassedThreshold) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                deck.hasPassedThreshold = true
            }
        },
        onDragEnd = {
            if (gestureBlocked) return@detectDragGestures
            val velocity = velocityTracker.calculateVelocity()
            val threshold = deck.containerWidthPx * deck.swipeThresholdFraction
            val endX = deck.dragOffsetX.value
            val endY = deck.dragOffsetY.value
            val distanceFromCenter = sqrt(endX * endX + endY * endY)
            val velocityMagnitude = sqrt(velocity.x * velocity.x + velocity.y * velocity.y)

            if (distanceFromCenter > threshold || velocityMagnitude > FLING_VELOCITY) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                deck.isAnimating = true
                val dismissedKey = deck.internalOrder.first()
                val swipedCard = cardsByKey.getValue(dismissedKey)
                // Derive direction from the drag vector, falling back to fling velocity
                // when the release happened with a near-zero offset.
                val useVelocity = abs(endX) < 1f && abs(endY) < 1f
                val direction = resolveSwipeDirection(
                    dx = if (useVelocity) velocity.x else endX,
                    dy = if (useVelocity) velocity.y else endY,
                )

                coroutineScope.launch {
                    deck.performDismiss(
                        dismissedKey = dismissedKey,
                        coroutineScope = coroutineScope,
                        onGestureUnlock = {
                            val resultingOrder = deck.internalOrder.map { cardsByKey.getValue(it) }
                            onSwipe(
                                SwipeResult(
                                    card = swipedCard,
                                    key = dismissedKey,
                                    direction = direction,
                                    resultingOrder = resultingOrder,
                                ),
                            )
                            deck.hasPassedThreshold = false
                            deck.isAnimating = false
                        },
                    )
                }
            } else {
                deck.isAnimating = true
                coroutineScope.launch { deck.settleBack() }
            }
        },
        onDragCancel = {
            if (gestureBlocked) return@detectDragGestures
            deck.isAnimating = true
            coroutineScope.launch { deck.settleBack() }
        },
    )
}
