# Epic 1 — Arbitrary-size infinite deck — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the hardcoded four-card, index-based `SwipingCards` with a generic, stateless component driven by an externally supplied list reconciled against an optimistic internal order by stable key, with no hardcoded card dimensions.

**Architecture:** Split the reconciliation core into pure Kotlin (`DeckReconciler`, unit-tested on the JVM) and keep the Compose layer (`DeckState` holder + `SwipingCards` composable) thin. `remember(externalKeys)` drives reconciliation so same-order recompositions never reset optimistic state. Animation state is re-keyed from `Int` index to stable `Any` key. Layout derives entirely from measured `BoxWithConstraints` values.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2024.09.00), Android library module (minSdk 33, JVM 11), JUnit 4.13.2 for unit tests.

## Global Constraints

- Module namespace: `com.maxkach.swipingcards`. All new files live in `swipingcards/src/main/java/com/maxkach/swipingcards/`.
- `DeckReconciler.kt` and `SwipeResult.kt` MUST NOT import anything from `android.*`, `java.lang.Math`, or `androidx.compose.*` — they are pure Kotlin so the reconciliation core stays common-Kotlin ready for the future KMP migration. Use `kotlin.math` (`PI`, `cos`, `sin`, `abs`).
- No hardcoded card width/height/aspect-ratio in the core component. All layout math derives from measured constraints.
- Public API is breaking (pre-1.0): `rememberSwipingCardsState` and `SwipingCards(state, …)` are removed. New public surface: `SwipingCards<T>`, `SwipeResult<T>`, `SwipeDirection`.
- `onSwipe` fires exactly once at threshold commit — never at animation end, never on reconcile.
- Preserve existing gestures, 3D stacking, spring animations, haptics, and fling behavior.
- Keep JVM target 11 / Java 11 as configured.

---

### Task 1: Pure reconciliation core (`DeckReconciler`)

Adds the JVM unit-test source set to the library module and the pure ordering/reconciliation logic that all later tasks build on.

**Files:**
- Modify: `swipingcards/build.gradle.kts` (add `testImplementation` for JUnit)
- Create: `swipingcards/src/main/java/com/maxkach/swipingcards/DeckReconciler.kt`
- Test: `swipingcards/src/test/java/com/maxkach/swipingcards/DeckReconcilerTest.kt`

**Interfaces:**
- Produces:
  - `data class ReconcileResult(val newOrder: List<Any>, val added: Set<Any>, val removed: Set<Any>)`
  - `internal object DeckReconciler` with:
    - `fun requireUniqueKeys(keys: List<Any>)` — throws `IllegalArgumentException` on the first duplicate.
    - `fun rotate(order: List<Any>): List<Any>` — front to back; returns input if size < 2.
    - `fun reconcile(internal: List<Any>, external: List<Any>): ReconcileResult` — equal ⇒ confirmation (no change); else external is authoritative.
    - `fun visibleCount(size: Int, maxVisibleCards: Int): Int` — `minOf(size, maxVisibleCards)`.

- [ ] **Step 1: Add JUnit test dependency to the library module**

In `swipingcards/build.gradle.kts`, append to the `dependencies { }` block:

```kotlin
    testImplementation(libs.junit)
```

(`libs.junit` already exists in `gradle/libs.versions.toml`.)

- [ ] **Step 2: Write the failing tests**

Create `swipingcards/src/test/java/com/maxkach/swipingcards/DeckReconcilerTest.kt`:

