package com.maxkach.swipingcards

/** Result of reconciling the optimistic internal order against a new external order. */
data class ReconcileResult(
    val newOrder: List<Any>,
    val added: Set<Any>,
    val removed: Set<Any>,
)

/**
 * Pure ordering + reconciliation logic for the deck. No Compose or Android
 * dependencies so it can be unit-tested on the JVM and later moved to common Kotlin.
 */
internal object DeckReconciler {

    /** Throws [IllegalArgumentException] naming the first duplicate key encountered. */
    fun requireUniqueKeys(keys: List<Any>) {
        val seen = HashSet<Any>(keys.size)
        for (key in keys) {
            require(seen.add(key)) {
                "SwipingCards: duplicate card key '$key'. Keys returned by `key` must be " +
                    "unique within the supplied list."
            }
        }
    }

    /** Moves the front card to the back. Returns the input unchanged when size < 2. */
    fun rotate(order: List<Any>): List<Any> =
        if (order.size < 2) order else order.drop(1) + order.first()

    /**
     * Reconciles the optimistic [internal] order against a new [external] order.
     * When they are equal, this is a confirmation of optimistic state and nothing
     * changes. Otherwise the external order is authoritative.
     */
    fun reconcile(internal: List<Any>, external: List<Any>): ReconcileResult {
        if (internal == external) {
            return ReconcileResult(newOrder = internal, added = emptySet(), removed = emptySet())
        }
        val internalSet = internal.toHashSet()
        val externalSet = external.toHashSet()
        val added = external.filterNot { it in internalSet }.toSet()
        val removed = internal.filterNot { it in externalSet }.toSet()
        return ReconcileResult(newOrder = external, added = added, removed = removed)
    }

    /** Number of cards actually rendered: `min(size, maxVisibleCards)`. */
    fun visibleCount(size: Int, maxVisibleCards: Int): Int = minOf(size, maxVisibleCards)
}
