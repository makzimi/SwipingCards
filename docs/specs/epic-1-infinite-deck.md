# Epic 1 — Arbitrary-size infinite card deck

## Status

Ready for implementation.

## Problem

The current implementation is hardcoded around exactly four cards. It must support any
number of cards while displaying no more than four by default.

Cards form an infinite circular queue. Swiping the front card moves it to the back:

```
[A][B][C][D](E)(F)   ← visible cards in brackets, hidden in parentheses

Swipe A:

[B][C][D][E](F)(A)
```

The component must also reconcile its optimistic internal animation state with
externally supplied state without restarting matching animations.

## Goals

- Accept any number of cards, including zero.
- Display up to a configurable maximum, defaulting to four.
- Rotate cards indefinitely.
- Preserve existing gestures, stacking, and animation behavior.
- Support externally controlled data without animation resets.
- Reconcile partial data changes by stable card identity.
- Remove hardcoded card dimensions.
- Keep the API suitable for a future Compose Multiplatform migration.

## Non-goals

- Creating themed example screens.
- Migrating the project to KMP.
- Publishing a library.
- Supporting different dimensions for individual cards within one deck.
- Adding persistence, undo history, networking, or business-specific swipe actions.

## Terminology

- **External order** — the list currently supplied by the caller.
- **Internal order** — the component's optimistic order, including committed swipes not
  yet confirmed externally.
- **Front card** — the next card the user can swipe.
- **Visible cards** — the first `min(cardCount, maxVisibleCards)` cards.
- **Committed swipe** — a gesture that crosses the configured swipe threshold.
- **Stable key** — a unique identity that remains unchanged for the lifetime of a
  logical card.

## Required behavior

### Card counts

- Zero cards display an empty deck without throwing.
- One to three cards display only those unique cards.
- Cards must never be duplicated to fill visible positions.
- With four or more cards, four are visible by default.
- `maxVisibleCards` is configurable and defaults to 4.
- The number rendered is `min(cards.size, maxVisibleCards)`.
- Invalid `maxVisibleCards` values must fail clearly or be constrained according to
  existing project conventions.

### Infinite rotation

When the front card crosses the swipe threshold:

1. The swipe commits exactly once.
2. The front card moves to the end of the internal order.
3. The next hidden card enters the visible stack.
4. The outgoing animation continues.
5. A callback reports the committed swipe and resulting order.
6. Further swipes continue rotating the list indefinitely.

Example:

```
A-B-C-D-E-F
B-C-D-E-F-A
C-D-E-F-A-B
D-E-F-A-B-C
```

A gesture that does not cross the threshold must return the card to its original
position without rotating or emitting a committed-swipe event.

### Optimistic state

The component uses optimistic reconciled state:

- Rotation occurs internally as soon as the threshold is crossed.
- UI progress does not wait for the parent to update its list.
- The deck may continue operating if the parent update is delayed.
- A later matching external update confirms the internal state.
- Confirmation must not restart, snap, or cancel a matching in-flight animation.

Example:

```
External:                   A-B-C
Internal after threshold:   B-C-A
External update:            B-C-A
Result:                     confirmation only; no visual reset
```

### External-state reconciliation

Cards are reconciled using stable keys.

Rules:

- The caller supplies a stable key for every card.
- Duplicate keys are invalid and must produce a clear error.
- A recomposition containing the same external order is not a new command and must not
  reset optimistic state.
- An external order matching the current internal order is confirmation only.
- When some cards survive an update, their existing UI/animation identity should be
  reused wherever possible.
- Removed keys disappear.
- Added keys are introduced according to the new external order.
- The incoming external order is authoritative when it is genuinely different.
- A completely different set replaces the current deck.

Partial update example:

```
Current internal order:   B-C-A
New external order:        B-C-D
Result:                    B-C-D
```

Cards B and C retain identity and should not reset unnecessarily; A is removed and D is
added.

If the actively animated card is removed externally, the component must settle safely
into the new order without crashes or duplicate callbacks.

### Stable identity

The API should support a key selector equivalent to `key: (T) -> Any`.

```kotlin
key = { card -> card.id }
```

Keys must be unique within the supplied list.

### Callbacks

A committed swipe should expose enough information for external state management:

- Swiped card
- Stable key
- Swipe direction, if the existing component supports direction
- Resulting internal card order

