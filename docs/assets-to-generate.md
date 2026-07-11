# Example gallery — assets to generate

Checklist for the generated artwork the Epic 2 example gallery uses. Bank cards are
code-drawn and need no files.

## Where the files go

```
sample/src/main/res/drawable/
```

File names must be **exactly** as listed (lowercase + underscores — Android rejects
other characters), format **`.webp`**. After the files are in place, the roster code
is swapped one line per entry from `Artwork.Placeholder(...)` to
`Artwork.Image(R.drawable.<name>, "<description>")` in `dating/DatingProfile.kt` and
`dnd/DndCard.kt` — no layout or API change.

## Rules for every image (pass these to the image model)

- No baked-in text, no logos, no watermarks.
- No card frame / border / UI — the app draws the frames. Dating is full-bleed;
  D&D is a full-square illustration placed inside an in-app frame.
- Match the stated aspect ratio exactly.
- Deliver `.webp` (if the tool only exports PNG, convert afterward).

---

## Dating — full-bleed cartoon portraits

- **Aspect ratio:** `2:3` (portrait), e.g. 1024×1536
- **Prompt template:** Flat-color cartoon character portrait, friendly modern
  illustration style, single person, waist-up, looking at viewer, soft gradient
  studio background, portrait 2:3 aspect ratio 1024×1536, full-bleed, no text, no
  border, no logo. Character: **[DESCRIPTION]**.

| Done | File | Character | `[DESCRIPTION]` |
| --- | --- | --- | --- |
| [ ] | `dating_mira.webp` | Mira (botanist) | cheerful botanist, curly hair, holding a small potted plant, warm greens |
| [ ] | `dating_rowan.webp` | Rowan (climber) | climber in a beanie, freckles, easy grin, terracotta/orange palette |
| [ ] | `dating_kai.webp` | Kai (jazz drummer) | jazz drummer, undercut, headphones round neck, cool indigo night palette |
| [ ] | `dating_sol.webp` | Sol (ceramicist) | ceramicist in an apron, calm smile, holding a teacup, sandy neutral palette |
| [ ] | `dating_nova.webp` | Nova (indie dev) | indie game dev, glasses, hoodie, playful expression, violet/magenta palette |

---

## D&D — square framed illustrations

- **Aspect ratio:** `1:1` (square), e.g. 1024×1024
- **Prompt template:** Fantasy trading-card illustration, painterly digital art,
  dramatic lighting, square 1:1 aspect ratio 1024×1024, centered subject filling the
  frame, no text, no card frame, no border, no UI. Subject: **[DESCRIPTION]**.

| Done | File | Character | `[DESCRIPTION]` |
| --- | --- | --- | --- |
| [ ] | `dnd_rogue.webp` | Lila Underbough — Halfling Rogue | a halfling rogue in a dark leather hood, twin daggers, sly grin, torch-lit |
| [ ] | `dnd_warlock.webp` | Kaelen Ashborn — Tiefling Warlock | a tiefling warlock, curling horns, glowing eldritch violet energy in hand |
| [ ] | `dnd_barbarian.webp` | Gruk the Unbroken — Half-Orc Barbarian | a hulking half-orc barbarian mid-roar, greataxe, war paint, stormy sky |
| [ ] | `dnd_ranger.webp` | Sylvara Nightbreeze — Elf Ranger | an elf ranger drawing a longbow in a misty forest, green cloak, hawk nearby |
| [ ] | `dnd_fighter.webp` | Ser Aldric Vane — Human Fighter | a human knight in plate armor, longsword raised, banner behind, golden light |
