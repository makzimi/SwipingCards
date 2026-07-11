# Epic 2 — Themed example gallery — Design

Date: 2026-07-11
Source spec: [`docs/specs/epic-2-example-gallery.md`](../../specs/epic-2-example-gallery.md)
Status: Approved, ready for implementation planning.

## Summary

Build a gallery in the existing Android `:sample` module with three visually distinct
demos — **Dating**, **Bank cards**, **D&D** — that each drive the public `SwipingCards`
component from Epic 1 with a **different aspect ratio**, content structure, action
vocabulary, and a small per-demo event history fed by the swipe callback. A simple
hoisted-state navigator lets the user open a demo and return to the gallery.

## Scope decisions (from brainstorming)

1. **Android-only, now.** Epic 3 (KMP extraction to `commonMain` + iOS launcher) is
   **out of scope**. Everything lives in the Android `:sample` module using ordinary
   Android Compose. We refactor for multiplatform in Epic 3; no portability
   contortions are made here. Epic 2 acceptance criteria that require iOS / shared
   `commonMain` are explicitly **deferred to Epic 3**.
2. **Artwork:** Dating and D&D use real generated bitmap artwork, supplied later, with
   clearly-replaceable placeholders shipped now. Bank cards are fully procedural
   (code-drawn) — no bitmaps, no risk of resembling a real payment product.
3. **Asset format:** `.webp` (Android decodes it natively; smaller than PNG), placed
   in `sample/src/main/res/drawable/`.
4. **No drag-overlay API.** The library's `cardContent` receives only the card `T`,
   with no drag-progress hook, so drag-reactive "LIKE/NOPE" overlays are not
   supported. The spec marks overlays optional ("if supported by the library API");
   we skip them and instead show **static action-hint chips** on each card plus the
   event history. (A drag-progress hook is a candidate future library enhancement,
   out of scope here.)
5. **D&D roster** uses the most-played D&D archetype combos (per the official player
   census — Halfling Rogue, Tiefling Warlock, Half-Orc Barbarian, Elf Ranger, Human
   Fighter) with **original names**, not WotC-owned named characters, honoring the
   spec's "no copyrighted third-party game branding" rule.

## Public library API consumed (unchanged)

- `@Composable fun <T> SwipingCards(cards, key, modifier, maxVisibleCards, maxRotationY, swipeThresholdFraction, onSwipe, cardContent)`
- `data class SwipeResult<T>(card, key, direction, resultingOrder)`
- `enum class SwipeDirection { Left, Right, Up, Down }`

Deck dimensions come entirely from the caller's `modifier` (e.g.
`Modifier.fillMaxWidth(0.8f).aspectRatio(2f/3f)`). No example touches library
internals or forks the deck.

## Architecture

### Navigation — hoisted state, no navigation library

```kotlin
sealed interface Destination { Gallery; Dating; Bank; Dnd }

@Composable fun GalleryApp() {
    var current by remember { mutableStateOf<Destination>(Destination.Gallery) }
    // BackHandler(enabled = current != Gallery) { current = Gallery }
    when (current) {
        Gallery -> GalleryScreen(onOpen = { current = it })
        Dating  -> DatingExampleScreen(onBack = { current = Destination.Gallery })
        Bank    -> BankExampleScreen(onBack = { current = Destination.Gallery })
        Dnd     -> DndExampleScreen(onBack = { current = Destination.Gallery })
    }
}
```

Each detail screen owns its own deck + event-history state inside its own subtree.
Leaving a demo disposes that subtree; returning recreates it fresh. Because each
`SwipingCards` instance has its own internal `DeckState`, **navigating between demos
cannot corrupt deck state**. `BackHandler` (Android) sends the user back to the
gallery.

### File layout (all under `sample/src/main/java/com/maxkach/swipingcardssample/`)

```
gallery/
  Destination.kt          sealed interface + per-demo metadata (title, blurb)
  GalleryApp.kt           navigator + BackHandler
  GalleryScreen.kt        list of the 3 demos with "what it demonstrates"
common/
  EventHistory.kt         EventHistoryState holder (cap ~5) + EventHistoryView (secondary UI)
  Artwork.kt              sealed Artwork + ArtworkImage composable (placeholder | webp image)
  ActionHints.kt          small left/right action-hint row shown on each card
dating/
  DatingProfile.kt        data class + sample roster + datingEventLabel()
  DatingCard.kt           card view (full-bleed art + scrim + name/chips)
  DatingExampleScreen.kt  deck (2:3) + event history
bank/
  BankCard.kt             data class + roster + bankEventLabel() + procedural card view
  BankExampleScreen.kt    deck (1.586:1) + event history
dnd/
  DndCard.kt              data class + roster + dndEventLabel() + card view (framed art + stats)
  DndExampleScreen.kt     deck (4:5) + event history
```

