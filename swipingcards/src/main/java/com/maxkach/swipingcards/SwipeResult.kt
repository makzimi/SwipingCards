package com.maxkach.swipingcards

import kotlin.math.abs

/** Direction of a committed swipe, derived from the dominant axis of the gesture. */
enum class SwipeDirection { Left, Right, Up, Down }

/**
 * Information about a committed swipe, emitted exactly once when the front card
 * crosses the swipe threshold.
 *
 * @param card the swiped card.
 * @param key the swiped card's stable key.
 * @param direction the dominant direction of the commit gesture.
 * @param resultingOrder the deck's internal order after the rotation, in card terms.
 */
data class SwipeResult<T>(
    val card: T,
    val key: Any,
    val direction: SwipeDirection,
    val resultingOrder: List<T>,
)

/**
 * Resolves a swipe direction from a movement vector. The larger-magnitude axis wins;
 * horizontal wins ties. Screen coordinates: positive [dy] points downward. A zero
 * vector (or a horizontal tie, `|dx| == |dy|`) resolves to a horizontal direction —
 * [SwipeDirection.Right] when `dx >= 0`.
 */
internal fun resolveSwipeDirection(dx: Float, dy: Float): SwipeDirection =
    if (abs(dx) >= abs(dy)) {
        if (dx >= 0f) SwipeDirection.Right else SwipeDirection.Left
    } else {
        if (dy >= 0f) SwipeDirection.Down else SwipeDirection.Up
    }
