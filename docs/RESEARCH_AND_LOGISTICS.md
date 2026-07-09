# Program 7 — Research, Infrastructure & Counter-Logistics

*Design capture (2026-07-09 session). The owner's vision for what escalation
actually **is**: units gated not by a timer but by **research + infrastructure
+ supply**, all surfaced and made disruptable through the datapad. The intended
result is that Program 7 plays as a **counter-logistics war** — you win by
reading the Program's tech-and-supply web and cutting the right thread, not by
out-damaging a spawn list. This is the through-line behind every "fair but
difficult / react and disrupt / infrastructure to work" note so far.*

---

## The three interlocking layers

### 1. Research — *what they can build*
A wide tech tree. The Program does **not** reach a tier by threshold; it
**researches** specific capabilities by studying blocks and units it has
encountered, and it **focuses** research based on how the player plays
(player walls up → research bombardment; player brings a bow → research armour;
player floods a base with mobs → research area-denial). Each node:
- unlocks a **unit or capability**, and
- **defines that unit's requirements** — the materials, parts, fuel, ammo, and
  **infrastructure** it needs to be built and sustained.

Replaces the current raw `tier2Unlocked()`-style threshold gate with a real,
legible, disruptable progression. This is the "capability-gated, never
calendar-gated" design pillar finally made concrete and **player-facing**.

### 2. Infrastructure & logistics — *what lets them build it and keep it alive*
Units are not free-floating spawns; they depend on standing structures.
- **Production:** specialised plants make sub-components (aircraft parts, large
  rotors), an **assembler/fabrication base** puts them together with large drones.
- **Fuel economy:** furnace fuels run machines; **food → biofuel** (biofuel
  generators — thematic for a colonised world); **blaze powder → refined
  high-power fuel** (primary for heavy units / aircraft).
- **Ammo economy:** an ammo plant manufactures rounds; heavy units must be
  **rearmed at base** — starve the ammo and they go quiet.
- **Sustainment structures:** a **helipad / airbase** gives a unit a place to
  **land, repair, rearm, shelter from harsh weather, and be defended** (hostile
  mobs *and* the player attack these units and their infra, so it must be a
  **fortified position**).
- **Support loss = weakness:** cut a unit's support and it **degrades** —
  can't repair/rearm, runs down, eventually grounded — rather than being a
  free-standing invincible threat.

### 3. The datapad — *the window that makes it disruptable*
Surfaces the research tree, the **current focus node + progress**, *what block
or unit they're currently studying*, and the **infrastructure/supply map**
(bases, helipads, depots, mine-heads, supply routes). This is what converts all
the hidden systems into player-actionable intel and closes the core loop:
**see the plan → go break the right piece.**

---

## Worked example — the Gunship (Tier 4 apex)

The concrete chain the owner spelled out, as the reference implementation of the
whole architecture:

- **Research prerequisites:** aircraft-frame → large-rotor → ducted-tiltrotor nodes.
- **Materials:** aircraft parts, large rotors, heavy iron/copper, high-power fuel.
- **Build infrastructure:** a fabrication base with a large assembler + large
  drones to assemble it; a fuel plant; an ammo plant.
- **Sustainment:** a **helipad/airbase** to land, repair, rearm, and shelter in
  harsh weather — itself **defended** against mobs and the player.
- **Without support:** can't repair or rearm → degrades and is eventually grounded.
- **Disruption vectors:** destroy the helipad (ground it) · intercept the
  parts/fuel/ammo convoy · kill the research before its first flight.

Silhouette locked separately in `UNITS.md`: ducted-tiltrotor Osprey with a
helicopter tail rotor and belly autocannon.

---

## Other systems in the vision

- **Enchanting (research branch):** the Program learns to enchant by studying
  the player's enchanting setup / raided gear; needs a lapis + XP analogue;
  applies to units or salvage. Counter by denying the knowledge source.
- **Supply lines & territory:** roads / railways physically link
  bases ↔ outposts ↔ depots ↔ mine-heads; haulers run them (interceptable).
  **Defensible outposts** garrison small-drone swarms or artillery, and *what*
  gets built **adapts to how the player plays**.

---

## Grounding — what already exists to build on
- **Couriers** + the `AssemblerBlockEntity` "Director pays → courier moves →
  structure acts" loop = the seed of part/fuel/ammo delivery and supply lines.
- **Outpost construction** (incremental, block-by-block) = the seed of building
  helipads / plants / fortified positions.
- **`Resources`** ledger is string-keyed and extension-ready = drop in
  aircraft-parts, rotors, fuels, ammo, enchant-analogue as new keys.
- **Capability tier gate** = the placeholder the research tree replaces.
- **Datapad v2** intel design (separate doc/artifact) = the surface for the
  research view + infra map.

---

## Open decisions (need the owner)
1. **Research shape:** authored tree with **adaptive focus** (recommended —
   buildable + legible + still reactive), fully **emergent/procedural**, or
   simple **linear tiers**?
2. **Disruption vectors** (likely several): destroy the block/unit they're
   *studying* · kill a **research/"cortex" structure** · **starve** the
   materials/supply · **beat a timer**. Which are must-haves?
3. **Support-loss severity:** **graded degrade** (recommended — can't
   repair/rearm, runs down), **hard** (support dies → unit falls), or **soft**
   (only blocks new production)?
4. **Fuel:** confirm blaze-powder-primary + food-biofuel as the model.
5. **Enchanting:** scope and player counterplay.
6. **Roads/rail:** real placed blocks (physical, block-able) vs. abstract routes
   drawn on the datapad?

---

## Concept-art backlog (owner loves the blockout style)
Produce Minecraft-blocky blockout sheets, gunship-style, for future units:
tank / APC (up-armoured haulers), self-propelled & MRLS artillery, the
helipad / airbase, and the fabrication base. Preview each for approval.
