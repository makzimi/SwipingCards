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
import kotlin.math.abs
import kotlin.math.sqrt

private const val FLING_VELOCITY = 2000f
private const val CARD_WIDTH_FRACTION = 0.75f
private const val CARD_ASPECT_RATIO = 4f / 3f
private const val CARD_MAX_HEIGHT_FRACTION = 0.55f
private const val CAMERA_DISTANCE = 12f

private interface SwipingCardsScope {
    val cardWidth: Dp
    val cardHeight: Dp
}

private class SwipingCardsScopeImpl(
    override val cardWidth: Dp,
    override val cardHeight: Dp,
) : SwipingCardsScope

@Composable
fun SwipingCards(
    state: SwipingCardsState,
    modifier: Modifier = Modifier,
    onCardSwiped: (index: Int) -> Unit = { },
    cardContent: @Composable (index: Int) -> Unit
) {
    if (state.indexOrder.isEmpty()) return

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val cardWidth = maxWidth * CARD_WIDTH_FRACTION
        val cardHeight = minOf(cardWidth * CARD_ASPECT_RATIO, maxHeight * CARD_MAX_HEIGHT_FRACTION)
        val cardWidthPx = with(LocalDensity.current) { cardWidth.toPx() }

        state.containerWidthPx = constraints.maxWidth.toFloat()
        state.cardWidthPx = cardWidthPx

        val scope = remember(cardWidth, cardHeight) {
            SwipingCardsScopeImpl(cardWidth, cardHeight)
        }

        with(scope) {
            RotationContainer(
                state = state,
                onCardSwiped = onCardSwiped,
                cardContent = cardContent,
            )
        }
    }
}

@Composable
private fun SwipingCardsScope.RotationContainer(
    state: SwipingCardsState,
    modifier: Modifier = Modifier,
    onCardSwiped: (index: Int) -> Unit = { },
    cardContent: @Composable (index: Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .size(cardWidth, cardHeight)
            .graphicsLayer {
                rotationY = state.stackRotationY + state.residualRotationY.value
                cameraDistance = CAMERA_DISTANCE * density
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            },
        contentAlignment = Alignment.Center
    ) {
        for (i in (state.visibleCount - 1) downTo 0) {
            val index = state.indexOrder[i]

            key(index) {
                SwipingCard(
                    stackPosition = i,
                    state = state,
                    coroutineScope = coroutineScope,
                    onCardSwiped = onCardSwiped,
                    cardContent = cardContent,
                )
            }
        }
    }
}


@Composable
private fun SwipingCardsScope.SwipingCard(
    stackPosition: Int,
    state: SwipingCardsState,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    onCardSwiped: (index: Int) -> Unit = {},
    cardContent: @Composable (index: Int) -> Unit = {},
) {
    val hapticFeedback = LocalHapticFeedback.current
    val index = state.indexOrder[stackPosition]
    val animState = state.getOrCreateCardAnimState(index, stackPosition)
    val positionConfig = stackPositionConfig(stackPosition)

    Box(
        modifier = modifier
            .size(cardWidth, cardHeight)
            .zIndex((state.visibleCount - stackPosition).toFloat())
            .graphicsLayer {
                scaleX = animState.scale.value
                scaleY = animState.scale.value
                rotationZ = animState.rotationZ.value
                this.alpha = animState.alpha.value

                val bottomAlignY = bottomAlignmentOffsetY(
                    scale = animState.scale.value,
                    rotationZRad = Math.toRadians(abs(animState.rotationZ.value).toDouble()),
                    cardWidthPx = cardWidth.toPx(),
                    cardHeightPx = cardHeight.toPx(),
                )

                translationX = animState.translationX.value +
                        if (stackPosition == 0) state.dragOffsetX.value
                        else state.backgroundRepulsionX * positionConfig.repulsionFactor
                translationY = bottomAlignY +
                        animState.translationY.value +
                        if (stackPosition == 0) state.dragOffsetY.value
                        else state.backgroundRepulsionY * positionConfig.repulsionFactor
            }
            .conditionalDragGesture(
                state = state,
                hapticFeedback = hapticFeedback,
                coroutineScope = coroutineScope,
                needToDrag = { stackPosition == 0 },
                onCardSwiped = onCardSwiped,
            )
    ) {
        cardContent(index)
    }
}

private fun Modifier.conditionalDragGesture(
    state: SwipingCardsState,
    hapticFeedback: HapticFeedback,
    coroutineScope: CoroutineScope,
    needToDrag: () -> Boolean,
    onCardSwiped: (index: Int) -> Unit,
): Modifier = then(
    if (needToDrag()) {
        Modifier.cardDragGesture(
            state = state,
            hapticFeedback = hapticFeedback,
            coroutineScope = coroutineScope,
            onCardSwiped = onCardSwiped,
        )
    } else {
        Modifier
    }
)
private fun Modifier.cardDragGesture(
    state: SwipingCardsState,
    hapticFeedback: HapticFeedback,
    coroutineScope: CoroutineScope,
    onCardSwiped: (index: Int) -> Unit,
): Modifier = pointerInput(Unit) {
    val velocityTracker = VelocityTracker()
    var gestureBlocked = false

    detectDragGestures(
        onDragStart = {
            gestureBlocked = state.isAnimating
            if (!gestureBlocked) {
                velocityTracker.resetTracking()
                state.hasPassedThreshold = false
            }
        },
        onDrag = { change, dragAmount ->
            if (gestureBlocked) return@detectDragGestures
            change.consume()
            velocityTracker.addPosition(change.uptimeMillis, change.position)
            coroutineScope.launch {
                state.dragOffsetX.snapTo(state.dragOffsetX.value + dragAmount.x)
            }
            coroutineScope.launch {
                state.dragOffsetY.snapTo(state.dragOffsetY.value + dragAmount.y)
            }
            val threshold = state.containerWidthPx * state.swipeThresholdFraction
            val distanceFromCenter = sqrt(
                state.dragOffsetX.value * state.dragOffsetX.value +
                    state.dragOffsetY.value * state.dragOffsetY.value
            )
            if (distanceFromCenter > threshold && !state.hasPassedThreshold) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                state.hasPassedThreshold = true
            }
        },
        onDragEnd = {
            if (gestureBlocked) return@detectDragGestures
            val velocity = velocityTracker.calculateVelocity()
            val threshold = state.containerWidthPx * state.swipeThresholdFraction
            val distanceFromCenter = sqrt(
                state.dragOffsetX.value * state.dragOffsetX.value +
                    state.dragOffsetY.value * state.dragOffsetY.value
            )
            val velocityMagnitude = sqrt(
                velocity.x * velocity.x + velocity.y * velocity.y
            )

            if (distanceFromCenter > threshold || velocityMagnitude > FLING_VELOCITY) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                state.isAnimating = true
                val dismissedIndex = state.indexOrder.first()

                coroutineScope.launch {
                    state.performDismiss(
                        dismissedIndex = dismissedIndex,
                        coroutineScope = coroutineScope,
                        onGestureUnlock = {
                            onCardSwiped(dismissedIndex)
                            state.hasPassedThreshold = false
                            state.isAnimating = false
                        }
                    )
                }
            } else {
                state.isAnimating = true
                coroutineScope.launch { state.settleBack() }
            }
        },
        onDragCancel = {
            if (gestureBlocked) return@detectDragGestures
            state.isAnimating = true
            coroutineScope.launch { state.settleBack() }
        }
    )
}