The callback fires once when the threshold is committed — not again when the exit
animation finishes, and not when external state confirms the order.

Exact names should follow existing project conventions after inspecting the current API.

### Sizing and layout

- The core component must not hardcode card width, height, or aspect ratio.
- The caller controls deck dimensions through `Modifier`, parent constraints, or card
  content.
- Layout and animation calculations must derive from actual constraints or measurements.
- Different deck instances may use substantially different dimensions.
- Cards within one deck are expected to use consistent dimensions.
- Existing visual stacking offsets and transformations should scale or behave correctly
  under different supported sizes.

At minimum, verify:

- Tall portrait deck
- Wide landscape deck
- Small constrained deck
- Parent-size or responsive deck

### Compatibility

Prefer evolving the existing public API without unnecessary breaking changes. If
compatibility overloads are practical, retain them and route them through the new
implementation.

Avoid introducing Android-only concepts into new core state logic where common Kotlin
or common Compose APIs are sufficient.

## Suggested API direction

The exact signature should be adapted to the repository:

```kotlin
@Composable
fun <T> SwipingCards(
    cards: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    maxVisibleCards: Int = 4,
    onSwipe: (SwipeResult<T>) -> Unit = {},
    cardContent: @Composable (T) -> Unit,
)
```

Illustrative result model:

```kotlin
data class SwipeResult<T>(
    val card: T,
    val key: Any,
    val direction: SwipeDirection,
    val resultingOrder: List<T>,
)
```

This is a design direction, not a requirement to replace compatible existing names.

## Acceptance scenarios

### Empty deck

```
Given the card list is empty
When the deck is composed
Then no card is displayed
And the component does not throw
```

### Fewer than four cards

```
Given cards A and B
And maxVisibleCards is 4
When the deck is composed
Then A and B are visible
And no duplicated cards are created
```

### More than four cards

```
Given cards A through F
And maxVisibleCards is 4
When the deck is composed
Then A, B, C, and D are visible
And E and F are not visible
```

### Infinite rotation

```
Given cards A through F
When A crosses the swipe threshold
Then the internal order becomes B-C-D-E-F-A
And B, C, D, and E are visible

When B crosses the swipe threshold
Then the internal order becomes C-D-E-F-A-B
And C, D, E, and F are visible
```

### Full loop

```
Given cards A, B, and C
When three swipes are committed
Then the internal order is A-B-C
And each swipe emitted exactly one event
```

### Cancelled gesture

```
Given A is the front card
When A is dragged without crossing the threshold
Then A returns to the front
And the order does not change
And no committed-swipe event is emitted
```

### Matching external confirmation

```
Given external order A-B-C
And A has committed a swipe
And internal order is B-C-A
When external order becomes B-C-A during the animation
Then the animation continues without restarting
And no additional swipe event is emitted
```

### Partial external change

```
Given internal order B-C-A
When external order becomes B-C-D
Then the resulting order is B-C-D
And B and C preserve their stable identity
And A is removed
And D is added
```

### Replacement

```
Given internal order B-C-A
When external order becomes X-Y-Z
Then the deck changes to X-Y-Z
And no swipe event is emitted for the replacement
```

### Duplicate keys

```
Given two cards have the same stable key
When the deck processes the list
Then it fails with a clear duplicate-key error
```

### Configurable visible count

```
Given six cards
And maxVisibleCards is 2
When the deck is composed
Then exactly two cards are visible
```

### Responsive dimensions

```
Given the same deck is composed once with portrait constraints
And once with landscape constraints
When cards are dragged and swiped
Then both decks lay out and animate correctly
And neither depends on a fixed internal card size
```

## Verification expectations

- Unit tests for circular ordering and reconciliation.
- Tests for zero, small, exact, and large lists.
- Tests for duplicate keys.
- Tests for callback count and resulting order.
- Tests for stale, confirming, partial, and replacement external updates.
- Compose UI or screenshot checks for multiple deck dimensions where supported.
- Existing tests remain passing.
- No unrelated Epic 2 or Epic 3 work is included.

## Definition of done

- All acceptance scenarios are implemented or covered by automated tests where
  practical.
- Existing four-card behavior remains visually compatible.
- Arbitrary list sizes work.
- Infinite rotation works.
- External confirmation does not reset animation.
- Partial external changes reconcile safely.
- Hardcoded size assumptions are removed.
- Public API and migration notes are documented.
- Relevant build and test commands pass.
