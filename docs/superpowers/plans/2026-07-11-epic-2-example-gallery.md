# Epic 2 — Themed Example Gallery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android gallery in the `:sample` module with three visually distinct demos (Dating 2:3, Bank 1.586:1 landscape, D&D 4:5) that each drive the Epic 1 `SwipingCards` component with a swipe-fed event history.

**Architecture:** A hoisted-state navigator (`GalleryApp`) routes between a gallery list and three example screens. Each example calls the public `SwipingCards<T>` API with a different `Modifier` aspect ratio and content, and records swipe outcomes into a per-screen `EventHistoryState`. Domain event strings come from small pure functions that are JVM-unit-tested. Dating/D&D use a replaceable `Artwork` type (placeholder now, `.webp` later); Bank is fully procedural.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), the local `:swipingcards` library, JUnit 4 for JVM unit tests.

## Global Constraints

- **Android-only.** No `commonMain`, no KMP, no iOS work — deferred to Epic 3. Use ordinary Android Compose.
- **Public API only.** Examples consume `SwipingCards`, `SwipeResult`, `SwipeDirection` from `com.maxkach.swipingcards`. Never fork or copy the deck; never import library internals.
- **Deck sizing via `Modifier` only.** No hardcoded card dimensions; each deck sets its aspect ratio on the `SwipingCards` modifier.
- **Direction semantics (total over all 4 directions):** positive action ← `Right` or `Up`; negative action ← `Left` or `Down`.
- **Assets:** `.webp` in `sample/src/main/res/drawable/`, lowercase-underscore names. Dating/D&D ship replaceable placeholders now. No bitmaps for Bank.
- **Copy rules:** Bank data is fictional and must show a "DEMO — NOT A REAL CARD" marker; must not resemble a real payment/security flow. D&D uses original names (no WotC-owned named characters). No third-party dating/game branding.
- **Package root:** `com.maxkach.swipingcardssample`.
- **Test command:** `./gradlew :sample:testDebugUnitTest`. **Build command:** `./gradlew :sample:assembleDebug`.

---

### Task 1: Test setup + swipe semantics

**Files:**
- Modify: `sample/build.gradle.kts` (add `testImplementation`)
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/common/SwipeSemantics.kt`
- Test: `sample/src/test/java/com/maxkach/swipingcardssample/common/SwipeSemanticsTest.kt`

**Interfaces:**
- Produces: `fun isPositiveSwipe(direction: SwipeDirection): Boolean` in package `com.maxkach.swipingcardssample.common`.

- [ ] **Step 1: Add the JVM test dependency**

In `sample/build.gradle.kts`, add to the `dependencies { }` block (after the last line, before the closing brace):

```kotlin
    testImplementation(libs.junit)
```

- [ ] **Step 2: Write the failing test**

Create `sample/src/test/java/com/maxkach/swipingcardssample/common/SwipeSemanticsTest.kt`:

```kotlin
package com.maxkach.swipingcardssample.common

