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

**The research structure (locked).** Research is embodied in a **psionic
research building at the main base**, powered by the same **psionic connection**
that gives the Program its power. It carries a **current focus node** and a
**visible progression meter** — no hard countdown — that advances as large drones
physically feed it materials. You can literally watch them stack iron onto it (or
watch a helipad rise), and read each one's completion % and status on the
datapad. Because it draws on the psionic link, it is a **single strategic choke
point**: cripple it — or, one day, sever the psionic connection itself — and the
whole tree stalls. Research is **directed**: the Program focuses the branch that
best counters how you play, and *you can bait that focus away* by changing your
behaviour.

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

## Spatial intel — knowing your area & guarding it
Currently `PlayerIntel` is spatially blind (risk tier / weapon profile / elytra
/ deaths only); the only position memory is a transient `lastSeenPos` inside the
gun goals that dies with the firefight. Add a **strategic spatial layer**:
- **Known area on `PlayerIntel`:** a last-known position + a **decaying "haunts"
  heatmap** of where the player has been seen operating. This is the *same data*
  as the datapad's "it knows you're here" red-chunk tint — one memory, two uses.
- **Contact reporting:** when a recon/combat unit reaches TRACKING/ENGAGING on
  the alert ramp, it reports the contact **up to the Director**, which banks it
  as known-area intel that fades over time (knowledge decays if not refreshed).
- **Guarding / picketing:** once haunts are known, the Director can **lock down
  chokepoints** — park units on a cave mouth the player keeps using, watch the
  base approach, sit on the last-seen point. Turns hunting into route denial.
- Surfaces on the datapad **threat-priority** readout: "knows your general area
  — cave mouth, NE." Bridges the tactical perception layer to the datapad map.

*Sequencing: hooks directly onto the perception pass (the alert ramp is the
trigger for reporting a contact up), so it lands right after.*

## Future — non-player threats & mod compat (much later)
The threat/target model should be **extensible to any dangerous entity, not just
players** — so the autofleet can, one day, engage a modded boss (e.g. a Wither
Storm) with missiles/aircraft/warships from extreme range and **break contact
and flee** when it can't win. Keep engage-or-flee logic keyed on a threat
abstraction, so mod compatibility is a data addition, not a rewrite. Explicitly
deferred until the core game is done.

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

## Locked decisions (2026-07-09)
1. **Research shape:** authored tree with **adaptive focus** — the Program rushes
   the branch that best counters how the player plays.
2. **Support-loss severity:** **graded degrade** — cut support and units can't
   repair/rearm, run down, and are eventually grounded/passive.
3. **Research is embodied** in the psionic research building (see layer 1):
   progression is a **meter**, fed physically by drones, **no hard timer**.
4. **Disruption vectors — all in:**
   - **Starve the supply** (blockade/intercept material, fuel, ammo convoys).
   - **Destroy what they're studying** (the block / unit / wreck being analysed).
   - **Destroy the research building or research units**, or make prototypes
     **too expensive to replace** (economic denial).
   - **Shift their focus** by changing how you play (bait the adaptive research
     off the branch you fear).
   - No countdown to "beat" — instead a visible **progression meter** you race.

## Still open
- **Fuel:** confirm blaze-powder-primary + food-biofuel as the model.
- **Enchanting:** scope + counterplay (fits as a research branch that draws on
  the psionic building).
- **Roads/rail:** real placed blocks (physical, block-able) vs. abstract routes
  drawn on the datapad.
- **Psionic connection:** is severing it a distinct late-game objective, or is
  the research building itself the only handle on it?

---

## Concept-art backlog (owner loves the blockout style)
Produce Minecraft-blocky blockout sheets, gunship-style, for future units:
tank / APC (up-armoured haulers), self-propelled & MRLS artillery, the
helipad / airbase, and the fabrication base. Preview each for approval.
