package com.maxkach.swipingcards

/**
 * [key] is the identity supplied to SwipingCards. [position] follows the internal visible
 * order (zero is the front), or is null while the card exits behind the visible stack.
 */
data class CardInfo(
    val key: Any,
    val position: Int?,
) {
    val isExiting: Boolean get() = position == null
}
