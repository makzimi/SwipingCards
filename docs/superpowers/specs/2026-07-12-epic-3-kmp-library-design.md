# Epic 3 — Compose Multiplatform library: design

Date: 2026-07-12
Epic spec: [docs/specs/epic-3-kmp-library.md](../../specs/epic-3-kmp-library.md)

## Goal

Extract SwipingCards into a reusable Kotlin Multiplatform / Compose Multiplatform
library, and host the existing example gallery (built in Epics 1 and 2) as one shared
Compose UI driven by thin Android and iOS launcher apps.

Epic 1's infinite-rotation and reconciliation behavior, and Epic 2's four gallery demos,
must be preserved unchanged. This epic is build/infrastructure work plus mechanical file
moves — not a redesign of the card component or the gallery.

## Context: current state

Epics 1 and 2 are already implemented on Android:

- `swipingcards/` — `com.android.library` module. Core files (`DeckState.kt`,
  `DeckReconciler.kt`, `SwipeResult.kt`, `SwipingCards.kt`) already import only
  `androidx.compose.*`, `kotlinx.coroutines`, and `kotlin.math`. No `Context`, no Android
  resources, no platform views. Tests use JUnit4.
- `sample/` — `com.android.application` module holding the gallery: `dating/`, `bank/`,
  `dnd/`, `streaming/`, `common/`, `gallery/`, `ui/theme/`. Navigation is a hoisted-state
  navigator (`GalleryApp.kt`), **not** `NavHost`. The `navigation-compose` dependency is
  declared but unused by the gallery flow.

The only genuinely Android-specific pieces in the gallery are:

1. Image art — `@DrawableRes Int` + `androidx.compose.ui.res.painterResource`, backed by
   `res/drawable/*.webp`.
2. `androidx.activity.compose.BackHandler` (hardware back).
3. `MainActivity` entry point (`enableEdgeToEdge`, `setContent`).

## Approved decisions

- **Module layout:** the epic's `samples/` grouping (library + `samples/{shared,
  androidApp, iosApp}`).
- **iOS verification:** build the shared framework **and** build + launch the iOS app on
  an iOS Simulator, confirming the gallery renders and a swipe works.
- **Delivery:** a single feature branch (`epic-3-kmp-library`), one PR. The repo stays
  buildable at every step (the Android build survives throughout).
- **Toolchain:** keep Kotlin 2.0.21; add Compose Multiplatform 1.7.3. No Kotlin bump.
- **Publishing coordinates:** `group = com.maxkach`, artifact `swipingcards`,
  `version = 0.1.0`.

Local toolchain confirmed present: Xcode 26.5, iOS Simulator runtimes (18.6 and 26.x),
JDK 21, Gradle 8.13.

## Module topology

```
swipingcards/                          KMP library — com.maxkach.swipingcards
  src/commonMain/kotlin/               DeckState, DeckReconciler, SwipeResult, SwipingCards
  src/commonTest/kotlin/               DeckReconcilerTest, SwipeDirectionTest (kotlin.test)
  src/androidMain/kotlin/              (expected empty; only for unavoidable platform APIs)
  src/iosMain/kotlin/                  (expected empty)

samples/
  shared/                              KMP — the whole gallery
    src/commonMain/kotlin/             gallery/, dating/, bank/, dnd/, streaming/,
                                       common/, ui/theme/, App()
    src/commonMain/composeResources/
      drawable/                        all .webp art
    src/androidMain/kotlin/            actual PlatformBackHandler
    src/iosMain/kotlin/                actual PlatformBackHandler (no-op) + MainViewController()
  androidApp/                          thin: MainActivity -> setContent { App() }
  iosApp/                              Xcode project -> links shared framework -> MainViewController()
