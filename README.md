# SwipingCards

A Jetpack Compose card stack widget with swipe-to-cycle gestures, 3D perspective rotation, and spring-based animations.

![SwipingCards-only-1](https://github.com/user-attachments/assets/17aa8af7-9f30-4a50-b03f-71967a675908)

## Features

- **Swipe to cycle** — swipe the top card away and it goes to the back of the stack (like swipe-to-dismiss, but nothing is lost)
- **3D rotation** — cards tilt in perspective as you drag
- **Stacked layout** — up to 4 cards visible with scale, rotation, and elevation
- **Background repulsion** — background cards push away as you drag the top one
- **Spring animations** — smooth settle-back and card promotion
- **Haptic feedback** — vibrates on threshold and on swipe
- **Fling support** — fast swipes work too
- **Customizable** — rotation, threshold, and card content are all yours

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
dimensions are hardcoded. The 3D tilt and swipe sensitivity are also tunable via the
optional `maxRotationY` (default `38f` degrees) and `swipeThresholdFraction` (default
`0.20f`) parameters.

**Reconciliation note:** external updates should preserve the relative position of
surviving cards. Removing/adding cards and confirming optimistic swipes reconcile
smoothly; reordering cards that remain in the deck to new stack positions is not
animated.

### Migrating from the index-based API

The previous `rememberSwipingCardsState(itemCount = …)` + `SwipingCards(state, …)` API
has been replaced by the generic list/key API above:

- Pass your `cards` list and a `key` selector instead of an item count.
- `cardContent` and `onSwipe` receive the card (`T`) instead of an integer index —
  `onSwipe` now delivers a `SwipeResult<T>` (`card`, `key`, `direction`, `resultingOrder`).
- Size the deck yourself via `Modifier` (e.g. `.aspectRatio(3f / 4f)`); the old fixed
  75%-width / 4:3 sizing is gone.

## License

```
MIT License

Copyright (c) 2026 Maxim Kachinkin

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