The existing `SwipingCardsSampleScreen.kt` and `SampleIdeaCards.kt` are removed; the
gallery supersedes them. `MainActivity` launches `GalleryApp()`.

### Direction semantics (shared, total over all 4 directions)

Swipes are 4-way. Each demo collapses them to a positive/negative action so its label
function is deterministic over every `SwipeDirection`:

- **Positive action** ← `Right` or `Up`
- **Negative action** ← `Left` or `Down`

Cards advertise only the horizontal pair in their action hints, but vertical swipes
still resolve cleanly (e.g. an up-swipe on a dating card = Like).

## The three demos — visibly different dimensions

| Demo | Card aspect ratio | Deck `modifier` | Layout / hierarchy |
| --- | --- | --- | --- |
| **Dating** | **2:3** tall portrait (0.667) | `fillMaxWidth(0.8f).aspectRatio(2f/3f)` | Full-bleed character art, bottom gradient scrim, name · age, tagline, interest chips |
| **Bank** | **1.586:1** wide landscape (ISO/IEC 7810 ID‑1 credit-card ratio) | `fillMaxWidth(0.92f).aspectRatio(1.586f)` | Procedural gradient, "chip", masked number, holder, DEMO marker |
| **D&D** | **4:5** near-square (0.8) | `fillMaxWidth(0.82f).aspectRatio(4f/5f)` | Framed **square** art window on top + parchment stat block below |

Three genuinely different ratios — **0.667 / 0.8 / 1.586** — exercising Epic 1's
responsive sizing, not just styling.

### Dating

- Roster (5): **Mira, Rowan, Kai, Sol, Nova**.
- Fields: name, age, one-line tagline, 2–3 interest chips.
- Actions: **Pass** (negative) / **Like** (positive), per the shared direction rule.
- `datingEventLabel(result)` → `"Liked Mira"` / `"Passed Rowan"`.

### Bank

- Roster (5), all fictional: `Aurora Everyday •••• 4242`, `Zephyr Travel •••• 8817`,
  `Nimbus Cashback •••• 3390`, `Onyx Reserve •••• 1265`, `Coral Student •••• 7734`.
- Fields: product name, masked number, fictional holder `A. EXAMPLE`, expiry,
  one-line perk. A clear **"DEMO — NOT A REAL CARD"** marker. No security/payment flow.
- Actions: **Skip** (negative) / **Select** (positive), per the shared direction rule.
- `bankEventLabel(result)` → `"Selected card ending 4242"` / `"Skipped Zephyr Travel"`.

### D&D

- Roster (5), popular archetypes with original names:
  1. **Lila Underbough** — Halfling Rogue (the "rogue")
  2. **Kaelen Ashborn** — Tiefling Warlock
  3. **Gruk the Unbroken** — Half-Orc Barbarian
  4. **Sylvara Nightbreeze** — Elf Ranger
  5. **Ser Aldric Vane** — Human Fighter
- Fields: name, class + race line, **HP · AC · CR** + two signature attributes (e.g.
  STR/DEX), a short flavor line.
- Actions: **Reject** (negative) / **Recruit** (positive), per the shared direction rule.
- `dndEventLabel(result)` → `"Recruited Lila the rogue"` / `"Rejected Gruk"`.

## Event history

- `EventHistoryState`: an observable list capped at the most recent ~5 entries, with
  `record(label: String)`. Not persisted (spec: persistence not required).
- `EventHistoryView`: rendered **secondary** to the deck (small text, muted), showing
  most-recent-first. Empty state: "Swipe a card to begin."
- Each demo builds its label with its pure `*EventLabel(SwipeResult<T>)` function.
  These functions are the unit-tested, deterministic core.

## Assets & replaceable placeholders

```kotlin
sealed interface Artwork {
    val contentDescription: String
    data class Placeholder(val seed: Color, val initials: String,
                           override val contentDescription: String) : Artwork
    data class Image(@DrawableRes val resId: Int,
                     override val contentDescription: String) : Artwork
}

@Composable fun ArtworkImage(artwork: Artwork, modifier: Modifier) // placeholder | painterResource
```

