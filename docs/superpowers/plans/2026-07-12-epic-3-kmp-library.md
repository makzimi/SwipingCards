# Epic 3 — Compose Multiplatform library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the Android-only SwipingCards library and its example gallery into a Kotlin Multiplatform / Compose Multiplatform project with one shared Compose UI hosted by thin Android and iOS launcher apps.

**Architecture:** Convert `swipingcards` to a KMP Compose Multiplatform library (Android + iOS targets), source moving into `commonMain` almost verbatim. Move the entire gallery into a new `samples/shared` KMP module (`commonMain`), migrating image resources to Compose Multiplatform resources and hardware-back to an `expect`/`actual` abstraction. Add a thin `samples/androidApp` (a single `ComponentActivity`) and a `samples/iosApp` Xcode project that links the shared framework. Add `maven-publish` metadata verified via `publishToMavenLocal`.

**Tech Stack:** Kotlin 2.0.21, Compose Multiplatform 1.7.3, AGP 8.13.2, Gradle 8.13, JDK 21, Xcode 26.5. Kotlin/Native iOS targets `iosX64`, `iosArm64`, `iosSimulatorArm64`.

## Global Constraints

- **Kotlin 2.0.21** and **AGP 8.13.2** and **Gradle 8.13** are unchanged. Do NOT bump Kotlin.
- **Compose Multiplatform 1.7.3** (`org.jetbrains.compose`), paired with the Kotlin-bundled compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`, already aliased as `libs.plugins.kotlin.compose`).
- **The library never depends on any `samples` module.**
- **No `android.*`, `androidx.activity.*`, `androidx.annotation.*`, or `R.drawable` references may appear in any `commonMain` source set.** Platform-only behavior hides behind `internal expect`/`actual`.
- **Epic 1 behavior (infinite rotation, reconciliation) and the library's public API must remain unchanged.** The existing library unit tests are the contract; they must keep passing.
- **Publishing coordinates:** `group = "com.maxkach"`, artifact base `swipingcards`, `version = "0.1.0"`. No signing, no credentials, no Maven Central.
- Android config stays `compileSdk 36`, `minSdk 33`, `targetSdk 36`, `JavaVersion.VERSION_11` / `jvmTarget "11"`.
- Library package stays `com.maxkach.swipingcards`. Gallery package stays `com.maxkach.swipingcardssample`.
- iOS targets and simulator run must succeed on the local machine (Xcode 26.5, an installed iOS Simulator runtime).

---

## File Structure

Files created or modified, by responsibility:

- `gradle/libs.versions.toml` — add CMP plugin/version + KMP plugin aliases.
- `build.gradle.kts` (root) — declare new plugin aliases `apply false`.
- `gradle.properties` — KMP/AGP compatibility flags.
- `settings.gradle.kts` — module membership: keep `:swipingcards`, drop `:sample`, add `:samples:shared`, `:samples:androidApp`.
- `swipingcards/build.gradle.kts` — KMP + Compose + maven-publish config.
- `swipingcards/src/commonMain/kotlin/com/maxkach/swipingcards/*.kt` — the four core files (moved).
- `swipingcards/src/commonTest/kotlin/com/maxkach/swipingcards/*.kt` — the two test files (moved, `kotlin.test`).
- `swipingcards/README.md` — consumer documentation.
- `samples/shared/build.gradle.kts` — KMP + Compose + resources + iOS framework.
- `samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/**` — the whole gallery (moved + edited).
- `samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/App.kt` — shared root composable.
- `samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/common/PlatformBackHandler.kt` — `expect`.
- `samples/shared/src/androidMain/kotlin/.../PlatformBackHandler.android.kt` — `actual`.
- `samples/shared/src/iosMain/kotlin/.../PlatformBackHandler.ios.kt` — `actual` (no-op).
- `samples/shared/src/iosMain/kotlin/.../MainViewController.kt` — iOS Compose entry point.
- `samples/shared/src/commonMain/composeResources/drawable/*.webp` — all art (moved).
- `samples/androidApp/build.gradle.kts`, `AndroidManifest.xml`, `MainActivity.kt`, launcher res — thin Android host.
- `samples/iosApp/iosApp.xcodeproj/project.pbxproj`, `iosApp/iOSApp.swift`, `iosApp/ContentView.swift`, `iosApp/Info.plist`, `iosApp/Assets.xcassets/**` — thin iOS host.
- `README.md` (root) — update module map.

---

## Task 1: Convert `swipingcards` to a KMP Compose Multiplatform library

Establishes the whole toolchain and proves the library logic on Android + iOS while the existing `sample` app keeps building.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (root — currently `build.gradle`; keep the existing file name)
- Modify: `gradle.properties`
- Rewrite: `swipingcards/build.gradle.kts` (currently `swipingcards/build.gradle`)
- Move: `swipingcards/src/main/java/com/maxkach/swipingcards/{DeckState,DeckReconciler,SwipeResult,SwipingCards}.kt` → `swipingcards/src/commonMain/kotlin/com/maxkach/swipingcards/`
- Move + edit: `swipingcards/src/test/java/com/maxkach/swipingcards/{DeckReconcilerTest,SwipeDirectionTest}.kt` → `swipingcards/src/commonTest/kotlin/com/maxkach/swipingcards/`
- Delete: `swipingcards/src/main/AndroidManifest.xml` (not needed for KMP Android target with no manifest entries), `swipingcards/proguard-rules.pro` if unused by the new build.

**Interfaces:**
- Produces: a KMP module `:swipingcards` with `androidTarget()`, `iosX64()`, `iosArm64()`, `iosSimulatorArm64()`, exposing the unchanged public API (`SwipingCards` composable, `SwipeResult`, `SwipeDirection`, `resolveSwipeDirection`, `DeckReconciler`, `DeckState`) in package `com.maxkach.swipingcards`.

- [ ] **Step 1: Add versions and plugin aliases to the catalog**

Edit `gradle/libs.versions.toml`. Under `[versions]` add:

```toml
composeMultiplatform = "1.7.3"
```

Under `[plugins]` add:

```toml
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
jetbrains-compose = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
```

- [ ] **Step 2: Declare the new plugins in the root build file**

The root build file is currently `build.gradle` (Groovy). Add the two aliases to its `plugins` block so it reads:

```groovy
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.compose) apply false
}
```

- [ ] **Step 3: Add KMP/AGP compatibility flags**

Append to `gradle.properties`:

```properties
# Compose Multiplatform 1.7.3 predates AGP 8.13; silence the version-compat check.
kotlin.mpp.androidGradlePluginCompatibility.nowarn=true
# Enable Compose resources / expect-actual without warnings.
kotlin.mpp.enableCInteropCommonization=true
```

- [ ] **Step 4: Move library sources into `commonMain` (unchanged content)**

Run:

```bash
mkdir -p swipingcards/src/commonMain/kotlin/com/maxkach/swipingcards
git mv swipingcards/src/main/java/com/maxkach/swipingcards/DeckState.kt       swipingcards/src/commonMain/kotlin/com/maxkach/swipingcards/DeckState.kt
git mv swipingcards/src/main/java/com/maxkach/swipingcards/DeckReconciler.kt  swipingcards/src/commonMain/kotlin/com/maxkach/swipingcards/DeckReconciler.kt
git mv swipingcards/src/main/java/com/maxkach/swipingcards/SwipeResult.kt     swipingcards/src/commonMain/kotlin/com/maxkach/swipingcards/SwipeResult.kt
git mv swipingcards/src/main/java/com/maxkach/swipingcards/SwipingCards.kt    swipingcards/src/commonMain/kotlin/com/maxkach/swipingcards/SwipingCards.kt
git rm swipingcards/src/main/AndroidManifest.xml
```

Do NOT edit the moved `.kt` files — every import is already multiplatform (`androidx.compose.*`, `kotlinx.coroutines.*`, `kotlin.math.*`).

- [ ] **Step 5: Move and convert the tests to `kotlin.test`**

Run:

```bash
mkdir -p swipingcards/src/commonTest/kotlin/com/maxkach/swipingcards
git mv swipingcards/src/test/java/com/maxkach/swipingcards/DeckReconcilerTest.kt swipingcards/src/commonTest/kotlin/com/maxkach/swipingcards/DeckReconcilerTest.kt
git mv swipingcards/src/test/java/com/maxkach/swipingcards/SwipeDirectionTest.kt swipingcards/src/commonTest/kotlin/com/maxkach/swipingcards/SwipeDirectionTest.kt
```

In `DeckReconcilerTest.kt` replace the JUnit imports:

```kotlin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
```

with:

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
```

Then replace the one `@Test(expected = ...)` case. Change:

```kotlin
    @Test(expected = IllegalArgumentException::class)
    fun requireUniqueKeys_throwsOnDuplicate() {
        DeckReconciler.requireUniqueKeys(listOf("A", "B", "A"))
    }
```

to:

```kotlin
    @Test
    fun requireUniqueKeys_throwsOnDuplicate() {
        assertFailsWith<IllegalArgumentException> {
            DeckReconciler.requireUniqueKeys(listOf("A", "B", "A"))
        }
    }
```

In `SwipeDirectionTest.kt` replace:

```kotlin
import org.junit.Assert.assertEquals
import org.junit.Test
```

with:

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
```

Note: `kotlin.test.assertEquals` takes `(expected, actual)` — the same argument order the tests already use, so no call-site changes.

- [ ] **Step 6: Write the KMP build file**

Delete the old `swipingcards/build.gradle` and create `swipingcards/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
}

group = "com.maxkach"
version = "0.1.0"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.maxkach.swipingcards"
    compileSdk = 36
    defaultConfig {
        minSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("SwipingCards")
            description.set("A Compose Multiplatform swipeable card deck.")
            url.set("https://github.com/makzimi/SwipingCards")
        }
    }
}
```

Add the coroutines library alias to `gradle/libs.versions.toml` if absent. Under `[versions]` add `kotlinxCoroutines = "1.9.0"` and under `[libraries]` add:

```toml
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
```

(`compose.runtime`, `compose.foundation`, etc. are provided by the `org.jetbrains.compose` plugin's `compose` DSL extension — no catalog entries needed.)

- [ ] **Step 7: Run the library unit tests on Android and iOS**

Run:

```bash
./gradlew :swipingcards:testDebugUnitTest :swipingcards:iosSimulatorArm64Test
```

Expected: BUILD SUCCESSFUL; all `DeckReconcilerTest` and `SwipeDirectionTest` cases pass on both the Android JVM unit-test target and the iOS simulator target.

- [ ] **Step 8: Confirm the existing Android sample still consumes the KMP library**

Run:

```bash
./gradlew :sample:assembleDebug
```

Expected: BUILD SUCCESSFUL. The `sample` app depends on `project(":swipingcards")` and Gradle resolves the Android variant of the now-KMP library. If it fails with a Compose version-skew error, it will be retired in Task 3 — but it should build here because CMP 1.7.3 maps to AndroidX Compose 1.7.x, compatible with the sample's BOM.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "Convert swipingcards to a Compose Multiplatform library

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Move the gallery into a shared KMP module (`samples/shared`)

Moves the entire gallery into `commonMain`, migrating images to Compose resources, back handling to `expect`/`actual`, and the theme off Android dynamic color. The old `sample` app is left untouched and still builds.

**Files:**
- Create: `samples/shared/build.gradle.kts`
- Move: everything under `sample/src/main/java/com/maxkach/swipingcardssample/{bank,common,dating,dnd,gallery,streaming,ui}` → `samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/...` (but NOT `MainActivity.kt`)
- Move: `sample/src/main/res/drawable/{dating_*,dnd_*,show_*}.webp` → `samples/shared/src/commonMain/composeResources/drawable/`
- Move + edit tests: `sample/src/test/java/com/maxkach/swipingcardssample/**` → `samples/shared/src/commonTest/kotlin/...`
- Edit: `common/Artwork.kt`, `dnd/DndCard.kt`, `dating/DatingProfile.kt`, `streaming/StreamingTitle.kt`, `ui/theme/Theme.kt`, `gallery/GalleryApp.kt`
- Create: `common/PlatformBackHandler.kt` (expect), `androidMain/.../PlatformBackHandler.android.kt`, `iosMain/.../PlatformBackHandler.ios.kt`, `App.kt`, `iosMain/.../MainViewController.kt`
- Modify: `settings.gradle` — add `include(":samples:shared")`

**Interfaces:**
- Consumes: `:swipingcards` (the `SwipingCards` composable and swipe models from Task 1).
- Produces: `@Composable fun App()` in `com.maxkach.swipingcardssample`; `fun MainViewController(): UIViewController` in the iOS framework; `internal expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)`.

- [ ] **Step 1: Register the module**

The `settings.gradle` uses Groovy. Change the includes so it reads:

```groovy
rootProject.name = "SwipingCard"
include(":swipingcards")
include(":sample")
include(":samples:shared")
```

(`:sample` stays for now; it is removed in Task 3.)

- [ ] **Step 2: Move gallery Kotlin sources into `commonMain`**

Run:

```bash
mkdir -p samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample
git mv sample/src/main/java/com/maxkach/swipingcardssample/bank      samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/bank
git mv sample/src/main/java/com/maxkach/swipingcardssample/common    samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/common
git mv sample/src/main/java/com/maxkach/swipingcardssample/dating    samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/dating
git mv sample/src/main/java/com/maxkach/swipingcardssample/dnd       samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/dnd
git mv sample/src/main/java/com/maxkach/swipingcardssample/gallery   samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/gallery
git mv sample/src/main/java/com/maxkach/swipingcardssample/streaming samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/streaming
git mv sample/src/main/java/com/maxkach/swipingcardssample/ui        samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/ui
```

Leave `sample/src/main/java/com/maxkach/swipingcardssample/MainActivity.kt` in place (retired in Task 3).

- [ ] **Step 3: Move the `.webp` art into Compose resources**

Run:

```bash
mkdir -p samples/shared/src/commonMain/composeResources/drawable
git mv sample/src/main/res/drawable/dating_kai.webp   samples/shared/src/commonMain/composeResources/drawable/dating_kai.webp
git mv sample/src/main/res/drawable/dating_mira.webp  samples/shared/src/commonMain/composeResources/drawable/dating_mira.webp
git mv sample/src/main/res/drawable/dating_nova.webp  samples/shared/src/commonMain/composeResources/drawable/dating_nova.webp
git mv sample/src/main/res/drawable/dating_rowan.webp samples/shared/src/commonMain/composeResources/drawable/dating_rowan.webp
git mv sample/src/main/res/drawable/dating_sol.webp   samples/shared/src/commonMain/composeResources/drawable/dating_sol.webp
git mv sample/src/main/res/drawable/dnd_barbarian.webp samples/shared/src/commonMain/composeResources/drawable/dnd_barbarian.webp
git mv sample/src/main/res/drawable/dnd_bard.webp      samples/shared/src/commonMain/composeResources/drawable/dnd_bard.webp
git mv sample/src/main/res/drawable/dnd_cleric.webp    samples/shared/src/commonMain/composeResources/drawable/dnd_cleric.webp
git mv sample/src/main/res/drawable/dnd_dragon.webp    samples/shared/src/commonMain/composeResources/drawable/dnd_dragon.webp
git mv sample/src/main/res/drawable/dnd_fighter.webp   samples/shared/src/commonMain/composeResources/drawable/dnd_fighter.webp
git mv sample/src/main/res/drawable/dnd_owlbear.webp   samples/shared/src/commonMain/composeResources/drawable/dnd_owlbear.webp
git mv sample/src/main/res/drawable/dnd_ranger.webp    samples/shared/src/commonMain/composeResources/drawable/dnd_ranger.webp
git mv sample/src/main/res/drawable/dnd_rogue.webp     samples/shared/src/commonMain/composeResources/drawable/dnd_rogue.webp
git mv sample/src/main/res/drawable/dnd_troll.webp     samples/shared/src/commonMain/composeResources/drawable/dnd_troll.webp
git mv sample/src/main/res/drawable/dnd_warlock.webp   samples/shared/src/commonMain/composeResources/drawable/dnd_warlock.webp
git mv sample/src/main/res/drawable/show_dark.webp              samples/shared/src/commonMain/composeResources/drawable/show_dark.webp
git mv sample/src/main/res/drawable/show_kpop_demon_hunters.webp samples/shared/src/commonMain/composeResources/drawable/show_kpop_demon_hunters.webp
git mv sample/src/main/res/drawable/show_mindhunter.webp        samples/shared/src/commonMain/composeResources/drawable/show_mindhunter.webp
git mv sample/src/main/res/drawable/show_squid_game.webp        samples/shared/src/commonMain/composeResources/drawable/show_squid_game.webp
git mv sample/src/main/res/drawable/show_stranger_things.webp   samples/shared/src/commonMain/composeResources/drawable/show_stranger_things.webp
git mv sample/src/main/res/drawable/show_the_crown.webp         samples/shared/src/commonMain/composeResources/drawable/show_the_crown.webp
```

- [ ] **Step 4: Write the shared module build file**

Create `samples/shared/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":swipingcards"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.maxkach.swipingcardssample"
    compileSdk = 36
    defaultConfig {
        minSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.maxkach.swipingcardssample.resources"
    generateResClass = always
}
```

- [ ] **Step 5: Add the `PlatformBackHandler` expect/actual abstraction**

Create `samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/common/PlatformBackHandler.kt`:

```kotlin
package com.maxkach.swipingcardssample.common

import androidx.compose.runtime.Composable

/**
 * Handles the platform hardware/system back action.
 * Android delegates to the activity back dispatcher; iOS is a no-op (each screen
 * provides an in-UI back button).
 */
