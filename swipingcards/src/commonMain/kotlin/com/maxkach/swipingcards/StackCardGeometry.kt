package com.maxkach.swipingcards

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Resting appearance of a card at a zero-based stack position.
 *
 * Cards scale and rotate around their center. The deck automatically aligns their
 * bottom edges with the front card, alternating the left/right bottom corners.
 * [offsetX] and [offsetY] are additional translations after that alignment, in dp;
 * positive values move right and down, independent of layout direction. They do not
 * change the deck's measured size, so allow room around the deck for a wider fan.
 *
 * @param scale positive, finite uniform scale relative to the deck's bounds.
 * @param rotationZ finite clockwise rotation in degrees.
 */
@Immutable
data class StackCardGeometry(
    val scale: Float = 1f,
    val rotationZ: Float = 0f,
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = 0.dp,
) {
    init {
        require(scale.isFinite() && scale > 0f) { "scale must be finite and > 0" }
        require(rotationZ.isFinite()) { "rotationZ must be finite" }
        require(offsetX.value.isFinite() && offsetY.value.isFinite()) {
            "offsetX and offsetY must be finite dp values"
        }
    }
}

/** Default geometry used by [SwipingCards]. */
object SwipingCardsDefaults {
    /**
     * Returns the original layout for [position], including deeper queue slots.
     * Use `copy` to override only the properties you need.
     */
    fun cardGeometry(position: Int): StackCardGeometry {
        require(position >= 0) { "position must be >= 0" }
        val config = stackPositionConfig(position)
        return StackCardGeometry(scale = config.scale, rotationZ = config.rotationZ)
    }
}