import com.maxkach.swipingcards.SwipeDirection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeSemanticsTest {
    @Test fun right_is_positive() = assertTrue(isPositiveSwipe(SwipeDirection.Right))
    @Test fun up_is_positive() = assertTrue(isPositiveSwipe(SwipeDirection.Up))
    @Test fun left_is_negative() = assertFalse(isPositiveSwipe(SwipeDirection.Left))
    @Test fun down_is_negative() = assertFalse(isPositiveSwipe(SwipeDirection.Down))
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :sample:testDebugUnitTest --tests "*SwipeSemanticsTest*"`
Expected: FAIL — unresolved reference `isPositiveSwipe`.

- [ ] **Step 4: Write the minimal implementation**

Create `sample/src/main/java/com/maxkach/swipingcardssample/common/SwipeSemantics.kt`:

```kotlin
package com.maxkach.swipingcardssample.common

import com.maxkach.swipingcards.SwipeDirection

/** A swipe is a positive action (like/select/recruit) when it goes Right or Up. */
fun isPositiveSwipe(direction: SwipeDirection): Boolean =
    direction == SwipeDirection.Right || direction == SwipeDirection.Up
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :sample:testDebugUnitTest --tests "*SwipeSemanticsTest*"`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
git add sample/build.gradle.kts sample/src/main/java/com/maxkach/swipingcardssample/common/SwipeSemantics.kt sample/src/test/java/com/maxkach/swipingcardssample/common/SwipeSemanticsTest.kt
git commit -m "feat(sample): add JVM test setup and swipe semantics helper"
```

---

### Task 2: Event history state + view

**Files:**
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/common/EventHistory.kt`
- Test: `sample/src/test/java/com/maxkach/swipingcardssample/common/EventHistoryTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `fun appendCapped(current: List<String>, entry: String, max: Int = 5): List<String>` — most-recent-first, capped.
  - `class EventHistoryState { val entries: List<String>; fun record(entry: String) }`
  - `@Composable fun rememberEventHistoryState(): EventHistoryState`
  - `@Composable fun EventHistoryView(state: EventHistoryState, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the failing test**

Create `sample/src/test/java/com/maxkach/swipingcardssample/common/EventHistoryTest.kt`:

```kotlin
package com.maxkach.swipingcardssample.common

import org.junit.Assert.assertEquals
import org.junit.Test

class EventHistoryTest {
    @Test fun newest_entry_is_first() {
        val r = appendCapped(listOf("a"), "b")
        assertEquals(listOf("b", "a"), r)
    }

    @Test fun caps_at_max_dropping_oldest() {
        val start = listOf("d", "c", "b", "a", "z") // already 5, newest-first
        val r = appendCapped(start, "e", max = 5)
        assertEquals(listOf("e", "d", "c", "b", "a"), r)
    }

    @Test fun empty_start_yields_single_entry() {
        assertEquals(listOf("only"), appendCapped(emptyList(), "only"))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :sample:testDebugUnitTest --tests "*EventHistoryTest*"`
Expected: FAIL — unresolved reference `appendCapped`.

- [ ] **Step 3: Write the implementation**

Create `sample/src/main/java/com/maxkach/swipingcardssample/common/EventHistory.kt`:

```kotlin
package com.maxkach.swipingcardssample.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val MAX_HISTORY = 5

/** Prepend [entry] (newest-first) and cap the list at [max], dropping the oldest. */
fun appendCapped(current: List<String>, entry: String, max: Int = MAX_HISTORY): List<String> =
    (listOf(entry) + current).take(max)

/** Holds the recent swipe descriptions for one example screen. Not persisted. */
@Stable
class EventHistoryState {
    var entries by mutableStateOf<List<String>>(emptyList())
        private set

    fun record(entry: String) {
        entries = appendCapped(entries, entry)
    }
}

@Composable
fun rememberEventHistoryState(): EventHistoryState = remember { EventHistoryState() }

/** Secondary, muted list of the most recent swipe outcomes. */
@Composable
fun EventHistoryView(state: EventHistoryState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        if (state.entries.isEmpty()) {
            Text(
                text = "Swipe a card to begin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            state.entries.forEachIndexed { index, entry ->
                Text(
                    text = entry,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                        .copy(alpha = if (index == 0) 1f else 0.6f),
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :sample:testDebugUnitTest --tests "*EventHistoryTest*"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add sample/src/main/java/com/maxkach/swipingcardssample/common/EventHistory.kt sample/src/test/java/com/maxkach/swipingcardssample/common/EventHistoryTest.kt
git commit -m "feat(sample): add event history state, cap logic, and view"
```

---

### Task 3: Replaceable artwork + action hints

**Files:**
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/common/Artwork.kt`
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/common/ActionHints.kt`

**Interfaces:**
- Produces:
  - `sealed interface Artwork` with `Artwork.Placeholder(seed: Color, initials: String, contentDescription: String)` and `Artwork.Image(@DrawableRes resId: Int, contentDescription: String)`, each exposing `val contentDescription: String`.
  - `@Composable fun ArtworkImage(artwork: Artwork, modifier: Modifier = Modifier)`
  - `@Composable fun ActionHints(negative: String, positive: String, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the Artwork implementation**

Create `sample/src/main/java/com/maxkach/swipingcardssample/common/Artwork.kt`:

```kotlin
package com.maxkach.swipingcardssample.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Card artwork. Ships now as [Placeholder]; swap one line to [Image] once the
 * generated `.webp` files land in `res/drawable/`. No layout change is required.
 */
sealed interface Artwork {
    val contentDescription: String

    data class Placeholder(
        val seed: Color,
        val initials: String,
        override val contentDescription: String,
    ) : Artwork

    data class Image(
        @DrawableRes val resId: Int,
        override val contentDescription: String,
    ) : Artwork
}

@Composable
fun ArtworkImage(artwork: Artwork, modifier: Modifier = Modifier) {
    when (artwork) {
        is Artwork.Image -> Image(
            painter = painterResource(artwork.resId),
            contentDescription = artwork.contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )

        is Artwork.Placeholder -> PlaceholderArt(artwork, modifier)
    }
}

@Composable
private fun PlaceholderArt(placeholder: Artwork.Placeholder, modifier: Modifier) {
    val darker = Color(
        red = placeholder.seed.red * 0.55f,
        green = placeholder.seed.green * 0.55f,
        blue = placeholder.seed.blue * 0.55f,
        alpha = 1f,
    )
    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(placeholder.seed, darker)))
            .semantics { contentDescription = placeholder.contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = placeholder.initials,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp,
        )
        // A visible "this is a placeholder" marker in the corner.
        Icon(
            imageVector = Icons.Outlined.BrokenImage,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(20.dp),
        )
    }
}
```

- [ ] **Step 2: Write the ActionHints implementation**

Create `sample/src/main/java/com/maxkach/swipingcardssample/common/ActionHints.kt`:

```kotlin
package com.maxkach.swipingcardssample.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/** Static "← negative | positive →" hints, since the library exposes no drag overlay. */
@Composable
fun ActionHints(
    negative: String,
    positive: String,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("← $negative", color = contentColor.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
        Text("$positive →", color = contentColor.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
    }
}
```

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew :sample:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add sample/src/main/java/com/maxkach/swipingcardssample/common/Artwork.kt sample/src/main/java/com/maxkach/swipingcardssample/common/ActionHints.kt
git commit -m "feat(sample): add replaceable Artwork type and action hints"
```

---

### Task 4: Gallery shell, navigation, and stub screens

**Files:**
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/gallery/Destination.kt`
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/gallery/GalleryScreen.kt`
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/gallery/GalleryApp.kt`
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/dating/DatingExampleScreen.kt` (stub)
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/bank/BankExampleScreen.kt` (stub)
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/dnd/DndExampleScreen.kt` (stub)
- Modify: `sample/src/main/java/com/maxkach/swipingcardssample/MainActivity.kt`
- Delete: `sample/src/main/java/com/maxkach/swipingcardssample/SwipingCardsSampleScreen.kt`
- Delete: `sample/src/main/java/com/maxkach/swipingcardssample/SampleIdeaCards.kt`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces:
  - `enum class Destination(val title: String, val blurb: String)` with `Dating`, `Bank`, `Dnd`.
  - `@Composable fun GalleryApp()`
  - `@Composable fun DatingExampleScreen(onBack: () -> Unit)` (stub now; filled in Task 7)
  - `@Composable fun BankExampleScreen(onBack: () -> Unit)` (stub now; filled in Task 9)
  - `@Composable fun DndExampleScreen(onBack: () -> Unit)` (stub now; filled in Task 11)

- [ ] **Step 1: Create the destinations**

Create `sample/src/main/java/com/maxkach/swipingcardssample/gallery/Destination.kt`:

```kotlin
package com.maxkach.swipingcardssample.gallery

/** The example demos listed in the gallery. */
enum class Destination(val title: String, val blurb: String) {
    Dating("Dating", "Tall 2:3 portrait cards with full-bleed art, name, and interests."),
    Bank("Bank cards", "Wide 1.586:1 landscape cards — the deck isn't tied to portrait."),
    Dnd("D&D", "Near-square 4:5 cards with framed art and a stat block."),
}
```

- [ ] **Step 2: Create the stub example screens**

Create `sample/src/main/java/com/maxkach/swipingcardssample/dating/DatingExampleScreen.kt`:

```kotlin
package com.maxkach.swipingcardssample.dating

import androidx.compose.runtime.Composable
import com.maxkach.swipingcardssample.gallery.ComingSoonScreen

@Composable
fun DatingExampleScreen(onBack: () -> Unit) = ComingSoonScreen("Dating", onBack)
```

Create `sample/src/main/java/com/maxkach/swipingcardssample/bank/BankExampleScreen.kt`:

```kotlin
package com.maxkach.swipingcardssample.bank

import androidx.compose.runtime.Composable
import com.maxkach.swipingcardssample.gallery.ComingSoonScreen

@Composable
fun BankExampleScreen(onBack: () -> Unit) = ComingSoonScreen("Bank cards", onBack)
```

Create `sample/src/main/java/com/maxkach/swipingcardssample/dnd/DndExampleScreen.kt`:

```kotlin
package com.maxkach.swipingcardssample.dnd

import androidx.compose.runtime.Composable
import com.maxkach.swipingcardssample.gallery.ComingSoonScreen

@Composable
fun DndExampleScreen(onBack: () -> Unit) = ComingSoonScreen("D&D", onBack)
```

- [ ] **Step 3: Create the gallery screen (list + shared scaffolding)**

Create `sample/src/main/java/com/maxkach/swipingcardssample/gallery/GalleryScreen.kt`:

```kotlin
package com.maxkach.swipingcardssample.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The gallery list: one tappable card per demo, with a one-line explanation. */
@Composable
fun GalleryScreen(onOpen: (Destination) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.systemBarsPadding().padding(16.dp)) {
            Text(
                text = "SwipingCards gallery",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = "Three demos, one deck component, three different card shapes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(Destination.entries) { destination ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(destination) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(destination.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    destination.blurb,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

/** Shared top bar with a back button, used by every example screen. */
@Composable
fun ExampleScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.systemBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to gallery")
                }
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            content(Modifier.weight(1f).fillMaxWidth())
        }
    }
}

/** Temporary placeholder body for demos not yet implemented. */
@Composable
fun ComingSoonScreen(title: String, onBack: () -> Unit) {
    ExampleScaffold(title = title, onBack = onBack) { modifier ->
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Coming soon", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
```

- [ ] **Step 4: Create the navigator**

Create `sample/src/main/java/com/maxkach/swipingcardssample/gallery/GalleryApp.kt`:

```kotlin
package com.maxkach.swipingcardssample.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.maxkach.swipingcardssample.bank.BankExampleScreen
import com.maxkach.swipingcardssample.dating.DatingExampleScreen
import com.maxkach.swipingcardssample.dnd.DndExampleScreen

/**
 * Hoisted-state navigator. `null` current = gallery. Each example screen owns its own
 * deck + history state; leaving disposes it, so demos can't corrupt each other's state.
 */
@Composable
fun GalleryApp() {
    var current by remember { mutableStateOf<Destination?>(null) }
    val back = { current = null }

    BackHandler(enabled = current != null, onBack = back)

    when (current) {
        null -> GalleryScreen(onOpen = { current = it })
        Destination.Dating -> DatingExampleScreen(onBack = back)
        Destination.Bank -> BankExampleScreen(onBack = back)
        Destination.Dnd -> DndExampleScreen(onBack = back)
    }
}
```

- [ ] **Step 5: Point MainActivity at the gallery and delete the old sample**

Replace the body of `sample/src/main/java/com/maxkach/swipingcardssample/MainActivity.kt` with:

```kotlin
package com.maxkach.swipingcardssample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.maxkach.swipingcardssample.gallery.GalleryApp
import com.maxkach.swipingcardssample.ui.theme.SwipingCardExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwipingCardExampleTheme {
                GalleryApp()
            }
        }
    }
}
```

Then delete the superseded files:

```bash
git rm sample/src/main/java/com/maxkach/swipingcardssample/SwipingCardsSampleScreen.kt \
       sample/src/main/java/com/maxkach/swipingcardssample/SampleIdeaCards.kt
```

- [ ] **Step 6: Build to verify it compiles**

Run: `./gradlew :sample:assembleDebug`
Expected: BUILD SUCCESSFUL (no remaining references to `SwipingCardsExampleScreen`/`sampleIdeas`).

- [ ] **Step 7: Run the app and verify the gallery**

Use the `run` skill (or `./gradlew :sample:installDebug` to an emulator/device). Verify: three cards listed (Dating, Bank cards, D&D); tapping one shows "Coming soon" with a working back arrow; system back also returns to the gallery.

- [ ] **Step 8: Commit**

```bash
git add sample/src/main/java/com/maxkach/swipingcardssample/gallery sample/src/main/java/com/maxkach/swipingcardssample/dating sample/src/main/java/com/maxkach/swipingcardssample/bank sample/src/main/java/com/maxkach/swipingcardssample/dnd sample/src/main/java/com/maxkach/swipingcardssample/MainActivity.kt
git commit -m "feat(sample): add gallery shell, navigation, and stub example screens"
```

**→ PR SLICE 1 boundary: "Shared gallery shell and navigation" is complete and shippable.**

---

### Task 5: Dating data + event label

**Files:**
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/dating/DatingProfile.kt`
- Test: `sample/src/test/java/com/maxkach/swipingcardssample/dating/DatingEventLabelTest.kt`

**Interfaces:**
- Consumes: `isPositiveSwipe` (Task 1), `Artwork` (Task 3).
- Produces:
  - `data class DatingProfile(name, age, tagline, interests: List<String>, artwork: Artwork)`
  - `val datingProfiles: List<DatingProfile>`
  - `fun datingEventLabel(direction: SwipeDirection, name: String): String`

- [ ] **Step 1: Write the failing test**

Create `sample/src/test/java/com/maxkach/swipingcardssample/dating/DatingEventLabelTest.kt`:

```kotlin
package com.maxkach.swipingcardssample.dating

import com.maxkach.swipingcards.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class DatingEventLabelTest {
    @Test fun right_likes() =
        assertEquals("Liked Mira", datingEventLabel(SwipeDirection.Right, "Mira"))

    @Test fun up_likes() =
        assertEquals("Liked Mira", datingEventLabel(SwipeDirection.Up, "Mira"))

    @Test fun left_passes() =
        assertEquals("Passed Rowan", datingEventLabel(SwipeDirection.Left, "Rowan"))

    @Test fun down_passes() =
        assertEquals("Passed Rowan", datingEventLabel(SwipeDirection.Down, "Rowan"))
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :sample:testDebugUnitTest --tests "*DatingEventLabelTest*"`
Expected: FAIL — unresolved reference `datingEventLabel`.

- [ ] **Step 3: Write the data + label implementation**

Create `sample/src/main/java/com/maxkach/swipingcardssample/dating/DatingProfile.kt`:

```kotlin
package com.maxkach.swipingcardssample.dating

import androidx.compose.ui.graphics.Color
import com.maxkach.swipingcards.SwipeDirection
import com.maxkach.swipingcardssample.common.Artwork
import com.maxkach.swipingcardssample.common.isPositiveSwipe

data class DatingProfile(
    val name: String,
    val age: Int,
    val tagline: String,
    val interests: List<String>,
    val artwork: Artwork,
)

private fun placeholder(seed: Long, initials: String, name: String) =
    Artwork.Placeholder(Color(seed), initials, "Portrait of $name")

// Swap each `artwork` to Artwork.Image(R.drawable.dating_<name>, "Portrait of <Name>")
// once the generated 2:3 .webp files land in res/drawable/.
val datingProfiles: List<DatingProfile> = listOf(
    DatingProfile("Mira", 27, "Grows plants and playlists in equal measure.",
        listOf("Botany", "Vinyl", "Baking"), placeholder(0xFF6DBE8A, "M", "Mira")),
    DatingProfile("Rowan", 29, "Bouldering by day, terrible puns always.",
        listOf("Climbing", "Puns", "Coffee"), placeholder(0xFFE0894C, "R", "Rowan")),
    DatingProfile("Kai", 26, "Jazz drummer chasing the perfect late set.",
        listOf("Jazz", "Drums", "Ramen"), placeholder(0xFF5B6DC6, "K", "Kai")),
    DatingProfile("Sol", 31, "Throws clay, brews tea, keeps it calm.",
        listOf("Ceramics", "Tea", "Hiking"), placeholder(0xFFC9A66B, "S", "Sol")),
    DatingProfile("Nova", 28, "Ships indie games and sci-fi opinions.",
        listOf("Gamedev", "Sci-fi", "Synths"), placeholder(0xFFB06CC6, "N", "Nova")),
)

fun datingEventLabel(direction: SwipeDirection, name: String): String =
    if (isPositiveSwipe(direction)) "Liked $name" else "Passed $name"
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :sample:testDebugUnitTest --tests "*DatingEventLabelTest*"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add sample/src/main/java/com/maxkach/swipingcardssample/dating/DatingProfile.kt sample/src/test/java/com/maxkach/swipingcardssample/dating/DatingEventLabelTest.kt
git commit -m "feat(sample): add dating roster and event label"
```

---

### Task 6: Dating card view

**Files:**
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/dating/DatingCard.kt`

**Interfaces:**
- Consumes: `DatingProfile` (Task 5), `ArtworkImage` + `ActionHints` (Task 3).
- Produces: `@Composable fun DatingCard(profile: DatingProfile, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the card view**

Create `sample/src/main/java/com/maxkach/swipingcardssample/dating/DatingCard.kt`:

```kotlin
package com.maxkach.swipingcardssample.dating

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxkach.swipingcardssample.common.ActionHints
import com.maxkach.swipingcardssample.common.ArtworkImage

@Composable
fun DatingCard(profile: DatingProfile, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Box(modifier.fillMaxSize().clip(shape)) {
        ArtworkImage(profile.artwork, Modifier.fillMaxSize())
        // Bottom scrim so text stays legible over any artwork.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.75f),
                    )
                )
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "${profile.name}, ${profile.age}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
            )
            Text(profile.tagline, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                profile.interests.forEach { interest ->
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            interest,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            ActionHints(negative = "Pass", positive = "Like", modifier = Modifier.padding(top = 4.dp))
        }
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew :sample:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add sample/src/main/java/com/maxkach/swipingcardssample/dating/DatingCard.kt
git commit -m "feat(sample): add dating card view"
```

---

### Task 7: Dating example screen (replace stub)

**Files:**
- Modify: `sample/src/main/java/com/maxkach/swipingcardssample/dating/DatingExampleScreen.kt`

**Interfaces:**
- Consumes: `SwipingCards` (library), `DatingCard`, `datingProfiles`, `datingEventLabel`, `ExampleScaffold`, `rememberEventHistoryState` + `EventHistoryView`.

- [ ] **Step 1: Replace the stub with the real screen**

Replace the entire contents of `sample/src/main/java/com/maxkach/swipingcardssample/dating/DatingExampleScreen.kt`:

```kotlin
package com.maxkach.swipingcardssample.dating

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maxkach.swipingcards.SwipingCards
import com.maxkach.swipingcardssample.common.EventHistoryView
import com.maxkach.swipingcardssample.common.rememberEventHistoryState
import com.maxkach.swipingcardssample.gallery.ExampleScaffold

@Composable
fun DatingExampleScreen(onBack: () -> Unit) {
    val history = rememberEventHistoryState()
    ExampleScaffold(title = "Dating", onBack = onBack) { modifier ->
        Box(modifier, contentAlignment = Alignment.Center) {
            SwipingCards(
                cards = datingProfiles,
                key = { it.name },
                modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(2f / 3f),
                onSwipe = { result -> history.record(datingEventLabel(result.direction, result.card.name)) },
            ) { profile -> DatingCard(profile) }
        }
        EventHistoryView(history, Modifier.padding(bottom = 16.dp))
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew :sample:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run the app and verify the dating demo**

Via the `run` skill: open Dating. Verify tall portrait cards, infinite rotation (swipe more than 5 times — cards keep coming), each swipe adds "Liked …"/"Passed …" newest-first (max 5), and back returns to the gallery.

- [ ] **Step 4: Commit**

```bash
git add sample/src/main/java/com/maxkach/swipingcardssample/dating/DatingExampleScreen.kt
git commit -m "feat(sample): implement dating example screen with event history"
```

**→ PR SLICE 2 boundary: "Dating example and event history pattern" is complete and shippable.**

---

### Task 8: Bank data, event label, and card view

**Files:**
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/bank/BankCard.kt`
- Test: `sample/src/test/java/com/maxkach/swipingcardssample/bank/BankEventLabelTest.kt`

**Interfaces:**
- Consumes: `isPositiveSwipe` (Task 1), `ActionHints` (Task 3).
- Produces:
  - `data class BankCard(product, last4, holder, expiry, perk, gradient: List<Color>)`
  - `val bankCards: List<BankCard>`
  - `fun bankEventLabel(direction: SwipeDirection, product: String, last4: String): String`
  - `@Composable fun BankCardView(card: BankCard, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the failing test**

Create `sample/src/test/java/com/maxkach/swipingcardssample/bank/BankEventLabelTest.kt`:

```kotlin
package com.maxkach.swipingcardssample.bank

import com.maxkach.swipingcards.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class BankEventLabelTest {
    @Test fun right_selects_by_last4() =
        assertEquals("Selected card ending 4242",
            bankEventLabel(SwipeDirection.Right, "Aurora Everyday", "4242"))

    @Test fun up_selects_by_last4() =
        assertEquals("Selected card ending 4242",
            bankEventLabel(SwipeDirection.Up, "Aurora Everyday", "4242"))

    @Test fun left_skips_by_product() =
        assertEquals("Skipped Zephyr Travel",
            bankEventLabel(SwipeDirection.Left, "Zephyr Travel", "8817"))

    @Test fun down_skips_by_product() =
        assertEquals("Skipped Zephyr Travel",
            bankEventLabel(SwipeDirection.Down, "Zephyr Travel", "8817"))
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :sample:testDebugUnitTest --tests "*BankEventLabelTest*"`
Expected: FAIL — unresolved reference `bankEventLabel`.

- [ ] **Step 3: Write the data, label, and card view**

Create `sample/src/main/java/com/maxkach/swipingcardssample/bank/BankCard.kt`:

```kotlin
package com.maxkach.swipingcardssample.bank

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxkach.swipingcards.SwipeDirection
import com.maxkach.swipingcardssample.common.ActionHints
import com.maxkach.swipingcardssample.common.isPositiveSwipe

data class BankCard(
    val product: String,
    val last4: String,
    val holder: String,
    val expiry: String,
    val perk: String,
    val gradient: List<Color>,
)

// All data is fictional. These are not real cards and imply no real payment flow.
val bankCards: List<BankCard> = listOf(
    BankCard("Aurora Everyday", "4242", "A. EXAMPLE", "08/29", "1% back on everything",
        listOf(Color(0xFF3A7BD5), Color(0xFF00D2FF))),
    BankCard("Zephyr Travel", "8817", "A. EXAMPLE", "11/28", "No foreign transaction fees",
        listOf(Color(0xFF654EA3), Color(0xFFEAAFC8))),
    BankCard("Nimbus Cashback", "3390", "A. EXAMPLE", "03/27", "3% back on groceries",
        listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
    BankCard("Onyx Reserve", "1265", "A. EXAMPLE", "06/30", "Airport lounge access",
        listOf(Color(0xFF232526), Color(0xFF414345))),
    BankCard("Coral Student", "7734", "A. EXAMPLE", "09/28", "No annual fee",
        listOf(Color(0xFFFF5F6D), Color(0xFFFFC371))),
)

fun bankEventLabel(direction: SwipeDirection, product: String, last4: String): String =
    if (isPositiveSwipe(direction)) "Selected card ending $last4" else "Skipped $product"

@Composable
fun BankCardView(card: BankCard, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(Brush.linearGradient(card.gradient)),
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(card.product, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("DEMO — NOT A REAL CARD", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
            }
            Spacer(Modifier.height(16.dp))
            // Procedural "chip" — abstract, not a real card network mark.
            Box(
                Modifier
                    .size(width = 40.dp, height = 30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.75f)),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "••••  ••••  ••••  ${card.last4}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CARDHOLDER", color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp)
                    Text(card.holder, color = Color.White, fontSize = 13.sp)
                }
                Column {
                    Text("EXPIRES", color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp)
                    Text(card.expiry, color = Color.White, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(card.perk, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            ActionHints(negative = "Skip", positive = "Select", modifier = Modifier.padding(top = 8.dp))
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :sample:testDebugUnitTest --tests "*BankEventLabelTest*"`
Expected: PASS (4 tests).

- [ ] **Step 5: Build to verify the view compiles**

Run: `./gradlew :sample:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add sample/src/main/java/com/maxkach/swipingcardssample/bank/BankCard.kt sample/src/test/java/com/maxkach/swipingcardssample/bank/BankEventLabelTest.kt
git commit -m "feat(sample): add bank roster, event label, and procedural card view"
```

---

### Task 9: Bank example screen (replace stub)

**Files:**
- Modify: `sample/src/main/java/com/maxkach/swipingcardssample/bank/BankExampleScreen.kt`

**Interfaces:**
- Consumes: `SwipingCards`, `BankCardView`, `bankCards`, `bankEventLabel`, `ExampleScaffold`, event-history helpers.

- [ ] **Step 1: Replace the stub with the real screen**

Replace the entire contents of `sample/src/main/java/com/maxkach/swipingcardssample/bank/BankExampleScreen.kt`:

```kotlin
package com.maxkach.swipingcardssample.bank

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maxkach.swipingcards.SwipingCards
import com.maxkach.swipingcardssample.common.EventHistoryView
import com.maxkach.swipingcardssample.common.rememberEventHistoryState
import com.maxkach.swipingcardssample.gallery.ExampleScaffold

@Composable
fun BankExampleScreen(onBack: () -> Unit) {
    val history = rememberEventHistoryState()
    ExampleScaffold(title = "Bank cards", onBack = onBack) { modifier ->
        Box(modifier, contentAlignment = Alignment.Center) {
            SwipingCards(
                cards = bankCards,
                key = { it.last4 },
                modifier = Modifier.fillMaxWidth(0.92f).aspectRatio(1.586f),
                onSwipe = { result ->
                    history.record(bankEventLabel(result.direction, result.card.product, result.card.last4))
                },
            ) { card -> BankCardView(card) }
        }
        EventHistoryView(history, Modifier.padding(bottom = 16.dp))
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew :sample:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run the app and verify the bank demo**

Via the `run` skill: open Bank cards. Verify wide landscape cards (clearly different shape from Dating), the DEMO marker is visible, infinite rotation, and swipes add "Selected card ending 4242"/"Skipped …". Confirm returning to the gallery and reopening Dating still works (no state corruption).

- [ ] **Step 4: Commit**

```bash
git add sample/src/main/java/com/maxkach/swipingcardssample/bank/BankExampleScreen.kt
git commit -m "feat(sample): implement bank example screen with landscape sizing"
```

**→ PR SLICE 3 boundary: "Bank-card example with landscape sizing" is complete and shippable.**

---

### Task 10: D&D data, event label, and card view

**Files:**
- Create: `sample/src/main/java/com/maxkach/swipingcardssample/dnd/DndCard.kt`
- Test: `sample/src/test/java/com/maxkach/swipingcardssample/dnd/DndEventLabelTest.kt`

**Interfaces:**
- Consumes: `isPositiveSwipe` (Task 1), `Artwork` + `ArtworkImage` + `ActionHints` (Task 3).
- Produces:
  - `data class DndCard(name, role, hp, ac, cr, str, dex, flavor, artwork: Artwork)`
  - `val dndCards: List<DndCard>`
  - `fun dndEventLabel(direction: SwipeDirection, name: String): String`
  - `@Composable fun DndCardView(card: DndCard, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the failing test**

Create `sample/src/test/java/com/maxkach/swipingcardssample/dnd/DndEventLabelTest.kt`:

```kotlin
package com.maxkach.swipingcardssample.dnd

import com.maxkach.swipingcards.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class DndEventLabelTest {
    @Test fun right_recruits() =
        assertEquals("Recruited Lila Underbough",
            dndEventLabel(SwipeDirection.Right, "Lila Underbough"))

    @Test fun up_recruits() =
        assertEquals("Recruited Lila Underbough",
            dndEventLabel(SwipeDirection.Up, "Lila Underbough"))

    @Test fun left_rejects() =
        assertEquals("Rejected Gruk the Unbroken",
            dndEventLabel(SwipeDirection.Left, "Gruk the Unbroken"))

    @Test fun down_rejects() =
        assertEquals("Rejected Gruk the Unbroken",
            dndEventLabel(SwipeDirection.Down, "Gruk the Unbroken"))
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :sample:testDebugUnitTest --tests "*DndEventLabelTest*"`
Expected: FAIL — unresolved reference `dndEventLabel`.

- [ ] **Step 3: Write the data, label, and card view**

Create `sample/src/main/java/com/maxkach/swipingcardssample/dnd/DndCard.kt`:

```kotlin
package com.maxkach.swipingcardssample.dnd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxkach.swipingcards.SwipeDirection
import com.maxkach.swipingcardssample.common.ActionHints
import com.maxkach.swipingcardssample.common.Artwork
import com.maxkach.swipingcardssample.common.ArtworkImage
import com.maxkach.swipingcardssample.common.isPositiveSwipe

data class DndCard(
    val name: String,
    val role: String,
    val hp: Int,
    val ac: Int,
    val cr: String,
    val str: Int,
    val dex: Int,
    val flavor: String,
    val artwork: Artwork,
)

private val parchment = Color(0xFFEFE3C8)
private val ink = Color(0xFF2B2118)

private fun art(seed: Long, initials: String, name: String) =
    Artwork.Placeholder(Color(seed), initials, "Illustration of $name")

// Swap each `artwork` to Artwork.Image(R.drawable.dnd_<role>, "Illustration of <Name>")
// once the generated 1:1 .webp files land in res/drawable/.
val dndCards: List<DndCard> = listOf(
    DndCard("Lila Underbough", "Halfling Rogue", 22, 15, "3", 9, 17,
        "Never met a lock she liked.", art(0xFF7A5C3E, "L", "Lila Underbough")),
    DndCard("Kaelen Ashborn", "Tiefling Warlock", 27, 12, "4", 10, 14,
        "Made a deal she intends to break.", art(0xFF6B2D8C, "K", "Kaelen Ashborn")),
    DndCard("Gruk the Unbroken", "Half-Orc Barbarian", 45, 13, "5", 18, 12,
        "Anger is a renewable resource.", art(0xFF3E6B2D, "G", "Gruk the Unbroken")),
    DndCard("Sylvara Nightbreeze", "Elf Ranger", 30, 14, "4", 12, 16,
        "Two arrows already in the air.", art(0xFF2D5C6B, "S", "Sylvara Nightbreeze")),
    DndCard("Ser Aldric Vane", "Human Fighter", 38, 18, "5", 16, 11,
        "Holds the line, every time.", art(0xFFB5952F, "A", "Ser Aldric Vane")),
)

fun dndEventLabel(direction: SwipeDirection, name: String): String =
    if (isPositiveSwipe(direction)) "Recruited $name" else "Rejected $name"

@Composable
fun DndCardView(card: DndCard, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(parchment)
            .border(3.dp, ink.copy(alpha = 0.4f), shape)
            .padding(10.dp),
    ) {
        // Framed square art window — different hierarchy from the full-bleed dating card.
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .border(2.dp, ink.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
        ) {
            ArtworkImage(card.artwork, Modifier.fillMaxSize())
        }
        Spacer(Modifier.padding(top = 8.dp))
        Text(card.name, color = ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(card.role, color = ink.copy(alpha = 0.8f), fontSize = 12.sp)
        Spacer(Modifier.padding(top = 6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("HP", card.hp.toString())
            Stat("AC", card.ac.toString())
            Stat("CR", card.cr)
            Stat("STR", card.str.toString())
            Stat("DEX", card.dex.toString())
        }
        Spacer(Modifier.padding(top = 6.dp))
        Text(card.flavor, color = ink.copy(alpha = 0.7f), fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        ActionHints(negative = "Reject", positive = "Recruit", contentColor = ink)
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = ink.copy(alpha = 0.6f), fontSize = 9.sp, textAlign = TextAlign.Center)
        Text(value, color = ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :sample:testDebugUnitTest --tests "*DndEventLabelTest*"`
Expected: PASS (4 tests).

- [ ] **Step 5: Build to verify the view compiles**

Run: `./gradlew :sample:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add sample/src/main/java/com/maxkach/swipingcardssample/dnd/DndCard.kt sample/src/test/java/com/maxkach/swipingcardssample/dnd/DndEventLabelTest.kt
git commit -m "feat(sample): add D&D roster, event label, and framed card view"
```

---

### Task 11: D&D example screen + final verification

**Files:**
- Modify: `sample/src/main/java/com/maxkach/swipingcardssample/dnd/DndExampleScreen.kt`

**Interfaces:**
- Consumes: `SwipingCards`, `DndCardView`, `dndCards`, `dndEventLabel`, `ExampleScaffold`, event-history helpers.

- [ ] **Step 1: Replace the stub with the real screen**

Replace the entire contents of `sample/src/main/java/com/maxkach/swipingcardssample/dnd/DndExampleScreen.kt`:

```kotlin
package com.maxkach.swipingcardssample.dnd

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maxkach.swipingcards.SwipingCards
import com.maxkach.swipingcardssample.common.EventHistoryView
import com.maxkach.swipingcardssample.common.rememberEventHistoryState
import com.maxkach.swipingcardssample.gallery.ExampleScaffold

@Composable
fun DndExampleScreen(onBack: () -> Unit) {
    val history = rememberEventHistoryState()
    ExampleScaffold(title = "D&D", onBack = onBack) { modifier ->
        Box(modifier, contentAlignment = Alignment.Center) {
            SwipingCards(
                cards = dndCards,
                key = { it.name },
                modifier = Modifier.fillMaxWidth(0.82f).aspectRatio(4f / 5f),
                onSwipe = { result -> history.record(dndEventLabel(result.direction, result.card.name)) },
            ) { card -> DndCardView(card) }
        }
        EventHistoryView(history, Modifier.padding(bottom = 16.dp))
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew :sample:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run the full unit-test suite**

Run: `./gradlew :sample:testDebugUnitTest`
Expected: PASS — all label/history/semantics tests (SwipeSemantics, EventHistory, Dating, Bank, D&D).

- [ ] **Step 4: Run the app and verify the whole gallery end-to-end**

Via the `run` skill, walk all three demos and confirm the acceptance criteria:
- Gallery lists all three examples.
- Each deck rotates indefinitely (swipe past the roster size).
- The three decks are visibly different shapes (2:3 tall / 1.586:1 wide / 4:5 near-square) with different content structure.
- Each swipe adds a correct newest-first history entry (max 5).
- Navigating between demos and back never corrupts deck state (open Dating → back → Bank → back → D&D, then re-open each).

- [ ] **Step 5: Commit**

```bash
git add sample/src/main/java/com/maxkach/swipingcardssample/dnd/DndExampleScreen.kt
git commit -m "feat(sample): implement D&D example screen and complete the gallery"
```

**→ PR SLICE 4 boundary: "D&D example and final cross-platform polish" (Android scope) is complete.**

---

## Notes on deferred work (Epic 3)

- iOS launcher, `commonMain` extraction, and the "opens on iOS / shared Compose UI" acceptance criteria are **out of scope** here and handled in Epic 3.
- When generated artwork is ready: add `dating_*.webp` (2:3) and `dnd_*.webp` (1:1) to `sample/src/main/res/drawable/`, then change each roster entry's `artwork = placeholder(...)`/`art(...)` to `Artwork.Image(R.drawable.<name>, "<description>")`. No other code changes required.
