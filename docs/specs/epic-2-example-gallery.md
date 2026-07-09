# Epic 2 — Themed example gallery

## Status

TODO. Recommended after Epics 1 and 3.

## Goal

Create a shared example gallery demonstrating that the same deck component supports
different domains, content structures, dimensions, aspect ratios, overlays, and swipe
callbacks.

## Dependencies

- **Epic 1** — generic deck, sizing, infinite rotation, callbacks.
- **Epic 3** — shared Compose example application for Android and iOS.

## Scope

Create one shared gallery with three destinations:

1. Dating / cartoon characters
2. Bank cards
3. D&D / fantasy cards

Every example must use the public library API. Examples must not fork or copy the deck
implementation.

## Shared gallery

- Displays the available examples.
- Briefly explains what each example demonstrates.
- Works from the same `commonMain` UI on Android and iOS.
- Allows returning to the gallery without recreating platform-specific navigation.

## Dating example

- Tall portrait cards.
- Cartoon-character artwork, supplied or replaced later by the project owner.
- Character name and short profile information.
- Left/right swipe actions such as pass/like.
- Optional visual action overlays if supported by the library API.
- Small event history such as "Liked Mira" or "Passed Rowan".

## Bank-card example

- Wide landscape cards.
- Demonstrates that the deck is not tied to portrait layouts.
- Uses fictional names, numbers, and financial data only.
- Provides domain-appropriate example actions.
- Small event history such as "Selected card ending 4242".
- Must not resemble a production payment or security workflow.

## D&D example

- Fantasy character, creature, spell, or encounter cards.
- Uses a different visual hierarchy from the other two demos.
- Includes relevant statistics or attributes.
- Provides example actions such as select, recruit, reject, or encounter.
- Small event history such as "Selected the rogue".

## Event history

- Visually secondary to the deck.
- Driven by public swipe callbacks.
- Shows the most recent actions.
- Uses concise, domain-specific descriptions.
- Demonstrates card identity and swipe direction where relevant.
- Does not require persistence across application restarts.

## Assets

- Bundled locally so examples work offline.
- Generated character artwork can be added later without changing core APIs.
- Temporary placeholders must be clearly replaceable.
- Assets should include appropriate descriptions or accessibility labels.
- No copyrighted third-party game or dating-app branding should be copied.

## Dimensions

The examples intentionally use different deck-level dimensions:

- Dating: tall portrait
- Bank: wide landscape
- D&D: a third distinct format

This is a functional demonstration of Epic 1's responsive sizing, not merely styling.
Individual cards inside a particular deck use consistent dimensions.

## Non-goals

- Real dating functionality
- Real payment functionality
- Full D&D rules
- Networking or backend services
- Account creation
- Persistent event history
- Separate Android and iOS UI implementations

## Acceptance criteria

- The gallery lists all three examples.
- Each example can be opened on Android and iOS.
- All examples use the same library component.
- Each deck rotates indefinitely.
- Each example has visibly different dimensions and content structure.
- Swiping adds a correct entry to the small event history.
- No example relies on hardcoded dimensions inside the library.
- Example assets work offline.
- Navigating between demos does not corrupt deck state.

## Suggested review-sized slices

1. Shared gallery shell and navigation.
2. Dating example and event history pattern.
3. Bank-card example with landscape sizing.
4. D&D example and final cross-platform polish.