```kotlin
package com.maxkach.swipingcards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckReconcilerTest {

    @Test
    fun rotate_movesFrontToBack() {
        assertEquals(listOf<Any>("B", "C", "A"), DeckReconciler.rotate(listOf("A", "B", "C")))
    }

    @Test
    fun rotate_fullLoopReturnsToStart() {
        var order: List<Any> = listOf("A", "B", "C")
        order = DeckReconciler.rotate(order); assertEquals(listOf<Any>("B", "C", "A"), order)
        order = DeckReconciler.rotate(order); assertEquals(listOf<Any>("C", "A", "B"), order)
        order = DeckReconciler.rotate(order); assertEquals(listOf<Any>("A", "B", "C"), order)
    }

    @Test
    fun rotate_emptyAndSingleAreUnchanged() {
        assertEquals(emptyList<Any>(), DeckReconciler.rotate(emptyList()))
        assertEquals(listOf<Any>("A"), DeckReconciler.rotate(listOf("A")))
    }

    @Test
    fun reconcile_equalOrderIsConfirmationWithNoChange() {
        val r = DeckReconciler.reconcile(listOf("B", "C", "A"), listOf("B", "C", "A"))
        assertEquals(listOf<Any>("B", "C", "A"), r.newOrder)
        assertTrue(r.added.isEmpty())
        assertTrue(r.removed.isEmpty())
    }

    @Test
    fun reconcile_partialChangeAdoptsExternalAndTracksDelta() {
        val r = DeckReconciler.reconcile(listOf("B", "C", "A"), listOf("B", "C", "D"))
        assertEquals(listOf<Any>("B", "C", "D"), r.newOrder)
        assertEquals(setOf<Any>("D"), r.added)
        assertEquals(setOf<Any>("A"), r.removed)
    }

    @Test
    fun reconcile_replacementAdoptsExternalEntirely() {
        val r = DeckReconciler.reconcile(listOf("B", "C", "A"), listOf("X", "Y", "Z"))
        assertEquals(listOf<Any>("X", "Y", "Z"), r.newOrder)
        assertEquals(setOf<Any>("X", "Y", "Z"), r.added)
        assertEquals(setOf<Any>("B", "C", "A"), r.removed)
    }

    @Test
    fun reconcile_emptyExternalRemovesEverything() {
        val r = DeckReconciler.reconcile(listOf("A", "B"), emptyList())
        assertEquals(emptyList<Any>(), r.newOrder)
        assertEquals(setOf<Any>("A", "B"), r.removed)
        assertTrue(r.added.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun requireUniqueKeys_throwsOnDuplicate() {
        DeckReconciler.requireUniqueKeys(listOf("A", "B", "A"))
    }

    @Test
    fun requireUniqueKeys_passesWhenUnique() {
        DeckReconciler.requireUniqueKeys(listOf("A", "B", "C"))
    }

    @Test
    fun visibleCount_isMinOfSizeAndMax() {
        assertEquals(0, DeckReconciler.visibleCount(size = 0, maxVisibleCards = 4))
        assertEquals(2, DeckReconciler.visibleCount(size = 2, maxVisibleCards = 4))
        assertEquals(4, DeckReconciler.visibleCount(size = 4, maxVisibleCards = 4))
        assertEquals(4, DeckReconciler.visibleCount(size = 6, maxVisibleCards = 4))
        assertEquals(2, DeckReconciler.visibleCount(size = 6, maxVisibleCards = 2))
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :swipingcards:testDebugUnitTest --tests "com.maxkach.swipingcards.DeckReconcilerTest"`
Expected: FAIL — compilation error, `DeckReconciler` / `ReconcileResult` unresolved.

- [ ] **Step 4: Write the minimal implementation**

Create `swipingcards/src/main/java/com/maxkach/swipingcards/DeckReconciler.kt`:

```kotlin
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
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :swipingcards:testDebugUnitTest --tests "com.maxkach.swipingcards.DeckReconcilerTest"`
Expected: PASS — 10 tests green.

- [ ] **Step 6: Commit**

```bash
git add swipingcards/build.gradle.kts \
        swipingcards/src/main/java/com/maxkach/swipingcards/DeckReconciler.kt \
        swipingcards/src/test/java/com/maxkach/swipingcards/DeckReconcilerTest.kt
git commit -m "feat: add pure DeckReconciler core with unit tests"
```

---

### Task 2: Swipe result model + direction resolution

The public result type and the pure direction helper used by the gesture handler.