@Composable
internal expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
```

Create `samples/shared/src/androidMain/kotlin/com/maxkach/swipingcardssample/common/PlatformBackHandler.android.kt`:

```kotlin
package com.maxkach.swipingcardssample.common

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
```

Create `samples/shared/src/iosMain/kotlin/com/maxkach/swipingcardssample/common/PlatformBackHandler.ios.kt`:

```kotlin
package com.maxkach.swipingcardssample.common

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses the in-UI back button; no system back to intercept.
}
```

- [ ] **Step 6: Point `GalleryApp` at `PlatformBackHandler`**

Edit `samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/gallery/GalleryApp.kt`. Replace the import:

```kotlin
import androidx.activity.compose.BackHandler
```

with:

```kotlin
import com.maxkach.swipingcardssample.common.PlatformBackHandler
```

and replace the call:

```kotlin
    BackHandler(enabled = current != null, onBack = back)
```

with:

```kotlin
    PlatformBackHandler(enabled = current != null, onBack = back)
```

- [ ] **Step 7: Migrate `Artwork` to Compose resources**

Overwrite `samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/common/Artwork.kt` — the only changes are the `Artwork.Image` payload type and the `painterResource` import/import source:

```kotlin
package com.maxkach.swipingcardssample.common

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Card artwork. [Image] renders a Compose Multiplatform drawable resource;
 * [Placeholder] renders a gradient with initials.
 */
