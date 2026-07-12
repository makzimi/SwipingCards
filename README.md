# SwipingCards

[![Maven Central](https://img.shields.io/maven-central/v/io.github.makzimi/swipingcards)](https://central.sonatype.com/artifact/io.github.makzimi/swipingcards)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A Compose Multiplatform (Android + iOS) card-stack widget with swipe-to-cycle gestures, 3D perspective rotation, and spring-based animations.

https://github.com/user-attachments/assets/5f20b261-ac51-4bd1-acd1-f8612707c9d6

## Features

- **Swipe to cycle** — swipe the top card away and it goes to the back of the stack (like swipe-to-dismiss, but nothing is lost)
- **Infinite deck of any size** — an arbitrary-length circular queue; the deck never runs out
- **Generic and stateless** — driven by your own `List<T>` and a stable `key`; no card dimensions are hardcoded
- **3D rotation** — cards tilt in perspective as you drag
- **Stacked layout** — up to `maxVisibleCards` visible with scale, rotation, and elevation, plus a hidden queue that cycles in behind them
- **Background repulsion** — background cards push away as you drag the top one
- **Spring animations** — smooth settle-back and card promotion
- **Haptic feedback** — vibrates on threshold and on swipe
- **Fling support** — fast swipes commit too
- **4-way swipe result** — every committed swipe reports its direction (Left / Right / Up / Down)

## Getting started

SwipingCards is available from Maven Central (see [`swipingcards/README.md`](swipingcards/README.md)
for full details). Add it to a Compose Multiplatform project's `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.makzimi:swipingcards:0.1.0")
        }
    }
}
```

You can also consume it locally without a Central release via
`./gradlew :swipingcards:publishToMavenLocal`.

Requires Android `minSdk 33` and iOS via Compose Multiplatform.

### Modules

- `swipingcards/` — the Compose Multiplatform library (Android + iOS targets)
- `samples/shared` — the shared example gallery (Compose Multiplatform)
- `samples/androidApp` — thin Android launcher for the gallery
- `samples/iosApp` — thin iOS launcher for the gallery

## Usage

```kotlin
SwipingCards(
    cards = items,
    key = { it.id },
    modifier = Modifier
        .fillMaxWidth(0.8f)
        .aspectRatio(2f / 3f),
    maxVisibleCards = 4,
    onSwipe = { result ->
        // result.card, result.key, result.direction, result.resultingOrder
    },
) { item ->
    MyCard(item)
}
```

That's the whole API: give it your `cards`, a `key`, a size (via `modifier`), and a
composable to render one card. Everything else — the gestures, the stack, the
animations — is handled for you.

## How it works

**Infinite circular deck.** The cards form a circular queue. Swiping the front card
sends it to the back, so the deck cycles forever and nothing is ever lost. When there
are more cards than `maxVisibleCards`, the extra cards wait in a hidden queue and
animate in from behind as the front cards leave.

**External list, reconciled by key.** You own the data. SwipingCards keeps an
*optimistic* internal order and reconciles it against your `cards` list by stable
`key`:

- Committing a swipe is optimistic — the internal order rotates immediately, and a
  matching update from your list is treated as a confirmation, so in-flight animations
  are never restarted.
- Adding or removing cards reconciles smoothly without interrupting animations.
- When an external update reorders cards that stay in the deck, surviving cards are
  re-seated to their new stack positions immediately (no animated transition).

Keys must be **stable and unique** within the list; the deck de-duplicates against them.

**You control the size.** The deck fills whatever bounds you give it through
`modifier` — no dimensions are baked in. Set an aspect ratio (e.g.
`.aspectRatio(2f / 3f)` for a tall poster, `.aspectRatio(1.586f)` for a wide card) and
each card fills the deck box.

## API reference

`@Composable fun <T> SwipingCards(...)`

| Parameter | Default | What it does |
| --- | --- | --- |
| `cards: List<T>` | — | Your current list of cards. May be empty. |
| `key: (T) -> Any` | — | Stable, unique identity for each card. |
| `modifier: Modifier` | `Modifier` | Sizes the deck (e.g. `fillMaxWidth(0.8f).aspectRatio(2f/3f)`). |
| `maxVisibleCards: Int` | `4` | Max cards rendered at once (must be ≥ 1). |
| `maxRotationY: Float` | `38f` | Degrees of 3D tilt at full drag. |
| `swipeThresholdFraction: Float` | `0.20f` | Fraction of the deck width the drag must cross to commit. |
| `onSwipe: (SwipeResult<T>) -> Unit` | `{}` | Called exactly once when a swipe commits. |
| `cardContent: @Composable (T) -> Unit` | — | Renders a single card. |

Every committed swipe delivers a `SwipeResult<T>`:

```kotlin
data class SwipeResult<T>(
    val card: T,                 // the card that was swiped
    val key: Any,                // its stable key
    val direction: SwipeDirection, // Left, Right, Up, or Down
    val resultingOrder: List<T>, // the deck's order after the swipe
)

enum class SwipeDirection { Left, Right, Up, Down }
```

## Example gallery

`samples/shared` is a gallery that drives the *same* `SwipingCards` component through
four very different demos — proof that one deck handles different dimensions, content,
actions, and card counts. Each demo feeds a small event history from its swipe callback.
It's launched on Android via `samples/androidApp` and on iOS via `samples/iosApp`.

| Demo | Card shape | Deck size | Content |
| --- | --- | --- | --- |
| **Dating** | Tall 2:3 portrait | 5 cards | Full-bleed character art, name, tagline, interest chips (pass / like) |
| **Bank cards** | Wide 1.586:1 landscape | 3 cards | Code-drawn gradient card with a chip and masked number (skip / select) |
| **D&D** | Near-square 4:5 | 10 cards | Ornate "printed card" — framed art, stat block (reject / recruit) |
| **Streaming** | 2:3 poster on black | 6 cards | Netflix-style poster with Play / My List buttons and badges |

Run it on Android with `./gradlew :samples:androidApp:installDebug` (or open the
project and launch the `androidApp` app), or open `samples/iosApp` in Xcode and run
it on an iOS simulator.

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
