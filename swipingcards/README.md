# swipingcards

A Compose Multiplatform swipeable card deck (Android + iOS).

## Coordinates

- group: `io.github.makzimi`
- artifact: `swipingcards`
- version: `0.1.0`

## Consuming from Maven Central

Once published, add it to a Compose Multiplatform project:

    repositories {
        mavenCentral()
        google()
    }

    kotlin {
        sourceSets {
            commonMain.dependencies {
                implementation("io.github.makzimi:swipingcards:0.1.0")
            }
        }
    }

## Consuming locally (before/without a Central release)

    ./gradlew :swipingcards:publishToMavenLocal

then add `mavenLocal()` to the consumer's `repositories { }` and the same dependency
coordinate.

## Usage

`SwipingCards` is a Composable in package `com.maxkach.swipingcards`. Supply a list of
items with stable keys and Compose content per card; the deck rotates infinitely and
reports committed swipes via a callback. See `samples/shared` for worked examples.

## Caller-owned animation

Both overloads accept `cardLayer: GraphicsLayerScope.(CardInfo) -> Unit` and
`isSwipeEnabled: Boolean = true`. The drawing callback runs after the library applies
its per-card scale, rotation, alpha and pixel translations. Read Compose animation
state here; timing, easing and replay remain in the caller.

`CardInfo.key` is the supplied stable key. `position` is the current internal visible
position (zero is the front), which can differ from the caller's list during a swipe.
Exiting cards also receive the callback, with `position == null` and `isExiting == true`.

For example, reveal the front card before the remaining cards:

```kotlin
val front = remember { Animatable(0f) }
val back = remember { Animatable(0f) }
LaunchedEffect(Unit) {
    front.animateTo(1f, tween(500))
    back.animateTo(1f, tween(600))
}

SwipingCards(
    cards = cards,
    key = { it.id },
    modifier = Modifier.size(280.dp, 378.dp),
    isSwipeEnabled = back.value == 1f,
    cardLayer = { info ->
        if (!info.isExiting) {
            val progress = if (info.position == 0) front.value else back.value
            alpha *= progress
            scaleX *= 0.6f + 0.4f * progress
            scaleY *= 0.6f + 0.4f * progress
        }
    },
) { card ->
    CardContent(card)
}
```

The callback changes drawing, not layout, z-order, semantics or gesture coordinate
conversion. Disable swipes while overriding the front card's scale or rotation.
Disabling during a drag cancels it and settles the card without committing a swipe;
clicks inside card content remain the caller's responsibility.

Do not mutate state or launch effects from `cardLayer`. Keep `cardGeometry` constant
and animate the rendered layer instead. Returning without changes preserves the
library's transforms. For restored screens or reduced motion, initialize animation
state at its end and skip the entrance effect. Ordinary swipes should not restart it.

## Publishing (maintainer)

The module uses the `com.vanniktech.maven.publish` plugin targeting the Sonatype
Central Portal, with GPG signing. Credentials/keys are read from
`~/.gradle/gradle.properties` (`mavenCentralUsername`, `mavenCentralPassword`,
`signingInMemoryKey`, `signingInMemoryKeyPassword`).

- Dry run / local: `./gradlew :swipingcards:publishToMavenLocal`
- Upload + auto-release to Central: `./gradlew :swipingcards:publishAndReleaseToMavenCentral`
- Upload only (release manually in the portal): `./gradlew :swipingcards:publishToMavenCentral`
