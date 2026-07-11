# Example gallery — image generation prompts

Copy-paste prompts for the generated artwork the Epic 2 example gallery uses. Each
image below has a **complete, standalone prompt** — just copy the block into Imagen,
set the **aspect ratio** noted above it, and generate. Bank cards are code-drawn and
need no images.

**Counts (deliberately different, to show the deck handles any size):** Dating = 5
portraits · D&D = **10** square illustrations · Streaming = 6 posters · Bank = 3
(code-drawn, no images). So there are **21 images total** to generate.

**Streaming note:** these are original, mood/genre-based poster illustrations that evoke
each show — deliberately **no real actor likenesses, no real logos, no copied poster
art**. The app renders each show's title and tags as text, so the poster only needs to
capture the vibe.

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

# Dating — cartoon portraits (5)

**Aspect ratio for all 5: `2:3` (portrait).**

### D1. `dating_mira.webp` — ratio `2:3`
```
Flat-color cartoon character portrait, friendly modern illustration style, clean vector-like shading, a single person shown waist-up, facing and looking at the viewer with a warm approachable smile, soft out-of-focus gradient studio background, full-bleed composition filling the entire frame, portrait 2:3 aspect ratio. Character: a cheerful young woman botanist with voluminous curly dark hair, wearing a cozy olive-green cardigan, gently cradling a small potted succulent, warm leafy green color palette. No text, no words, no border, no frame, no logo, no watermark.
```

### D2. `dating_rowan.webp` — ratio `2:3`
```
Flat-color cartoon character portrait, friendly modern illustration style, clean vector-like shading, a single person shown waist-up, facing and looking at the viewer with a warm approachable smile, soft out-of-focus gradient studio background, full-bleed composition filling the entire frame, portrait 2:3 aspect ratio. Character: a friendly young man rock climber with light freckles and a relaxed easy grin, wearing a knit beanie and a rugged jacket, a little chalk dust on his hands, terracotta and burnt-orange color palette. No text, no words, no border, no frame, no logo, no watermark.
```

### D3. `dating_kai.webp` — ratio `2:3`
```
Flat-color cartoon character portrait, friendly modern illustration style, clean vector-like shading, a single person shown waist-up, facing and looking at the viewer with a warm approachable smile, soft out-of-focus gradient studio background, full-bleed composition filling the entire frame, portrait 2:3 aspect ratio. Character: a cool young jazz drummer with a stylish undercut hairstyle, over-ear headphones resting around the neck, wearing a dark tee, casually holding a pair of drumsticks, deep indigo and midnight-blue night palette with subtle neon accents. No text, no words, no border, no frame, no logo, no watermark.
```

### D4. `dating_sol.webp` — ratio `2:3`
```
Flat-color cartoon character portrait, friendly modern illustration style, clean vector-like shading, a single person shown waist-up, facing and looking at the viewer with a warm approachable smile, soft out-of-focus gradient studio background, full-bleed composition filling the entire frame, portrait 2:3 aspect ratio. Character: a calm ceramicist wearing a clay-smudged apron, hair tied back, softly smiling while holding a handmade teacup, sandy beige and warm neutral earthy color palette. No text, no words, no border, no frame, no logo, no watermark.
```

### D5. `dating_nova.webp` — ratio `2:3`
```
Flat-color cartoon character portrait, friendly modern illustration style, clean vector-like shading, a single person shown waist-up, facing and looking at the viewer with a warm approachable smile, soft out-of-focus gradient studio background, full-bleed composition filling the entire frame, portrait 2:3 aspect ratio. Character: a playful indie game developer with round glasses and a colorful hoodie, mischievous smile, faint pixel and synth motifs floating softly in the background, violet and magenta color palette. No text, no words, no border, no frame, no logo, no watermark.
```

---

# D&D — fantasy card illustrations (10)

**Aspect ratio for all 10: `1:1` (square).** A mix of heroes and creatures.

