# SwipingCards roadmap

This directory holds the specs for the three epics that take SwipingCards from a
hardcoded four-card Android demo to a reusable Compose Multiplatform library with a
themed example gallery.

## Status and recommended order

| Epic | Status | Recommended order |
| --- | --- | --- |
| [Epic 1 — Arbitrary-size infinite deck](epic-1-infinite-deck.md) | Ready for implementation | 1 |
| [Epic 3 — Compose Multiplatform library](epic-3-kmp-library.md) | TODO | 2 |
| [Epic 2 — Themed example gallery](epic-2-example-gallery.md) | TODO | 3 |

Epic 3 precedes Epic 2 so the examples are implemented once in shared Compose code.

## Summary

- **Epic 1 — Arbitrary-size infinite deck.** Replace the fixed four-card assumption
  with an infinite circular deck of any size, reconciled against externally supplied
  state by stable key, with no hardcoded card dimensions.
- **Epic 3 — Compose Multiplatform library.** Extract the deck into a reusable
  KMP/Compose Multiplatform library with Android and iOS launchers hosting one shared
  example UI, plus publish-ready metadata.
- **Epic 2 — Themed example gallery.** Build a shared gallery with three visually
  distinct demos (dating, bank cards, D&D) that exercise the responsive sizing,
  infinite rotation, and swipe callbacks from Epic 1.