**Files:**
- Create: `swipingcards/src/main/java/com/maxkach/swipingcards/SwipeResult.kt`
- Test: `swipingcards/src/test/java/com/maxkach/swipingcards/SwipeDirectionTest.kt`

**Interfaces:**
- Produces:
  - `enum class SwipeDirection { Left, Right, Up, Down }`
  - `data class SwipeResult<T>(val card: T, val key: Any, val direction: SwipeDirection, val resultingOrder: List<T>)`
  - `internal fun resolveSwipeDirection(dx: Float, dy: Float): SwipeDirection` — dominant axis; horizontal ties resolve to Left/Right. Screen coordinates: `dy > 0` is downward.

- [ ] **Step 1: Write the failing tests**

Create `swipingcards/src/test/java/com/maxkach/swipingcards/SwipeDirectionTest.kt`:

```kotlin
package com.maxkach.swipingcards

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeDirectionTest {

    @Test
    fun horizontalDominantResolvesLeftRight() {
        assertEquals(SwipeDirection.Right, resolveSwipeDirection(dx = 100f, dy = 10f))
        assertEquals(SwipeDirection.Left, resolveSwipeDirection(dx = -100f, dy = 10f))
    }

    @Test
    fun verticalDominantResolvesUpDown() {
        assertEquals(SwipeDirection.Down, resolveSwipeDirection(dx = 5f, dy = 80f))
        assertEquals(SwipeDirection.Up, resolveSwipeDirection(dx = 5f, dy = -80f))
    }

    @Test
    fun equalMagnitudeTieResolvesHorizontal() {
        assertEquals(SwipeDirection.Right, resolveSwipeDirection(dx = 50f, dy = 50f))
        assertEquals(SwipeDirection.Left, resolveSwipeDirection(dx = -50f, dy = -50f))
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :swipingcards:testDebugUnitTest --tests "com.maxkach.swipingcards.SwipeDirectionTest"`
Expected: FAIL — `SwipeDirection` / `resolveSwipeDirection` unresolved.

- [ ] **Step 3: Write the minimal implementation**

Create `swipingcards/src/main/java/com/maxkach/swipingcards/SwipeResult.kt`:

```kotlin
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
 * horizontal wins ties. Screen coordinates: positive [dy] points downward.
 */
internal fun resolveSwipeDirection(dx: Float, dy: Float): SwipeDirection =
    if (abs(dx) >= abs(dy)) {
        if (dx >= 0f) SwipeDirection.Right else SwipeDirection.Left
    } else {
        if (dy >= 0f) SwipeDirection.Down else SwipeDirection.Up
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :swipingcards:testDebugUnitTest --tests "com.maxkach.swipingcards.SwipeDirectionTest"`
Expected: PASS — 3 tests green.

- [ ] **Step 5: Commit**

```bash
git add swipingcards/src/main/java/com/maxkach/swipingcards/SwipeResult.kt \
        swipingcards/src/test/java/com/maxkach/swipingcards/SwipeDirectionTest.kt
git commit -m "feat: add SwipeResult model and direction resolution"
```

---

### Task 3: Key-based deck state holder (`DeckState`)

Ports the animation/stacking machinery from the old `SwipingCardsState` to a key-addressed holder, replaces `java.lang.Math` with `kotlin.math`, and wires reconciliation + anim-state pruning. This task is compile-gated (Compose state with `Animatable` is verified by the manual UI check in Task 5, not unit tests).

**Files:**
- Create: `swipingcards/src/main/java/com/maxkach/swipingcards/DeckState.kt`
- Delete: `swipingcards/src/main/java/com/maxkach/swipingcards/SwipingCardsState.kt` (its contents move here, re-keyed) — deleted in Task 4 once `SwipingCards.kt` no longer references it.

