# Epic 1 — Arbitrary-size infinite deck — Design

Date: 2026-07-10
Source spec: [`docs/specs/epic-1-infinite-deck.md`](../../specs/epic-1-infinite-deck.md)
Status: Approved, ready for implementation planning.

## Summary

Replace the hardcoded four-card, index-based `SwipingCards` with a generic,
stateless component driven by an externally supplied list reconciled against the
component's optimistic internal order by **stable key**. Remove all hardcoded card
dimensions. Keep the animation/gesture behavior intact and re-key it from integer
index to stable key. Add pure-Kotlin unit tests for the ordering/reconciliation
core.

This is a breaking API change, acceptable pre-1.0. `rememberSwipingCardsState` and
the index-based `SwipingCards(state, …)` overload are removed.

## Decisions (from brainstorming)

1. **Stateless generic API** — `SwipingCards<T>(cards, key, …)`; no hoisted public
   state object. Replaces the old index-based state API.
2. **4-way direction** — `SwipeDirection { Left, Right, Up, Down }`, derived from the
   dominant axis of the commit drag/fling vector.
3. **Unit tests only** — pure-Kotlin JUnit tests on the extracted reconciliation
   core; UI/layout across dimensions verified manually in the sample app.
4. **Deck fills constraints** — the caller sizes the deck via `Modifier`; each card
   fills the deck box; all layout math derives from measured constraints. The sample
   is updated to wrap the deck in a sized `Box`.

## Architecture

Split the tricky reconciliation logic away from Compose so it is unit-testable on a
plain JVM without Robolectric or an emulator.

### `DeckReconciler` — pure Kotlin (no Compose/Android imports)

The unit-tested core. Operates purely on lists of `Any` keys.

- `requireUniqueKeys(keys: List<Any>)` — throws a clear `IllegalArgumentException`
  naming the offending duplicate key.
- `rotate(order: List<Any>): List<Any>` — `order.drop(1) + order.first()`; returns
  the input unchanged when empty or singleton.
- `reconcile(internal: List<Any>, external: List<Any>): ReconcileResult`
  - If `external == internal` → **confirmation**: `newOrder = internal`, `added` and
    `removed` empty. No visual change.
  - Otherwise → the external order is **authoritative**: `newOrder = external`,
    `removed = internal - external`, `added = external - internal`.

`ReconcileResult(newOrder: List<Any>, added: Set<Any>, removed: Set<Any>)`.

### `DeckState` — `@Stable`, Compose layer (internal, not public)

Holds the optimistic state and ports the existing animation machinery, re-keyed from
`Int` index to stable `Any` key:

- `internalOrder: List<Any>` as snapshot state (`mutableStateOf`).
- `cardAnimStates: MutableMap<Any, CardAnimState>` — one per live key; created on
  demand, pruned when a key is removed by reconciliation.
- Drag offsets, residual rotation, measured `containerWidthPx` / `cardWidthPx` /
  `cardHeightPx`, `isAnimating`, `hasPassedThreshold` — as today.
- `reconcile(externalKeys)` — calls `DeckReconciler.reconcile`, applies `newOrder` to
  `internalOrder`, and prunes anim states for `removed` keys. Never fires callbacks.
- `commitSwipe(...)` / `settleBack()` / `performDismiss(...)` — the ported dismiss,
  settle, and promote animations, addressed by key.

### `SwipingCards<T>` — the public composable

Owns the public API, gestures, layout, and drives reconciliation:

```kotlin
val externalKeys = remember(cards) { cards.map(key).also(DeckReconciler::requireUniqueKeys) }
val cardsByKey   = remember(cards) { cards.associateBy(key) }
val deck         = remember { DeckState() }
remember(externalKeys) { deck.reconcile(externalKeys) }   // runs ONLY when order changes
```

## Reconciliation flow (optimistic state machine)

- **`remember(externalKeys)` is the linchpin.** A recomposition with the same
  external order never calls `reconcile` → optimistic state is never reset (spec:
  "same external order is not a new command").
- **Optimistic swipe** rotates `deck.internalOrder` directly in the gesture handler,
  independent of input, so the UI never waits on the parent.
- **Confirmation** — when the parent later supplies the rotated order, `externalKeys`
  changes → `reconcile` runs → `external == internalOrder` → no-op. In-flight
  animations continue untouched because `CardAnimState`s are keyed by stable key and
  never recreated.
