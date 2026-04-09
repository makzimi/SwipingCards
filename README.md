# SwipingCards

A Jetpack Compose card stack widget with swipe-to-cycle gestures, 3D perspective rotation, and spring-based animations.

<p align="center">
  <!-- TODO: Add a GIF or screenshot here -->
  <!-- <img src="art/demo.gif" width="300" /> -->
</p>

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
val state = rememberSwipingCardsState(itemCount = items.size)

SwipingCards(
    state = state,
    modifier = Modifier.fillMaxSize(),
    onCardSwiped = { index -> /* handle dismiss */ },
) { index ->
    // Your card content
    MyCard(items[index])
}
```

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