**Interfaces:**
- Consumes: `DeckReconciler.reconcile`, `DeckReconciler.rotate`, `DeckReconciler.visibleCount` (Task 1).
- Produces (all `internal`):
  - `class CardAnimState(config: StackPositionConfig, initialXPx: Float)` with `scale, rotationZ, translationX, translationY, alpha` Animatables.
  - `data class StackPositionConfig(scale, rotationZ, alpha, repulsionFactor, elevation)` and `fun stackPositionConfig(position: Int): StackPositionConfig`.
  - `fun idleTranslationXPx(position, scale, rotationZDeg, cardWidthPx): Float`
  - `fun bottomAlignmentOffsetY(scale, rotationZRad: Double, cardWidthPx, cardHeightPx): Float`
  - `class DeckState` with:
    - snapshot state: `internalOrder: List<Any>`, `isAnimating: Boolean`, `hasPassedThreshold: Boolean`, `containerWidthPx: Float`, `cardWidthPx: Float`, `cardHeightPx: Float`.
    - config vars set by the composable each pass: `maxVisibleCards: Int`, `maxRotationY: Float`, `swipeThresholdFraction: Float`.
    - `dragOffsetX`, `dragOffsetY`, `residualRotationY` Animatables.
    - `val visibleCount: Int`, `val dragProgress: Float`, `val stackRotationY: Float`, `val backgroundRepulsionX/Y: Float`.
    - `fun getOrCreateCardAnimState(key: Any, stackPosition: Int): CardAnimState`
    - `fun reconcile(externalKeys: List<Any>)` — applies reconciler result to `internalOrder` and prunes removed keys' anim states.
    - `suspend fun settleBack()`
    - `suspend fun performDismiss(dismissedKey: Any, coroutineScope: CoroutineScope, onGestureUnlock: () -> Unit)`

- [ ] **Step 1: Create the new `DeckState.kt`**

Create `swipingcards/src/main/java/com/maxkach/swipingcards/DeckState.kt`:

```kotlin
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
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :swipingcards:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (`SwipingCardsState.kt` and `SwipingCards.kt` still exist and still compile against the old state; they are replaced in Task 4. `DeckState` is currently unreferenced — that is fine.)

- [ ] **Step 3: Commit**

```bash
git add swipingcards/src/main/java/com/maxkach/swipingcards/DeckState.kt
git commit -m "feat: add key-based DeckState holder (kotlin.math, reconcile + prune)"
```

---

### Task 4: Generic `SwipingCards` composable + remove old API

Rewrites the public composable to the generic, list/key-based, dimension-agnostic API, wires reconciliation, builds `SwipeResult`, and deletes the old index-based state file. Compile-gated; behavior verified in Task 5.

**Files:**
- Modify (full rewrite): `swipingcards/src/main/java/com/maxkach/swipingcards/SwipingCards.kt`
- Delete: `swipingcards/src/main/java/com/maxkach/swipingcards/SwipingCardsState.kt`

**Interfaces:**
- Consumes: `DeckState`, `getOrCreateCardAnimState`, `reconcile`, `settleBack`, `performDismiss`, `stackPositionConfig`, `bottomAlignmentOffsetY` (Task 3); `DeckReconciler.requireUniqueKeys` (Task 1); `SwipeResult`, `resolveSwipeDirection` (Task 2).
- Produces (public API):
  - `@Composable fun <T> SwipingCards(cards: List<T>, key: (T) -> Any, modifier: Modifier = Modifier, maxVisibleCards: Int = 4, maxRotationY: Float = 38f, swipeThresholdFraction: Float = 0.20f, onSwipe: (SwipeResult<T>) -> Unit = {}, cardContent: @Composable (T) -> Unit)`

- [ ] **Step 1: Rewrite `SwipingCards.kt`**

Replace the entire contents of `swipingcards/src/main/java/com/maxkach/swipingcards/SwipingCards.kt` with:

```kotlin
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
```

- [ ] **Step 2: Delete the old state file**

Run: `git rm swipingcards/src/main/java/com/maxkach/swipingcards/SwipingCardsState.kt`
Expected: file removed. (All of its symbols now live in `DeckState.kt`, re-keyed.)

- [ ] **Step 3: Verify the library compiles and unit tests still pass**

Run: `./gradlew :swipingcards:compileDebugKotlin :swipingcards:testDebugUnitTest`
Expected: BUILD SUCCESSFUL; the 13 unit tests from Tasks 1–2 pass. No references to `rememberSwipingCardsState`, `SwipingCardsState`, or `COMPOSED_CARDS` remain in the library module.

- [ ] **Step 4: Commit**

```bash
git add swipingcards/src/main/java/com/maxkach/swipingcards/SwipingCards.kt
git rm --cached swipingcards/src/main/java/com/maxkach/swipingcards/SwipingCardsState.kt 2>/dev/null || true
git commit -m "feat: generic key-reconciled SwipingCards API, remove index-based state"
```

---

### Task 5: Update the sample and verify UI across dimensions

Migrates the sample app to the new API and a caller-sized deck, then verifies the four required dimension cases by hand (the chosen test scope is unit tests only; UI is verified manually).

**Files:**
- Modify: `sample/src/main/java/com/maxkach/swipingcardssample/SwipingCardsSampleScreen.kt`

**Interfaces:**
- Consumes: `SwipingCards<T>` public API (Task 4); existing `IdeaCard` / `sampleIdeas` / `IdeaCardView` (unchanged). `IdeaCard.title` is unique across `sampleIdeas`, so it is a valid stable key.

- [ ] **Step 1: Rewrite the sample screen to the new API + sized deck**

Replace the entire contents of `sample/src/main/java/com/maxkach/swipingcardssample/SwipingCardsSampleScreen.kt` with:

```kotlin
package com.maxkach.swipingcardssample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.maxkach.swipingcards.SwipingCards
import androidx.compose.foundation.layout.Box