### N1. `dnd_rogue.webp` — ratio `1:1` — Lila Underbough, Halfling Rogue
```
Fantasy trading-card character illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic heroic pose, atmospheric background, square 1:1 aspect ratio. Subject: a halfling rogue — a small nimble humanoid with a sly confident grin, wearing a dark hooded leather outfit, wielding two gleaming daggers in a ready crouch, a torch-lit stone dungeon corridor behind her with moody warm firelight. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

### N2. `dnd_warlock.webp` — ratio `1:1` — Kaelen Ashborn, Tiefling Warlock
```
Fantasy trading-card character illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic heroic pose, atmospheric background, square 1:1 aspect ratio. Subject: a tiefling warlock — a humanoid with crimson skin and long curling horns, glowing violet eldritch energy swirling around one raised hand, wearing ornate dark robes, a faintly glowing arcane sigil behind, mysterious purple lighting. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

### N3. `dnd_barbarian.webp` — ratio `1:1` — Gruk the Unbroken, Half-Orc Barbarian
```
Fantasy trading-card character illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic heroic pose, atmospheric background, square 1:1 aspect ratio. Subject: a half-orc barbarian — a massive muscular green-skinned warrior with tusks, mid-roar in a battle cry, hoisting a huge two-handed greataxe overhead, tribal war paint across his face and arms, a stormy dark sky with dramatic lightning behind him. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

### N4. `dnd_ranger.webp` — ratio `1:1` — Sylvara Nightbreeze, Elf Ranger
```
Fantasy trading-card character illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic heroic pose, atmospheric background, square 1:1 aspect ratio. Subject: an elf ranger — a graceful pointed-eared archer drawing a longbow with an arrow nocked, wearing a hooded green forest cloak, a hawk in flight nearby, a misty sunlit ancient forest background with cool green light. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

### N5. `dnd_fighter.webp` — ratio `1:1` — Ser Aldric Vane, Human Fighter
```
Fantasy trading-card character illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic heroic pose, atmospheric background, square 1:1 aspect ratio. Subject: a human knight fighter in polished plate armor, longsword raised confidently, a heraldic banner waving behind, standing resolute, warm golden heroic lighting on a battlefield at dawn. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

### N6. `dnd_cleric.webp` — ratio `1:1` — Brother Halden, Dwarf Cleric
```
Fantasy trading-card character illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic heroic pose, atmospheric background, square 1:1 aspect ratio. Subject: a dwarf cleric — a stout bearded dwarf in gleaming ornate plate armor, raising a glowing holy warhammer and a shield bearing a radiant sun emblem, warm divine golden light haloing him inside a stone temple. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

### N7. `dnd_bard.webp` — ratio `1:1` — Fenn Quickstring, Half-Elf Bard
```
Fantasy trading-card character illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic heroic pose, atmospheric background, square 1:1 aspect ratio. Subject: a half-elf bard — a charismatic pointed-eared young man mid-performance strumming an ornate lute, wearing a flamboyant feathered hat and a traveler's coat, warm tavern candlelight with floating musical sparkles around him. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

### N8. `dnd_dragon.webp` — ratio `1:1` — Ashmaw, Young Red Dragon (creature)
```
Fantasy trading-card creature illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic menacing pose, atmospheric background, square 1:1 aspect ratio. Subject: a young red dragon — a fierce scaled crimson reptilian beast with spread leathery wings, snarling with smoke curling from its jaws, perched atop a glittering hoard of gold coins, fiery orange and red cavern lighting. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

