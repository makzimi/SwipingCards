# Epic 3 — Compose Multiplatform library

## Status

TODO. Recommended after Epic 1 and before Epic 2.

## Goal

Extract SwipingCards into a reusable Compose Multiplatform / Kotlin Multiplatform
library and provide Android and iOS applications hosting the same shared example UI.

## Scope

- Reusable KMP library module.
- Common Compose implementation.
- Android target.
- iOS device and simulator targets appropriate to the project toolchain.
- Shared example UI in `commonMain`.
- Thin Android and iOS launchers.
- Publish-ready library metadata.
- Local project consumption by both examples.

## Suggested structure

Exact names should follow existing repository conventions:

```
swiping-cards/
  commonMain/
  commonTest/
  androidMain/
  iosMain/

samples/
  shared/
  androidApp/
  iosApp/
```

The library must not depend on the sample modules.

## Common code

Move the following into common code where possible:

- Deck composable
- Deck state and circular ordering
- Reconciliation logic
- Swipe result models
- Stable-key validation
- Gesture handling
- Animation logic
- Public API
- Unit tests

Platform-specific source sets should contain only code that genuinely requires platform
APIs.

## Android example

- Thin Android application entry point.
- Hosts the shared Compose gallery.
- Consumes the local KMP library artifact/module.
- Does not duplicate example screens.

## iOS example

- Thin Swift/SwiftUI or UIKit entry point as appropriate.
- Hosts the same shared Compose gallery.
- Consumes the generated shared framework.
- Does not reimplement the gallery in Swift.
- Works on the supported iOS simulator and device architectures.

## Shared example UI

- Lives in common Compose code.
- Contains shared navigation and example screens.
- Produces equivalent behavior on Android and iOS.
- Platform launchers may provide only lifecycle, window, or host integration.

## Publishing readiness

Include:

- Stable group and artifact identifiers
- Version configuration
- KMP publications for configured targets
- Source artifacts where supported
- Required project metadata
- Consumer documentation for local Gradle use

Do not include:

- Maven Central account setup
- Signing keys
- Credentials
- Production release automation
- Namespace ownership work
- Public release as part of this epic

## API constraints

- Public library APIs should use common Kotlin and Compose types.
- Do not expose Android `Context`, Android resources, or platform views through the
  common API.
- Platform-specific behavior must be hidden behind internal abstractions when necessary.
- Card content remains caller-supplied Compose content.
- Epic 1 behavior must remain unchanged after migration.

## Targets

Initially supported:

- Android
- iOS device
- iOS simulator

Initially out of scope:

- Desktop
- Web/Wasm
- macOS-native application
- Swift-native non-Compose card rendering

## Testing

- Common unit tests for ordering and reconciliation.
- Common tests for public state behavior.
- Android build and smoke verification.
- iOS framework build.
- iOS simulator build or launch verification where the environment supports it.
- Shared sample compilation for both platforms.
- Confirm no Android-only dependency leaks into common source sets.

## Migration approach

Prefer incremental extraction:

1. Establish KMP module structure without changing behavior.
2. Move pure ordering and reconciliation logic into `commonMain`.
3. Move the Compose component into common code.
4. Keep an adapter or compatibility path for the existing application.
5. Add the shared sample.
6. Add thin Android and iOS launchers.
7. Add publish-ready metadata and documentation.

Keep the repository buildable at each slice.

## Non-goals

- Maven Central publication
- Release signing
- CI release deployment
- Desktop or web support
- Separate native UI implementations
- Redesigning the card component
- Implementing the themed gallery before the shared sample structure exists

## Acceptance criteria

- The card library compiles as a KMP module.
- Core implementation resides in `commonMain`.
- Android consumes and displays the local library.
- iOS consumes and displays the local library.
- Both applications host the same shared Compose UI.
- Epic 1's infinite rotation and reconciliation behave consistently on both platforms.
- The library can be consumed independently of the sample applications.
- Publishing metadata can produce local publications without production credentials.
- Relevant common, Android, and iOS builds pass.

## Suggested review-sized slices

1. KMP build structure and common state engine.
2. Common Compose library extraction with compatibility adapter.
3. Shared sample plus Android launcher.
4. iOS launcher and framework integration.
5. Publication metadata, documentation, and cross-platform verification.