- **Genuinely different** external order is adopted. Surviving keys keep their
  `CardAnimState` (no reset); removed keys' states are pruned; added keys get fresh
  states positioned by the new order.
- **Actively-animated card removed externally** — reconcile drops it from
  `internalOrder` and prunes its anim state; the promote loop guards missing keys
  (`?: continue`); the outgoing exit animation simply completes off-screen. No crash,
  no duplicate callback (the commit callback already fired exactly once).

Partial example (spec): internal `B-C-A`, external becomes `B-C-D` → result `B-C-D`;
B and C keep identity, A pruned, D added.

## Public API & result model

```kotlin
@Composable
fun <T> SwipingCards(
    cards: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    maxVisibleCards: Int = 4,          // require(maxVisibleCards >= 1) — fail clearly
    maxRotationY: Float = 38f,
    swipeThresholdFraction: Float = 0.20f,
    onSwipe: (SwipeResult<T>) -> Unit = {},
    cardContent: @Composable (T) -> Unit,
)

enum class SwipeDirection { Left, Right, Up, Down }

data class SwipeResult<T>(
    val card: T,
    val key: Any,
    val direction: SwipeDirection,     // dominant axis of the commit drag/fling vector
    val resultingOrder: List<T>,       // internalOrder after rotation, mapped keys -> T
)
```

- `onSwipe` fires **exactly once at threshold commit** — not at animation end, not on
  reconcile confirmation, not on replacement.
- `resultingOrder` is the post-rotation `internalOrder` mapped through `cardsByKey`;
  the swiped card is still present (moved to the back).
- `direction`: at commit, compare `abs(dx)` vs `abs(dy)` of the combined drag/fling
  vector; horizontal → `Left`/`Right` by sign of dx, vertical → `Up`/`Down` by sign
  of dy.

## Sizing (remove hardcoded dimensions)

Delete `CARD_WIDTH_FRACTION`, `CARD_ASPECT_RATIO`, `CARD_MAX_HEIGHT_FRACTION` and the
forced `.size(cardWidth, cardHeight)`.

- `BoxWithConstraints(modifier)` takes its size from the caller's `Modifier`.
- Each card fills the box (`maxWidth` x `maxHeight`).
- `cardWidthPx`, `cardHeightPx`, `containerWidthPx`, and the swipe threshold all
  derive from measured `constraints`.
- Existing bottom-alignment and repulsion math already scale with measured px, so
  stacking stays correct at any size.
- Sample wraps the deck in a sized box, e.g.
  `Modifier.fillMaxWidth(0.75f).aspectRatio(3f / 4f)`, to stay visually close to the
  current four-card look.

Dimensions verified manually in the sample: tall portrait, wide landscape, small
constrained, and parent-responsive.

## CMP-friendliness

- Replace `java.lang.Math.toRadians` / `toDegrees` with `kotlin.math` equivalents
  (`x * PI / 180`).
- Keep `DeckReconciler` free of Compose and Android imports so the core logic is
  common-Kotlin ready for the future KMP migration.

## Testing

Add a `src/test/` source set and JUnit to the `swipingcards` module (none exists
today). Pure-Kotlin unit tests against `DeckReconciler` and any extracted helpers:

- Rotation / circular ordering: `A-B-C -> B-C-A -> C-A-B -> A-B-C` (full loop).
- Reconcile: confirmation (`external == internal` → no change); authoritative replace
  (`X-Y-Z`); partial (`B-C-A` + `B-C-D` → `B-C-D`, added `{D}`, removed `{A}`).
- Duplicate-key error.
- Counts: zero, one-to-three (no duplication), exact four, large; `visibleCount ==
  min(size, maxVisibleCards)`.
- `maxVisibleCards` validation (`>= 1`) and configurable visible count.
- `SwipeResult.resultingOrder` correctness; single callback per commit (logic-level).

UI/layout across the four dimension cases is verified manually in the sample.

## Out of scope (Epic 1 non-goals)

Themed example screens, KMP migration, publishing, per-card differing dimensions,
persistence/undo/networking/business swipe actions. No Epic 2 or Epic 3 work.

## Definition of done

- All acceptance scenarios covered by automated tests where practical, else manual.
- Existing four-card look remains visually compatible (manual check in sample).
- Arbitrary list sizes and indefinite rotation work.
- External confirmation does not reset animation; partial changes reconcile safely.
- Hardcoded size assumptions removed.
- Public API and migration notes documented (README + this design updated).
- Build and unit-test commands pass.