```

The library never depends on any `samples` module. `samples/androidApp` and
`samples/iosApp` both render the same `App()` from `samples/shared/commonMain`; neither
duplicates gallery code.

## Toolchain and versions

- Kotlin **2.0.21** (unchanged), AGP **8.13.2** (unchanged), Gradle **8.13** (unchanged).
- Add Compose Multiplatform **1.7.3** (`org.jetbrains.compose` plugin +
  `org.jetbrains.kotlin.multiplatform`). Known-good pairing with Kotlin 2.0.21.
- Compose artifacts switch from the AndroidX Compose BOM to CMP aliases: `compose.runtime`,
  `compose.foundation`, `compose.material3`, `compose.ui`, `compose.components.resources`,
  `compose.materialIconsExtended`, `compose.components.uiToolingPreview`.
- Drop `navigation-compose` (unused by the gallery).
- `libs.versions.toml` gains the `compose` plugin version and `kotlin-multiplatform`
  plugin; the Android-only Compose BOM entries are removed from KMP modules (androidApp
  still uses the AGP/Compose plugins via the CMP setup).

## Library conversion (`swipingcards`)

- Plugins: `org.jetbrains.kotlin.multiplatform` + `org.jetbrains.compose` +
  `com.android.library` (Android target still needs the AGP plugin) +
  `org.jetbrains.kotlin.plugin.compose`.
- Targets: `androidTarget()`, `iosX64()`, `iosArm64()`, `iosSimulatorArm64()`.
- Android `namespace = "com.maxkach.swipingcards"`, `compileSdk 36`, `minSdk 33`.
- Source: move `src/main/java/**` -> `src/commonMain/kotlin/**` unchanged. All current
  imports are multiplatform-safe (`LocalHapticFeedback`, `VelocityTracker`, `LocalDensity`,
  `graphicsLayer`, `detectDragGestures` all exist in common Compose).
- Tests: move `src/test/java/**` -> `src/commonTest/kotlin/**`; swap `org.junit.Test` /
  `org.junit.Assert.assertEquals` for `kotlin.test.Test` / `kotlin.test.assertEquals`
  (identical call surface). Add `kotlin.test` to `commonTest` deps.
- Public API and package (`com.maxkach.swipingcards`) unchanged -> Epic 1 behavior preserved.

## Shared sample (`samples/shared`)

Gallery code moves into `commonMain` essentially verbatim. Three edits:

1. **Images.** `.webp` files -> `src/commonMain/composeResources/drawable/`. The generated
   accessor is `swipingcardssample.shared.generated.resources.Res` with
   `Res.drawable.<name>` of type `DrawableResource`. `Artwork.Image(@DrawableRes Int)`
   becomes `Artwork.Image(DrawableResource)`; `androidx.compose.ui.res.painterResource`
   becomes `org.jetbrains.compose.resources.painterResource`. Call sites change
   `R.drawable.dnd_rogue` -> `Res.drawable.dnd_rogue`.
2. **Back handling.** Replace `androidx.activity.compose.BackHandler` with an
   `internal expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)`. Android
   `actual` delegates to `BackHandler`; iOS `actual` is a no-op (iOS relies on the in-UI
   back button, which already exists in each example screen's top bar). This is the
   internal abstraction the epic calls for.
3. **Entry point.** Add `@Composable fun App()` = `SwipingCardExampleTheme { GalleryApp() }`.
   `iosMain` adds `fun MainViewController(): UIViewController = ComposeUIViewController { App() }`.

The theme (`ui/theme/`, Material3) moves to `commonMain` — Material3 is multiplatform.
Hardcoded strings stay in Kotlin (they already are). Android string/launcher resources move
to `androidApp`.

## Android launcher (`samples/androidApp`)

- `com.android.application`, `applicationId = "com.maxkach.swipingcardssample"`,
  `compileSdk 36`, `minSdk 33`, `targetSdk 36`.
- Depends on `project(":samples:shared")`.
- Contains only `MainActivity` (`enableEdgeToEdge()` + `setContent { App() }`), the Android
  manifest, and launcher icons/strings moved from today's `sample`. No screen code.

## iOS launcher (`samples/iosApp`)

- `samples/shared` declares a static framework `baseName = "Shared"` for `iosX64`,
  `iosArm64`, `iosSimulatorArm64` (`isStatic = true`).
- Committed `iosApp.xcodeproj` with a SwiftUI `@main App`. The root view wraps
  `MainViewController()` via a `UIViewControllerRepresentable` (`ComposeView`), ignoring
  safe-area insets so Compose draws edge to edge.
- A "Run Script" build phase runs
  `./gradlew :samples:shared:embedAndSignAppleFrameworkForXcode` and adds the framework
  search path, so Xcode always links a freshly built framework.
- No gallery/card logic in Swift.

## Publishing metadata

- `com.vanniktech.maven.publish` plugin on `swipingcards`.
- `group = "com.maxkach"`, artifact base `swipingcards`, `version = "0.1.0"`.
- KMP publications for all configured targets; Android publishes the `release` variant
  (`androidTarget { publishLibraryVariants("release") }`). `withSourcesJar()` for source
  artifacts.
- POM metadata: name, description, license placeholder, project URL.
- Publishes to the Sonatype Central Portal (`publishToMavenCentral()`), with GPG signing of
  all publications via `signingInMemoryKey` / `signingInMemoryKeyPassword`; signing is
  conditional on those properties being present so `publishToMavenLocal` still works
  keyless for contributors without a signing key.
- Verified with `publishToMavenLocal`.
- Consumer documentation (local Gradle `mavenLocal()` + dependency coordinate) added to the
  `swipingcards` README.

> Note: the original epic explicitly excluded Maven Central publication and signing (see
> Non-goals below); that exclusion was intentionally expanded by the user during
> implementation to include signed Central Portal publishing.

## Testing and verification

1. `:swipingcards:allTests` (or `:swipingcards:testDebugUnitTest`) — ordering and
   reconciliation, now in `commonTest`.
2. `:samples:androidApp:assembleDebug` — Android build + smoke.
3. iOS framework link for `swipingcards` and `samples:shared` (`iosSimulatorArm64`).
4. Build + launch `iosApp` on an iOS Simulator; confirm the gallery lists all four demos,
   a demo opens, and a swipe commits.
5. `:swipingcards:publishToMavenLocal` produces artifacts under `~/.m2`.
6. Leak check: no `android.*`, `androidx.activity.*`, or `R.drawable` references in any
   `commonMain` source set.

## Migration order (single branch, buildable at each step)

Follows the epic's suggested slices, but landed on one branch:

1. Establish the KMP module structure and convert `swipingcards` to a KMP library; move
   core + tests into `commonMain`/`commonTest`. Keep Android building.
2. Create `samples/shared` and move the gallery into `commonMain`; migrate images to
   compose resources and back handling to `expect/actual`; add `App()`.
3. Add `samples/androidApp` thin launcher consuming `samples/shared`; retire old `sample`.
4. Add iOS framework config + `samples/iosApp` Xcode project; build and run on simulator.
5. Add publishing metadata and consumer docs; run full cross-platform verification.

## Risks

- **Compose resources wiring** (generated `Res` accessor, source-set config) is the
  fiddliest build step — isolated to `samples/shared`.
- **Xcode <-> Gradle framework path** in the Run Script phase is the classic CMP friction
  point; proven by the simulator run.
- Everything else is mechanical file moves. The Android build survives each step, so the
  repo never goes dark.

## Non-goals (from the epic)

Maven Central publication, signing, CI release deployment, desktop/web/Wasm support,
native non-Compose iOS card rendering, redesigning the card component, and any new gallery
demos beyond the existing four.