### N9. `dnd_owlbear.webp` — ratio `1:1` — Bramblebeak, Owlbear (creature)
```
Fantasy trading-card creature illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic menacing pose, atmospheric background, square 1:1 aspect ratio. Subject: an owlbear — a monstrous hybrid with the feathered head and hooked beak of a giant owl and the massive muscular brown-furred body and claws of a bear, rearing up mid-roar in a shadowy forest, moody green-and-brown lighting. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

### N10. `dnd_troll.webp` — ratio `1:1` — Gorehide, Cave Troll (creature)
```
Fantasy trading-card creature illustration, painterly digital art, rich detail, dramatic cinematic lighting, a single subject centered and filling the frame in a dynamic menacing pose, atmospheric background, square 1:1 aspect ratio. Subject: a cave troll — a towering gangly green-skinned troll with long grasping claws and a wart-covered hide, hunched and menacing in a damp torch-lit cavern, sickly green and amber lighting. No text, no words, no card frame, no border, no UI, no logo, no watermark.
```

---

# Streaming — poster illustrations (6)

**Aspect ratio for all 6: `2:3` (portrait movie-poster), e.g. 1024×1536.** Original
mood/genre art only — **no real people, no logos, no copied posters** (the app draws
the title + tags itself). Cinematic, evocative, full-bleed.

### S1. `show_stranger_things.webp` — ratio `2:3`
```
Cinematic 1980s supernatural-horror movie poster, moody portrait 2:3 aspect ratio, full-bleed. A group of kids on bicycles in silhouette against an ominous dark pine forest, an eerie upside-down otherworldly sky glowing blood-red, retro small-town Americana, drifting fog, dramatic red-and-teal lighting, subtle film grain. No text, no words, no logo, no watermark, no recognizable real people.
```

### S2. `show_squid_game.webp` — ratio `2:3`
```
Cinematic survival-thriller movie poster, portrait 2:3 aspect ratio, full-bleed. Ranks of faceless figures in matching teal-green tracksuits standing inside a giant surreal maze of pastel pink and mint staircases, masked guards in pink jumpsuits with simple geometric black masks, ominous playground colors, harsh high-contrast lighting, unsettling tense mood. No text, no words, no logo, no watermark, no recognizable real people.
```

### S3. `show_the_crown.webp` — ratio `2:3`
```
Prestige historical royal-drama movie poster, portrait 2:3 aspect ratio, full-bleed. An elegant silhouette of a regal woman wearing an ornate jeweled crown, seated on a throne in a grand candlelit palace hall, rich golds and deep royal blues, oil-painting cinematic lighting, dignified and somber. No text, no words, no logo, no watermark, no recognizable real people.
```

### S4. `show_dark.webp` — ratio `2:3`
```
Bleak sci-fi mystery movie poster, portrait 2:3 aspect ratio, full-bleed. A lone hooded figure standing at the mouth of a dark dripping cave tunnel with an eerie glowing light, a nuclear power-plant silhouette and dense pine forest under a stormy sky, cold desaturated blue-green palette, ominous time-travel atmosphere. No text, no words, no logo, no watermark, no recognizable real people.
```

### S5. `show_mindhunter.webp` — ratio `2:3`
```
Moody 1970s crime-thriller movie poster, portrait 2:3 aspect ratio, full-bleed. A dim interrogation room with a single hanging lamp casting hard shadows over an empty metal chair, curling cigarette smoke, muted brown and amber tones, tense psychological noir atmosphere. No text, no words, no logo, no watermark, no recognizable real people.
```

### S6. `show_kpop_demon_hunters.webp` — ratio `2:3`
```
Vibrant animated action-fantasy movie poster, portrait 2:3 aspect ratio, full-bleed. Stylized K-pop idol warriors in glowing neon stage outfits wielding magical weapons against shadowy demons, dynamic heroic poses, explosive pink-purple-cyan neon lighting, energetic modern animation style. No text, no words, no logo, no watermark, no recognizable real people.
```

---

## Progress — all 21 delivered ✅

Dating (5):    [x] mira  [x] rowan  [x] kai  [x] sol  [x] nova
D&D (10):      [x] rogue  [x] warlock  [x] barbarian  [x] ranger  [x] fighter  [x] cleric  [x] bard  [x] dragon  [x] owlbear  [x] troll
Streaming (6): [x] stranger_things  [x] squid_game  [x] the_crown  [x] dark  [x] mindhunter  [x] kpop_demon_hunters