- Every Dating/D&D card carries an `Artwork`. Ships **now** as `Placeholder` — a
  tasteful gradient (from `seed`) + large `initials` + a small "replace me" affordance
  so it is visibly a placeholder. Accessibility: `contentDescription` is always set.
- When the generated `.webp` files land in `res/drawable/`, each entry flips one line
  to `Artwork.Image(R.drawable.dating_mira, "…")`. No API or layout change.

## Image-generation prompts (to be produced outside this session)

Deliver as **`.webp`**, no baked-in text, no logos, no card frame/border (the app
draws frames). Lowercase-underscore file names into `sample/src/main/res/drawable/`.

### Dating — full-bleed cartoon portraits · aspect ratio `2:3` · 1024×1536

Files: `dating_mira`, `dating_rowan`, `dating_kai`, `dating_sol`, `dating_nova`.

Template: *"Flat-color cartoon character portrait, friendly modern illustration
style, single person, waist-up, looking at viewer, soft gradient studio background,
portrait 2:3 aspect ratio 1024×1536, full-bleed, no text, no border, no logo.
Character: [DESCRIPTION]."*

- Mira — cheerful botanist, curly hair, holding a small potted plant, warm greens.
- Rowan — climber in a beanie, freckles, easy grin, terracotta/orange palette.
- Kai — jazz drummer, undercut, headphones round neck, cool indigo night palette.
- Sol — ceramicist in an apron, calm smile, holding a teacup, sandy neutral palette.
- Nova — indie game dev, glasses, hoodie, playful expression, violet/magenta palette.

### D&D — square framed illustrations · aspect ratio `1:1` · 1024×1024

Files: `dnd_rogue`, `dnd_warlock`, `dnd_barbarian`, `dnd_ranger`, `dnd_fighter`.

Template: *"Fantasy trading-card illustration, painterly digital art, dramatic
lighting, square 1:1 aspect ratio 1024×1024, centered subject filling the frame, no
text, no card frame, no border, no UI. Subject: [DESCRIPTION]."*

- Rogue — a halfling rogue in a dark leather hood, twin daggers, sly grin, torch-lit.
- Warlock — a tiefling warlock, curling horns, glowing eldritch violet energy in hand.
- Barbarian — a hulking half-orc barbarian mid-roar, greataxe, war paint, stormy sky.
- Ranger — an elf ranger drawing a longbow in a misty forest, green cloak, hawk nearby.
- Fighter — a human knight in plate armor, longsword raised, banner behind, golden light.

## Testing

- **JVM unit tests** (pure-Kotlin, no emulator) for `datingEventLabel`,
  `bankEventLabel`, `dndEventLabel` — direction + card → exact history string, for
  each `SwipeDirection`.
- **App smoke-run** (via the `run`/`verify` skill): open each demo, confirm the deck
  renders at its ratio, rotates indefinitely, a swipe appends the correct history
  entry, and back-nav returns to the gallery without state corruption.

## Acceptance criteria mapping

| Spec criterion | How met |
| --- | --- |
| Gallery lists all three examples | `GalleryScreen` |
| Each example opens on Android and **iOS** | Android ✓; **iOS deferred to Epic 3** |
| All examples use the same library component | All call `SwipingCards` |
| Each deck rotates indefinitely | Inherent to Epic 1 circular deck |
| Visibly different dimensions & content structure | 2:3 / 1.586:1 / 4:5 + distinct layouts |
| Swiping adds a correct event-history entry | `*EventLabel` + `EventHistoryState` |
| No hardcoded dimensions inside the library | Sizing via `modifier` only |
| Example assets work offline | Bundled `res/drawable` webp + procedural bank |
| Navigating between demos does not corrupt deck state | Per-screen disposed/fresh `DeckState` |

## Non-goals (from spec)

Real dating/payment/D&D functionality, networking/backend, accounts, persistent
history, and — for this epic — any `commonMain`/iOS/multiplatform work.

## PR slices (follows the spec's suggested slicing)

1. Gallery shell + hoisted navigator + `EventHistory` + `Artwork` infra; remove old
   idea screen; `MainActivity` → `GalleryApp`.
2. Dating example (2:3) + event-history pattern + label unit tests.
3. Bank example (1.586:1 procedural landscape) + label unit tests.
4. D&D example (4:5 framed square art) + final polish.