@Composable
fun SwipingCardsExampleScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        SwipingCards(
            cards = sampleIdeas,
            key = { idea -> idea.title },
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .aspectRatio(3f / 4f),
            maxVisibleCards = 4,
        ) { idea ->
            IdeaCardView(idea)
        }
    }
}
```

- [ ] **Step 2: Verify the whole project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL for both `:swipingcards` and `:sample`.

- [ ] **Step 3: Manual UI verification (record results in the commit body)**

Build and run the sample on a device/emulator (`./gradlew :sample:installDebug` or Android Studio). Confirm each item:

  - [ ] Four cards visible by default; stacking (scale/rotation/elevation) matches the pre-change look.
  - [ ] Swiping the front card sends it to the back; the deck cycles indefinitely through all four.
  - [ ] A drag released below threshold settles back with no reorder.
  - [ ] **Tall portrait deck** — temporarily set `modifier = Modifier.fillMaxHeight(0.7f).aspectRatio(2f / 3f)`; cards lay out and animate correctly.
  - [ ] **Wide landscape deck** — temporarily set `modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(16f / 9f)`; correct layout/animation.
  - [ ] **Small constrained deck** — temporarily set `modifier = Modifier.size(160.dp, 200.dp)`; correct layout/animation.
  - [ ] **Parent-responsive deck** — temporarily set `modifier = Modifier.fillMaxSize()`; correct layout/animation.
  - [ ] Restore `modifier = Modifier.fillMaxWidth(0.75f).aspectRatio(3f / 4f)` after verification.

  (The temporary modifiers require importing `fillMaxHeight` / `size` / `dp` as needed while testing; revert those imports too.)

- [ ] **Step 4: Commit**

```bash
git add sample/src/main/java/com/maxkach/swipingcardssample/SwipingCardsSampleScreen.kt
git commit -m "feat: migrate sample to key-based SwipingCards API and caller-sized deck

Manual UI verification: four-card look preserved; infinite cycling, cancelled
gesture, and tall/wide/small/responsive dimension cases all correct."
```

---

### Task 6: Documentation + migration notes

Updates the README usage to the new API and records the breaking-change migration.

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: the public API from Task 4.

- [ ] **Step 1: Update the README usage section and add migration notes**

In `README.md`, replace the `## Usage` code block (the `val state = rememberSwipingCardsState(...)` example) with:

