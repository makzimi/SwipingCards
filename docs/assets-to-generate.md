# Example gallery — image generation prompts

Copy-paste prompts for the generated artwork the Epic 2 example gallery uses. Each
image below has a **complete, standalone prompt** — just copy the block into Imagen,
set the **aspect ratio** noted above it, and generate. Bank cards are code-drawn and
need no images.

## Where the files go

```
sample/src/main/res/drawable/
```

File names must be **exactly** as listed (lowercase + underscores — Android rejects
other characters), format **`.webp`**. After the files are in place, the roster code
is swapped one line per entry from `Artwork.Placeholder(...)` to
`Artwork.Image(R.drawable.<name>, "<description>")` — no layout or API change.

## What is shared vs. different

- **Dating** images share one style (flat cartoon portrait, 2:3) — only the character
  differs. **D&D** images share one style (painterly fantasy card art, 1:1) — only the
  subject differs. The shared style is already written into every prompt below, so each
  block is self-contained.
- **Every image, both sets:** no baked-in text, no words, no logo, no watermark, no
  card frame/border/UI (the app draws frames). Deliver `.webp` (if Imagen exports PNG,
  convert afterward).

---

# Dating — cartoon portraits

**Aspect ratio for all 5: `2:3` (portrait).**

### 1. `dating_mira.webp` — ratio `2:3`
```
Flat-color cartoon character portrait, friendly modern illustration style, clean vector-like shading, a single person shown waist-up, facing and looking at the viewer with a warm approachable smile, soft out-of-focus gradient studio background, full-bleed composition filling the entire frame, portrait 2:3 aspect ratio. Character: a cheerful young woman botanist with voluminous curly dark hair, wearing a cozy olive-green cardigan, gently cradling a small potted succulent, warm leafy green color palette. No text, no words, no border, no frame, no logo, no watermark.
```

### 2. `dating_rowan.webp` — ratio `2:3`
```
Flat-color cartoon character portrait, friendly modern illustration style, clean vector-like shading, a single person shown waist-up, facing and looking at the viewer with a warm approachable smile, soft out-of-focus gradient studio background, full-bleed composition filling the entire frame, portrait 2:3 aspect ratio. Character: a friendly young man rock climber with light freckles and a relaxed easy grin, wearing a knit beanie and a rugged jacket, a little chalk dust on his hands, terracotta and burnt-orange color palette. No text, no words, no border, no frame, no logo, no watermark.
```

### 3. `dating_kai.webp` — ratio `2:3`
```
Flat-color cartoon character portrait, friendly modern illustration style, clean vector-like shading, a single person shown waist-up, facing and looking at the viewer with a warm approachable smile, soft out-of-focus gradient studio background, full-bleed composition filling the entire frame, portrait 2:3 aspect ratio. Character: a cool young jazz drummer with a stylish undercut hairstyle, over-ear headphones resting around the neck, wearing a dark tee, casually holding a pair of drumsticks, deep indigo and midnight-blue night palette with subtle neon accents. No text, no words, no border, no frame, no logo, no watermark.
```

### 4. `dating_sol.webp` — ratio `2:3`
```
Flat-color cartoon character portrait, friendly modern illustration style, clean vector-like shading, a single person shown waist-up, facing and looking at the viewer with a warm approachable smile, soft out-of-focus gradient studio background, full-bleed composition filling the entire frame, portrait 2:3 aspect ratio. Character: a calm ceramicist wearing a clay-smudged apron, hair tied back, softly smiling while holding a handmade teacup, sandy beige and warm neutral earthy color palette. No text, no words, no border, no frame, no logo, no watermark.
```

### 5. `dating_nova.webp` — ratio `2:3`
```
Flat-color cartoon character portrait, friendly modern illustration style, clean vector-like shading, a single person shown waist-up, facing and looking at the viewer with a warm approachable smile, soft out-of-focus gradient studio background, full-bleed composition filling the entire frame, portrait 2:3 aspect ratio. Character: a playful indie game developer with round glasses and a colorful hoodie, mischievous smile, faint pixel and synth motifs floating softly in the background, violet and magenta color palette. No text, no words, no border, no frame, no logo, no watermark.
```

---

# D&D — fantasy card illustrations

**Aspect ratio for all 5: `1:1` (square).**

### 6. `dnd_rogue.webp` — ratio `1:1` — Lila Underbough, Halfling Rogue
```
Fantasy trading-card character illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic heroic pose, atmospheric background, square 1:1 aspect ratio. Subject: a halfling rogue — a small nimble humanoid with a sly confident grin, wearing a dark hooded leather outfit, wielding two gleaming daggers in a ready crouch, a torch-lit stone dungeon corridor behind her with moody warm firelight. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

### 7. `dnd_warlock.webp` — ratio `1:1` — Kaelen Ashborn, Tiefling Warlock
```
Fantasy trading-card character illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic heroic pose, atmospheric background, square 1:1 aspect ratio. Subject: a tiefling warlock — a humanoid with crimson skin and long curling horns, glowing violet eldritch energy swirling around one raised hand, wearing ornate dark robes, a faintly glowing arcane sigil behind, mysterious purple lighting. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

### 8. `dnd_barbarian.webp` — ratio `1:1` — Gruk the Unbroken, Half-Orc Barbarian
```
Fantasy trading-card character illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic heroic pose, atmospheric background, square 1:1 aspect ratio. Subject: a half-orc barbarian — a massive muscular green-skinned warrior with tusks, mid-roar in a battle cry, hoisting a huge two-handed greataxe overhead, tribal war paint across his face and arms, a stormy dark sky with dramatic lightning behind him. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

### 9. `dnd_ranger.webp` — ratio `1:1` — Sylvara Nightbreeze, Elf Ranger
```
Fantasy trading-card character illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic heroic pose, atmospheric background, square 1:1 aspect ratio. Subject: an elf ranger — a graceful pointed-eared archer drawing a longbow with an arrow nocked, wearing a hooded green forest cloak, a hawk in flight nearby, a misty sunlit ancient forest background with cool green light. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

### 10. `dnd_fighter.webp` — ratio `1:1` — Ser Aldric Vane, Human Fighter
```
Fantasy trading-card character illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic heroic pose, atmospheric background, square 1:1 aspect ratio. Subject: a human knight fighter in polished plate armor, longsword raised confidently, a heraldic banner waving behind, standing resolute, warm golden heroic lighting on a battlefield at dawn. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

---

## Progress

Dating: [ ] mira  [ ] rowan  [ ] kai  [ ] sol  [ ] nova
D&D:    [ ] rogue  [ ] warlock  [ ] barbarian  [ ] ranger  [ ] fighter