sealed interface Artwork {
    val contentDescription: String

    data class Placeholder(
        val seed: Color,
        val initials: String,
        override val contentDescription: String,
    ) : Artwork

    data class Image(
        val resource: DrawableResource,
        override val contentDescription: String,
    ) : Artwork
}

@Composable
fun ArtworkImage(artwork: Artwork, modifier: Modifier = Modifier) {
    when (artwork) {
        is Artwork.Image -> Image(
            painter = painterResource(artwork.resource),
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

- [ ] **Step 8: Update the three call sites from `R.drawable.*` to `Res.drawable.*`**

In `dnd/DndCard.kt`, replace the import:

```kotlin
import com.maxkach.swipingcardssample.R
```

with:

```kotlin
import com.maxkach.swipingcardssample.resources.Res
import com.maxkach.swipingcardssample.resources.dnd_barbarian
import com.maxkach.swipingcardssample.resources.dnd_bard
import com.maxkach.swipingcardssample.resources.dnd_cleric
import com.maxkach.swipingcardssample.resources.dnd_dragon
import com.maxkach.swipingcardssample.resources.dnd_fighter
import com.maxkach.swipingcardssample.resources.dnd_owlbear
import com.maxkach.swipingcardssample.resources.dnd_ranger
import com.maxkach.swipingcardssample.resources.dnd_rogue
import com.maxkach.swipingcardssample.resources.dnd_troll
import com.maxkach.swipingcardssample.resources.dnd_warlock
```

and change every `R.drawable.dnd_x` to `Res.drawable.dnd_x` (e.g. `Artwork.Image(R.drawable.dnd_rogue, ...)` → `Artwork.Image(Res.drawable.dnd_rogue, ...)`). There are 10 occurrences: `dnd_rogue`, `dnd_warlock`, `dnd_barbarian`, `dnd_ranger`, `dnd_fighter`, `dnd_cleric`, `dnd_bard`, `dnd_dragon`, `dnd_owlbear`, `dnd_troll`.

In `dating/DatingProfile.kt`, replace `import com.maxkach.swipingcardssample.R` with:

```kotlin
import com.maxkach.swipingcardssample.resources.Res
import com.maxkach.swipingcardssample.resources.dating_kai
import com.maxkach.swipingcardssample.resources.dating_mira
import com.maxkach.swipingcardssample.resources.dating_nova
import com.maxkach.swipingcardssample.resources.dating_rowan
import com.maxkach.swipingcardssample.resources.dating_sol
```

and change the 5 `R.drawable.dating_x` to `Res.drawable.dating_x` (`dating_mira`, `dating_rowan`, `dating_kai`, `dating_sol`, `dating_nova`).

In `streaming/StreamingTitle.kt`, replace `import com.maxkach.swipingcardssample.R` with:

```kotlin
import com.maxkach.swipingcardssample.resources.Res
import com.maxkach.swipingcardssample.resources.show_dark
import com.maxkach.swipingcardssample.resources.show_kpop_demon_hunters
import com.maxkach.swipingcardssample.resources.show_mindhunter
import com.maxkach.swipingcardssample.resources.show_squid_game
import com.maxkach.swipingcardssample.resources.show_stranger_things
import com.maxkach.swipingcardssample.resources.show_the_crown
```

and change the 6 `R.drawable.show_x` to `Res.drawable.show_x` (`show_stranger_things`, `show_squid_game`, `show_the_crown`, `show_dark`, `show_mindhunter`, `show_kpop_demon_hunters`).

- [ ] **Step 9: Drop Android dynamic color from the shared theme**

Overwrite `samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/ui/theme/Theme.kt` (removes `android.os.Build` and `LocalContext`):

```kotlin
package com.maxkach.swipingcardssample.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

@Composable
fun SwipingCardExampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
```

- [ ] **Step 10: Add the shared `App()` entry point**

Create `samples/shared/src/commonMain/kotlin/com/maxkach/swipingcardssample/App.kt`:

```kotlin
package com.maxkach.swipingcardssample

import androidx.compose.runtime.Composable
import com.maxkach.swipingcardssample.gallery.GalleryApp
import com.maxkach.swipingcardssample.ui.theme.SwipingCardExampleTheme

/** Shared root of the example gallery, hosted identically on Android and iOS. */
@Composable
fun App() {
    SwipingCardExampleTheme {
        GalleryApp()
    }
}
```

- [ ] **Step 11: Add the iOS Compose entry point**

Create `samples/shared/src/iosMain/kotlin/com/maxkach/swipingcardssample/MainViewController.kt`:

```kotlin
package com.maxkach.swipingcardssample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Framework entry point consumed by the iOS app's SwiftUI wrapper. */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
```

- [ ] **Step 12: Move and convert the gallery unit tests**

Run:

```bash
mkdir -p samples/shared/src/commonTest/kotlin/com/maxkach/swipingcardssample
git mv sample/src/test/java/com/maxkach/swipingcardssample/bank      samples/shared/src/commonTest/kotlin/com/maxkach/swipingcardssample/bank
git mv sample/src/test/java/com/maxkach/swipingcardssample/common    samples/shared/src/commonTest/kotlin/com/maxkach/swipingcardssample/common
git mv sample/src/test/java/com/maxkach/swipingcardssample/dating    samples/shared/src/commonTest/kotlin/com/maxkach/swipingcardssample/dating
git mv sample/src/test/java/com/maxkach/swipingcardssample/dnd       samples/shared/src/commonTest/kotlin/com/maxkach/swipingcardssample/dnd
git mv sample/src/test/java/com/maxkach/swipingcardssample/streaming samples/shared/src/commonTest/kotlin/com/maxkach/swipingcardssample/streaming
```

Then in each moved test file, replace JUnit imports with `kotlin.test`:
- `import org.junit.Test` → `import kotlin.test.Test`
- `import org.junit.Assert.assertEquals` → `import kotlin.test.assertEquals`
- `import org.junit.Assert.assertTrue` → `import kotlin.test.assertTrue`
- `import org.junit.Assert.assertFalse` → `import kotlin.test.assertFalse`

Use grep to find every JUnit import that needs swapping:

```bash
grep -rln "org.junit" samples/shared/src/commonTest
```

For any `@Test(expected = X::class)` found, rewrite it to wrap the body in `kotlin.test.assertFailsWith<X> { ... }` (add `import kotlin.test.assertFailsWith`). If none are found, no change needed.

- [ ] **Step 13: Build the shared module for Android and link the iOS framework**

Run:

```bash
./gradlew :samples:shared:compileDebugKotlinAndroid :samples:shared:linkDebugFrameworkIosSimulatorArm64 :samples:shared:iosSimulatorArm64Test
```

Expected: BUILD SUCCESSFUL. The Compose resources plugin generates `com.maxkach.swipingcardssample.resources.Res` with `Res.drawable.*` accessors; the gallery compiles for Android and links as an iOS framework; the migrated gallery unit tests pass.

If the `Res` accessors do not resolve, run `./gradlew :samples:shared:generateComposeResClass` and re-check the generated package matches `packageOfResClass`.

- [ ] **Step 14: Commit**

```bash
git add -A
git commit -m "Move the example gallery into a shared Compose Multiplatform module

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Add the thin Android launcher and retire the old `sample` module

**Files:**
- Create: `samples/androidApp/build.gradle.kts`
- Create: `samples/androidApp/src/main/AndroidManifest.xml`
- Create: `samples/androidApp/src/main/kotlin/com/maxkach/swipingcardssample/MainActivity.kt`
- Move: `sample/src/main/res/{mipmap-*,values,xml,drawable/ic_launcher_*}` → `samples/androidApp/src/main/res/...`
- Modify: `settings.gradle` — replace `:sample` with `:samples:androidApp`
- Delete: the `sample/` directory

**Interfaces:**
- Consumes: `App()` from `:samples:shared`.

- [ ] **Step 1: Write the Android app build file**

Create `samples/androidApp/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.maxkach.swipingcardssample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.maxkach.swipingcardssample"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":samples:shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
}
```

Create `samples/androidApp/proguard-rules.pro` as an empty file:

```bash
touch samples/androidApp/proguard-rules.pro
```

- [ ] **Step 2: Write `MainActivity`**

Create `samples/androidApp/src/main/kotlin/com/maxkach/swipingcardssample/MainActivity.kt`:

```kotlin
package com.maxkach.swipingcardssample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
```

- [ ] **Step 3: Move the Android manifest and resources**

Run:

```bash
mkdir -p samples/androidApp/src/main
git mv sample/src/main/AndroidManifest.xml samples/androidApp/src/main/AndroidManifest.xml
git mv sample/src/main/res samples/androidApp/src/main/res
```

Then edit `samples/androidApp/src/main/AndroidManifest.xml` so the `<activity>` `android:name` is `.MainActivity` (it already is, since the package is unchanged). Verify the manifest references `@style/...`, `@mipmap/ic_launcher`, and `@string/app_name`, all of which moved with `res/`.

- [ ] **Step 4: Swap module membership**

Edit `settings.gradle` so it reads:

```groovy
rootProject.name = "SwipingCard"
include(":swipingcards")
include(":samples:shared")
include(":samples:androidApp")
```

- [ ] **Step 5: Delete the retired sample module**

Run:

```bash
git rm -r sample
```

- [ ] **Step 6: Build and smoke-test the Android app**

Run:

```bash
./gradlew :samples:androidApp:assembleDebug
```

Expected: BUILD SUCCESSFUL and an APK at `samples/androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

If an emulator or device is available, install and launch:

```bash
./gradlew :samples:androidApp:installDebug
adb shell am start -n com.maxkach.swipingcardssample/.MainActivity
```

Expected: the gallery lists Dating, Bank cards, D&D, Streaming; opening one shows cards; a swipe commits.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Add the thin Android launcher and retire the old sample module

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Add the iOS launcher and run on a simulator

**Files:**
- Create: `samples/iosApp/iosApp.xcodeproj/project.pbxproj`
- Create: `samples/iosApp/iosApp/iOSApp.swift`
- Create: `samples/iosApp/iosApp/ContentView.swift`
- Create: `samples/iosApp/iosApp/Info.plist`
- Create: `samples/iosApp/iosApp/Assets.xcassets/Contents.json` (+ `AppIcon.appiconset/Contents.json`, `AccentColor.colorset/Contents.json`)

**Interfaces:**
- Consumes: `MainViewController()` from the `Shared` framework built by `:samples:shared`.

- [ ] **Step 1: Write the SwiftUI host**

Create `samples/iosApp/iosApp/iOSApp.swift`:

```swift
import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
        }
    }
}
```

Create `samples/iosApp/iosApp/ContentView.swift`:

```swift
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}
```

- [ ] **Step 2: Write `Info.plist`**

Create `samples/iosApp/iosApp/Info.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>$(DEVELOPMENT_LANGUAGE)</string>
    <key>CFBundleExecutable</key>
    <string>$(EXECUTABLE_NAME)</string>
    <key>CFBundleIdentifier</key>
    <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>$(PRODUCT_NAME)</string>
    <key>CFBundlePackageType</key>
    <string>$(PRODUCT_BUNDLE_PACKAGE_TYPE)</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSRequiresIPhoneOS</key>
    <true/>
    <key>UILaunchScreen</key>
    <dict/>
    <key>UISupportedInterfaceOrientations</key>
    <array>
        <string>UIInterfaceOrientationPortrait</string>
    </array>
</dict>
</plist>
```

- [ ] **Step 3: Write asset catalog stubs**

Create `samples/iosApp/iosApp/Assets.xcassets/Contents.json`:

```json
{
  "info" : { "author" : "xcode", "version" : 1 }
}
```

Create `samples/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json`:

```json
{
  "images" : [
    { "idiom" : "universal", "platform" : "ios", "size" : "1024x1024" }
  ],
  "info" : { "author" : "xcode", "version" : 1 }
}
```

Create `samples/iosApp/iosApp/Assets.xcassets/AccentColor.colorset/Contents.json`:

```json
{
  "colors" : [ { "idiom" : "universal" } ],
  "info" : { "author" : "xcode", "version" : 1 }
}
```

- [ ] **Step 4: Write the Xcode project**

Create `samples/iosApp/iosApp.xcodeproj/project.pbxproj`. This is a minimal single-target iOS app project with a Run Script phase that builds and embeds the Kotlin framework. Use exactly:

```
// !$*UTF8*$!
{
	archiveVersion = 1;
	classes = {
	};
	objectVersion = 54;
	objects = {

/* Begin PBXBuildFile section */
		A1000002 /* iOSApp.swift in Sources */ = {isa = PBXBuildFile; fileRef = A1000001 /* iOSApp.swift */; };
		A1000004 /* ContentView.swift in Sources */ = {isa = PBXBuildFile; fileRef = A1000003 /* ContentView.swift */; };
		A1000006 /* Assets.xcassets in Resources */ = {isa = PBXBuildFile; fileRef = A1000005 /* Assets.xcassets */; };
/* End PBXBuildFile section */

/* Begin PBXFileReference section */
		A1000001 /* iOSApp.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = iOSApp.swift; sourceTree = "<group>"; };
		A1000003 /* ContentView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = ContentView.swift; sourceTree = "<group>"; };
		A1000005 /* Assets.xcassets */ = {isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = Assets.xcassets; sourceTree = "<group>"; };
		A1000007 /* Info.plist */ = {isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = "<group>"; };
		A1000008 /* iosApp.app */ = {isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = iosApp.app; sourceTree = BUILT_PRODUCTS_DIR; };
/* End PBXFileReference section */

/* Begin PBXFrameworksBuildPhase section */
		A1000009 /* Frameworks */ = {
			isa = PBXFrameworksBuildPhase;
			buildActionMask = 2147483647;
			files = (
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXFrameworksBuildPhase section */

/* Begin PBXGroup section */
		A1000010 = {
			isa = PBXGroup;
			children = (
				A1000012 /* iosApp */,
				A1000011 /* Products */,
			);
			sourceTree = "<group>";
		};
		A1000011 /* Products */ = {
			isa = PBXGroup;
			children = (
				A1000008 /* iosApp.app */,
			);
			name = Products;
			sourceTree = "<group>";
		};
		A1000012 /* iosApp */ = {
			isa = PBXGroup;
			children = (
				A1000001 /* iOSApp.swift */,
				A1000003 /* ContentView.swift */,
				A1000005 /* Assets.xcassets */,
				A1000007 /* Info.plist */,
			);
			path = iosApp;
			sourceTree = "<group>";
		};
/* End PBXGroup section */

/* Begin PBXNativeTarget section */
		A1000013 /* iosApp */ = {
			isa = PBXNativeTarget;
			buildConfigurationList = A1000016 /* Build configuration list for PBXNativeTarget "iosApp" */;
			buildPhases = (
				A1000020 /* Build Kotlin Framework */,
				A1000014 /* Sources */,
				A1000009 /* Frameworks */,
				A1000015 /* Resources */,
			);
			buildRules = (
			);
			dependencies = (
			);
			name = iosApp;
			productName = iosApp;
			productReference = A1000008 /* iosApp.app */;
			productType = "com.apple.product-type.application";
		};
/* End PBXNativeTarget section */

/* Begin PBXProject section */
		A1000017 /* Project object */ = {
			isa = PBXProject;
			attributes = {
				BuildIndependentTargetsInParallel = 1;
				LastSwiftUpdateCheck = 1500;
				LastUpgradeCheck = 1500;
				TargetAttributes = {
					A1000013 = {
						CreatedOnToolsVersion = 15.0;
					};
				};
			};
			buildConfigurationList = A1000018 /* Build configuration list for PBXProject "iosApp" */;
			compatibilityVersion = "Xcode 14.0";
			developmentRegion = en;
			hasScannedForEncodings = 0;
			knownRegions = (
				en,
				Base,
			);
			mainGroup = A1000010;
			productRefGroup = A1000011 /* Products */;
			projectDirPath = "";
			projectRoot = "";
			targets = (
				A1000013 /* iosApp */,
			);
		};
/* End PBXProject section */

/* Begin PBXResourcesBuildPhase section */
		A1000015 /* Resources */ = {
			isa = PBXResourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				A1000006 /* Assets.xcassets in Resources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXResourcesBuildPhase section */

/* Begin PBXShellScriptBuildPhase section */
		A1000020 /* Build Kotlin Framework */ = {
			isa = PBXShellScriptBuildPhase;
			alwaysOutOfDate = 1;
			buildActionMask = 2147483647;
			files = (
			);
			inputPaths = (
			);
			name = "Build Kotlin Framework";
			outputPaths = (
			);
			runOnlyForDeploymentPostprocessing = 0;
			shellPath = /bin/sh;
			shellScript = "cd \"$SRCROOT/../..\"\n./gradlew :samples:shared:embedAndSignAppleFrameworkForXcode\n";
		};
/* End PBXShellScriptBuildPhase section */

/* Begin PBXSourcesBuildPhase section */
		A1000014 /* Sources */ = {
			isa = PBXSourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				A1000002 /* iOSApp.swift in Sources */,
				A1000004 /* ContentView.swift in Sources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXSourcesBuildPhase section */

/* Begin XCBuildConfiguration section */
		A1000030 /* Debug */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = dwarf;
				ENABLE_TESTABILITY = YES;
				GCC_OPTIMIZATION_LEVEL = 0;
				IPHONEOS_DEPLOYMENT_TARGET = 15.0;
				ONLY_ACTIVE_ARCH = YES;
				SDKROOT = iphoneos;
				SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG;
				SWIFT_OPTIMIZATION_LEVEL = "-Onone";
			};
			name = Debug;
		};
		A1000031 /* Release */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				COPY_PHASE_STRIP = NO;
				IPHONEOS_DEPLOYMENT_TARGET = 15.0;
				SDKROOT = iphoneos;
				SWIFT_OPTIMIZATION_LEVEL = "-O";
				VALIDATE_PRODUCT = YES;
			};
			name = Release;
		};
		A1000032 /* Debug */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;
				CODE_SIGN_STYLE = Automatic;
				CODE_SIGNING_ALLOWED = NO;
				CODE_SIGNING_REQUIRED = NO;
				FRAMEWORK_SEARCH_PATHS = (
					"$(inherited)",
					"$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
				);
				GENERATE_INFOPLIST_FILE = NO;
				INFOPLIST_FILE = iosApp/Info.plist;
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				PRODUCT_BUNDLE_IDENTIFIER = com.maxkach.swipingcardssample;
				PRODUCT_NAME = "$(TARGET_NAME)";
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
			};
			name = Debug;
		};
		A1000033 /* Release */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;
				CODE_SIGN_STYLE = Automatic;
				CODE_SIGNING_ALLOWED = NO;
				CODE_SIGNING_REQUIRED = NO;
				FRAMEWORK_SEARCH_PATHS = (
					"$(inherited)",
					"$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
				);
				GENERATE_INFOPLIST_FILE = NO;
				INFOPLIST_FILE = iosApp/Info.plist;
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				PRODUCT_BUNDLE_IDENTIFIER = com.maxkach.swipingcardssample;
				PRODUCT_NAME = "$(TARGET_NAME)";
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
			};
			name = Release;
		};
/* End XCBuildConfiguration section */

/* Begin XCConfigurationList section */
		A1000016 /* Build configuration list for PBXNativeTarget "iosApp" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				A1000032 /* Debug */,
				A1000033 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
		A1000018 /* Build configuration list for PBXProject "iosApp" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				A1000030 /* Debug */,
				A1000031 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
/* End XCConfigurationList section */
	};
	rootObject = A1000017 /* Project object */;
}
```

- [ ] **Step 5: Pre-build the framework for the simulator**

Building the framework once from Gradle catches Kotlin/Native errors before Xcode is involved. Run:

```bash
./gradlew :samples:shared:linkDebugFrameworkIosSimulatorArm64
```

Expected: BUILD SUCCESSFUL; a `Shared.framework` under `samples/shared/build/bin/iosSimulatorArm64/debugFramework/`.

- [ ] **Step 6: Build the iOS app with xcodebuild**

Pick an available simulator (from `xcrun simctl list devices available`, e.g. an iPhone on the iOS 18.6 runtime). Run:

```bash
xcodebuild \
  -project samples/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  -derivedDataPath samples/iosApp/build \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Expected: `** BUILD SUCCEEDED **`. The Run Script phase invokes `:samples:shared:embedAndSignAppleFrameworkForXcode`, which copies `Shared.framework` into the app bundle.

Note: if `xcodebuild` reports it cannot find scheme `iosApp`, generate a shared scheme by opening the project once, or create `samples/iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`. If needed, add that file with a standard scheme referencing target `A1000013` / product `iosApp.app`.

- [ ] **Step 7: Launch on the simulator and verify the gallery**

Run:

```bash
xcrun simctl boot "iPhone 16" 2>/dev/null || true
open -a Simulator
APP_PATH=$(find samples/iosApp/build/Build/Products -name "iosApp.app" -type d | head -1)
xcrun simctl install booted "$APP_PATH"
xcrun simctl launch booted com.maxkach.swipingcardssample
```

Expected: the app launches and shows the gallery list (Dating, Bank cards, D&D, Streaming). Manually (or via a screenshot with `xcrun simctl io booted screenshot /tmp/ios.png`) confirm a demo opens and the front card can be swiped. Attach the screenshot as evidence.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "Add the iOS launcher and shared framework integration

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Publishing metadata, documentation, and full verification

**Files:**
- Modify: `swipingcards/build.gradle.kts` (already has the publishing block from Task 1 — verify it publishes locally)
- Create: `swipingcards/README.md`
- Modify: `README.md` (root)

**Interfaces:** none (final verification task).

- [ ] **Step 1: Publish the library to the local Maven repo**

Run:

```bash
./gradlew :swipingcards:publishToMavenLocal
```

Expected: BUILD SUCCESSFUL. Confirm artifacts exist:

```bash
ls ~/.m2/repository/com/maxkach/swipingcards*/0.1.0/
```

Expected: at least the root `swipingcards` (KMP metadata) and `swipingcards-android` publications, each with a `.module`, `.pom`, `.jar`, and a `-sources.jar`, plus iOS target publications (`swipingcards-iosx64`, `swipingcards-iosarm64`, `swipingcards-iossimulatorarm64`).

- [ ] **Step 2: Write the library consumer README**

Create `swipingcards/README.md`:

```markdown
# swipingcards

A Compose Multiplatform swipeable card deck (Android + iOS).

## Coordinates

- group: `com.maxkach`
- artifact: `swipingcards`
- version: `0.1.0`

## Local consumption

Publish to your local Maven repository:

    ./gradlew :swipingcards:publishToMavenLocal

In a consuming Compose Multiplatform project, add `mavenLocal()` and the dependency:

    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    kotlin {
        sourceSets {
            commonMain.dependencies {
                implementation("com.maxkach:swipingcards:0.1.0")
            }
        }
    }

## Usage

`SwipingCards` is a Composable in package `com.maxkach.swipingcards`. Supply a list of
items with stable keys and Compose content per card; the deck rotates infinitely and
reports committed swipes via a callback. See `samples/shared` for worked examples.

> Not published to Maven Central. No signing or release automation is configured.
```

- [ ] **Step 3: Update the root README module map**

Read the current root `README.md`, then update its module/structure section to describe the new layout: `swipingcards/` (KMP library) and `samples/{shared,androidApp,iosApp}`. Replace any reference to the old `sample` module and the old Android-only framing with the Compose Multiplatform description. Keep the existing tone and headings; only change the parts that are now inaccurate.

- [ ] **Step 4: Run the full cross-platform verification suite**

Run each and confirm BUILD SUCCESSFUL:

```bash
./gradlew :swipingcards:testDebugUnitTest :swipingcards:iosSimulatorArm64Test
./gradlew :samples:shared:iosSimulatorArm64Test
./gradlew :samples:androidApp:assembleDebug
./gradlew :samples:shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :swipingcards:publishToMavenLocal
```

- [ ] **Step 5: Assert no platform leaks into common source sets**

Run:

```bash
grep -rn "import android\.\|androidx\.activity\|androidx\.annotation\|R\.drawable" \
  swipingcards/src/commonMain samples/shared/src/commonMain
```

Expected: NO output. Any hit is a leak that must be moved behind `expect`/`actual` or into a platform source set.

- [ ] **Step 6: Rebuild and relaunch the iOS app for final confirmation**

Repeat Task 4 Steps 6–7 (xcodebuild + simctl launch). Expected: gallery renders and a swipe works. Capture a final screenshot.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Add publishing metadata, consumer docs, and cross-platform verification

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage** (against `docs/specs/epic-3-kmp-library.md` and the design doc):

- Reusable KMP library module → Task 1.
- Common Compose implementation / core in `commonMain` → Tasks 1–2.
- Android + iOS device + simulator targets → Task 1 (`androidTarget`, `iosX64`, `iosArm64`, `iosSimulatorArm64`).
- Shared example UI in `commonMain` → Task 2.
- Thin Android and iOS launchers → Tasks 3 and 4.
- Library must not depend on samples → build files depend one-way (`samples/*` → `:swipingcards`), asserted by inspection.
- Move deck composable/state/reconciliation/models/gestures/animation/public API/tests into common → Tasks 1–2.
- Card content stays caller-supplied Compose content → unchanged public API (Task 1).
- No Android `Context`/resources/platform views in the common API → dynamic color removed (Task 2 Step 9), images via Compose resources, back via `expect`/`actual`; asserted in Task 5 Step 5.
- Publish-ready metadata, KMP publications, source artifacts, local Gradle consumption docs → Tasks 1 and 5.
- Publishing excludes signing/credentials/Central → build file has no signing; noted in README.
- Epic 1 behavior unchanged → library sources moved byte-for-byte; existing tests kept (Task 1).
- Testing: common unit tests, Android build/smoke, iOS framework build, simulator launch, shared sample compiles both platforms, no Android-only leak → Task 5 Step 4–6 and Task 4.

**Placeholder scan:** No "TBD"/"TODO"/"handle edge cases"/"add tests" left; every code and command step is concrete. The only conditional steps (scheme generation in Task 4 Step 6; `generateComposeResClass` in Task 2 Step 13) give the exact fallback command.

**Type consistency:** `Artwork.Image(resource: DrawableResource, ...)` defined in Task 2 Step 7 matches the `Res.drawable.*` call-site changes in Step 8 (both `DrawableResource`). `PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)` signature is identical across the `expect` (Step 5), both `actual`s (Step 5), and the call site (Step 6). `App()` (Task 2 Step 10) is consumed by `MainActivity` (Task 3 Step 2) and `MainViewController()` (Task 2 Step 11); `MainViewController()` return type `UIViewController` matches the Swift `MainViewControllerKt.MainViewController()` call (Task 4 Step 1). Publishing coordinates `com.maxkach` / `swipingcards` / `0.1.0` are consistent across the build file, README, and verification `ls`.