````markdown
## Usage

```kotlin
SwipingCards(
    cards = items,
    key = { it.id },
    modifier = Modifier
        .fillMaxWidth(0.8f)
        .aspectRatio(3f / 4f),
    maxVisibleCards = 4,
    onSwipe = { result ->
        // result.card, result.key, result.direction, result.resultingOrder
    },
) { item ->
    MyCard(item)
}
```

The deck is an infinite circular queue of any size: swiping the front card sends it
to the back. Supply your list via `cards` and a stable `key`; the component keeps an
optimistic internal order and reconciles it against your list without restarting
in-flight animations. The deck fills the size you give it through `modifier` — no card
dimensions are hardcoded.

### Migrating from the index-based API

The previous `rememberSwipingCardsState(itemCount = …)` + `SwipingCards(state, …)` API
has been replaced by the generic list/key API above:

- Pass your `cards` list and a `key` selector instead of an item count.
- `cardContent` and `onSwipe` receive the card (`T`) instead of an integer index —
  `onSwipe` now delivers a `SwipeResult<T>` (`card`, `key`, `direction`, `resultingOrder`).
- Size the deck yourself via `Modifier` (e.g. `.aspectRatio(3f / 4f)`); the old fixed
  75%-width / 4:3 sizing is gone.
````

- [ ] **Step 2: Verify the README renders sensibly**

Run: `grep -n "rememberSwipingCardsState" README.md`
Expected: matches appear ONLY inside the "Migrating from the index-based API" section (describing the old API), not in the primary usage example.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: document key-based SwipingCards API and migration notes"
```

---

## Self-Review

**Spec coverage** (each Epic 1 requirement → task):
- Any number of cards incl. zero → Task 1 (`rotate`/`reconcile` empty), Task 4 (`if internalOrder.isEmpty() return`).
- Up to configurable max, default 4 / `min(size, maxVisibleCards)` → Task 1 (`visibleCount`), Task 4 (param, `require >= 1`).
- Infinite rotation, commit once, callback with order → Task 3 (`performDismiss`/`rotate`), Task 4 (`onSwipe` once in `onGestureUnlock`).
- Cancelled gesture returns, no event → Task 4 (`settleBack` branch).
- Optimistic state, confirmation without reset → Task 3 (`reconcile` equal-order no-op), Task 4 (`remember(externalKeys)`).
- Reconcile by stable key; partial/replacement/removed/added → Task 1 (`reconcile` delta), Task 3 (prune removed).
- Duplicate keys error → Task 1 (`requireUniqueKeys`), Task 4 (call site).
- `SwipeResult` (card/key/direction/order) → Task 2, Task 4.
- No hardcoded dimensions; derive from constraints; portrait/landscape/small/responsive → Task 4 (BoxWithConstraints), Task 5 (manual checks).
- CMP-friendly (no `java.lang.Math`, core Compose-free) → Task 1/2 (pure), Task 3 (`kotlin.math`).
- Removed-animating-card safety → Task 3 (`performDismiss` null-guard + `?: continue`).
- Tests (ordering, reconciliation, duplicate keys, callback/order, counts) → Task 1, Task 2.
- Existing behavior compatible + docs → Task 5 (manual), Task 6 (README).

**Placeholder scan:** No TBD/TODO/"handle edge cases"; every code step shows complete code.

**Type consistency:** `DeckState.internalOrder: List<Any>`, `reconcile(externalKeys: List<Any>)`, `performDismiss(dismissedKey: Any, …)`, `getOrCreateCardAnimState(key: Any, stackPosition: Int)`, `resolveSwipeDirection(dx, dy)`, `SwipeResult<T>(card, key, direction, resultingOrder)` are used identically across Tasks 3–5. `DeckReconciler` methods (`rotate`, `reconcile`, `visibleCount`, `requireUniqueKeys`) match Task 1 definitions at all call sites.
