# swipingcards

A Compose Multiplatform swipeable card deck (Android + iOS).

## Coordinates

- group: `com.maxkach`
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
                implementation("com.maxkach:swipingcards:0.1.0")
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

## Publishing (maintainer)

The module uses the `com.vanniktech.maven.publish` plugin targeting the Sonatype
Central Portal, with GPG signing. Credentials/keys are read from
`~/.gradle/gradle.properties` (`mavenCentralUsername`, `mavenCentralPassword`,
`signingInMemoryKey`, `signingInMemoryKeyPassword`).

- Dry run / local: `./gradlew :swipingcards:publishToMavenLocal`
- Upload + auto-release to Central: `./gradlew :swipingcards:publishAndReleaseToMavenCentral`
- Upload only (release manually in the portal): `./gradlew :swipingcards:publishToMavenCentral`
