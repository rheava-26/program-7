# Program 7 — State of the Project & Expansion Plan

*Working design doc (2026-07-08). Current code reality + decomposition of the
expanded vision: real mining/tunnelling, scaffolding construction, outposts &
depots, large recon swarms, prioritized scouting, and a tiered material tree.
Synthesizes a full-project outline with an engineering second opinion.*

---

## TL;DR — engineering rulings (read this first)

- **Mining is HALF-built, not missing.** `MineResourceGoal` already breaks
  exposed / cave-reachable ore & logs (crack stages, sounds, `breakBlock`,
  banks ledger units). The real gap is **tunnelling** — digging *through* solid
  rock to reach buried veins. That's the hard, XL item.
- **36+ *simultaneous* recon drones is the one number to walk back.** Full AI
  entities are expensive (goal ticks, `BirdNavigation` re-paths, per-tick
  LOS/vision-cone raycasts, client render/tracking, mob caps ~70/player). Ship
  the *feeling* instead: **cap live recon at ~6–10 per player**, rotate them,
  and add a cumulative **"drones fielded today: N"** counter on the datapad
  (director-side tally, zero extra entities). Layer distant whir/particle
  fakery for swarm ambience. Revisit literal 36-live only after profiling.
- **Outposts = one-shot Director placements**, reusing the existing
  `placeAssembler` / `placeLaunchCatapult` pattern — NOT drones physically
  building walls block-by-block. Same "the Program is expanding" beat, a
  fraction of the cost. Live mob-construction is the item to defer/cut.
- **Material tree is cheap.** `Resources` is string-keyed by design; adding
  gold/amethyst/diamond/glowstone/prismarine/netherite + cost gates is data
  work grafted onto plumbing that already exists. High design value, low risk —
  pull it forward.
- **Ship a griefing gate BEFORE aggressive digging.** `MineResourceGoal` does
  **not** currently check the `mobGriefing` gamerule, and there's no exclusion
  around player-placed blocks. The config already promises
  `interfereWithPlayerStructures: off` — honour it in code first, so new digging
  power doesn't compound into eating a player's offline base.

---

## A. Current state — what exists and works

### A1. The Director (`director/ProgramDirectorState`)
Per-world `PersistentState`, ticked each overworld tick. Real today: landing
schedule (first pod ~2–3 days in, 300–600 blocks out, live descent if
witnessed); landing kit (probe core + auto-placed assembler + catapult + 2
surveyors + 2 harvesters + starter stockpile); two-way **heat** dial
(`globalThreat` 0–100, scans raise it, 2 days quiet decays it,
HUNTING/ACTIVE/WATCHFUL/DORMANT/NEUTRAL postures on the datapad); per-player
`PlayerIntel` (risk tier, weapon profile, elytra, deaths); proportional,
weapon-profile-adapted dispatch paid atomically from the ledger; capability
tier-2 gate (threat ≥8 or ≥3 scans); orbital resupply every 3 days per core.
Multi-core bookkeeping exists (`coreSites`) but **no outpost/territory
concept** — every core is a full base clone. No Tier-3 cap, no elytra counter.

### A2. Units (20 entity types, all with renderers)
Combat (working): Attack/Medium/Sniper/Heavy drones, Mortar, IFV (deploys a
fireteam), Gunboat, Recon Helicopter, Battery Center (repair aura), Ground
Drone, Autogun & Anti-Air turrets, Scout Car (target-painting), Air UAV.
Economy: Surveyor (scan+steal), Harvester & Medium Mining Drone (miners),
Logistics/Wheeled Hauler/Transport (couriers). Base physics in
`ProgramDroneEntity`: anti-flier melee bonuses, knockback scramble/crash, rotor
top-slice, mace shatter, inertial flight, typed `ArmorProfile`, wreck loot.

### A3. Mining/harvesting — real but SURFACE-ONLY
`MineResourceGoal` finds nearest wanted block (iron/copper/redstone/coal ore
tags + logs via `HarvestTargets`), drives within 2.8 blocks, grinds 80 ticks
with crack stages, `breakBlock`s it, banks ledger units. Search is 20 wide / 8
tall and only breaks the *target* block — **no tunnelling, no shafts, no cave
entry.** Stalls on buried ore.

### A4. Economy / assembler / couriers — a playable loop
Ledger (`Resources`: iron/copper/redstone/coal/gunpowder/wood, string keys,
built for extension). `AssemblerBlockEntity` keeps a defense quota, charges the
ledger, and **physically ships payment by courier** from the probe core; kill
the courier and the payment spills as loot. The pattern **"Director pays →
courier moves → structure acts"** already exists and is the seed of C2/C3.

### A5. Theft (both on the Surveyor, config-gated)
`StealItemsGoal` (dropped stacks) + `RaidStorageGoal` (cracks unattended
containers, skips program7 block entities). Haul-to-base storage v2 not built.

### A6–A9. Blocks (probe_core, assembler, catapult, wreck, autogun_turret
block+entity); audio via placeholder redirects + interference HUD; 7 shipped
advancements (imperative grants, no criterion framework yet); `P7Config` (7
toggles) + `/program7` commands.

---

## B. Open backlog (playtest)
- **R1:** debris-fall bang, gun cadence/whistle, approach whir, base alarm →
  **shipped**; still open: sound-attraction, camera/sensor blocks, big landfall
  base with crush, bigger/higher fixed-wing, **Datapad v2 GUI (needs sketch)**.
- **R2 DREAD:** shipped turret-block, spotted-stinger, vision beam, faster
  drones, overheat (turret). Open: volumetric explosions, true LOS-hiding,
  hover-over-terrain micro-locomotion, frantic spotted reaction.
- **R3:** long-range engagement ✅, container-raiding ✅; **dig / steal-to-base
  / field-fabrication** = section C; 3D salvage models ✅.

---

## C. Expanded vision, decomposed

### C1. Underground mining/tunnelling — **XL** (hardest item)
Break *non-resource* blocks to get somewhere. Recommended first cut: a
**bore-vector `TunnelGoal`** (no real pathfinding) — lock a target vein/cave,
carve a 2×1/2×2 shaft with crack-stage breaks, advance into the hole, drop
support/light behind. Add Director-designated mine sites, hazard handling
(lava/water/gravel), cave-first doctrine, and an **abstract off-screen
extraction fallback** for unloaded chunks. *Gotchas:* mobGriefing/terrain
config, ticking-chunk limits, `setBlockBreakingInfo` per-viewer packet cost.

### C2. Metallic scaffolding & construction — **L**
Block set (`metal_scaffold`, hull plating, strut, light beacon, depot crate —
all salvage-dropping). `BuildStructureGoal` places a small blueprint
**incrementally** (one block / N ticks, watchable & interruptible) via a
constructor unit, funded through the existing courier system. Foundation scan
reuses `placeAssembler`'s replaceable-block logic. *Respect
`interfereWithPlayerStructures`.*

### C3. Outposts & supply depots — **XL (mostly Director architecture)**
Promote `coreSites` to `BaseSite` records (role MAIN/OUTPOST/DEPOT/MINE_HEAD,
footprint, garrison quota, sub-ledger). Score sites from scout intel; expand by
spending surplus on a constructor+escort+courier caravan (interceptable).
Depots = forward ledger caches so nearby dispatches draw locally and killing a
depot regionally starves responses. **Build depots as one-shot placements
first**, not live construction.

### C4. Large recon presence — **L (the LOD layer is the work)**
Stripped **Recon Drone** frame (no steal goals, light tick) + a Director
sector-coverage plan. **LOD/virtualization is the load-bearing call:** drones
near a player are real entities; beyond ticking range they become **abstract
patrol tokens** ticked by the Director and materialize when a player nears.
Cap real entities per player. See open question on "36 live vs. reported."

### C5. Scouting priorities (caves/structures/ores) — **M; feeds C1/C3/C4**
`ScoutIntel` store keyed by chunk/sector: caves (entrances/volume), structures
(`getStructureAccessor` lookups), ore deposits (budgeted section scans, cached).
Priority queue ranks unscouted sectors by expected value + ledger "appetite."
The intel map doubles as the base-kill lore artifact. *Never scan unloaded
chunks synchronously.*

### C6. Tiered material tree — **M (data) / L–XL (acquisition paths)**
Extend `Resources` with GOLD/AMETHYST/DIAMOND/GLOWSTONE/PRISMARINE/NETHERITE
(+ a smelted CONDUCTOR intermediate from copper). Extend `HarvestTargets`;
deep/geode/Nether/ocean ores hard-depend on C1/C5/Nether-expansion. Gate
escalation on **threat AND materials-banked** (finally the "capability-gated,
never calendar-gated" pillar in code). Surface progress on the datapad
("they're stockpiling amethyst…"). Optional: stolen player items feed the tree.

---

## D. Dependency-ordered build sequence
1. **Director refactor:** `BaseSite` registry + `ScoutIntel` map (pure data,
   everything hangs off it; version the NBT now).
2. **Scouting priorities (C5)** — also improves existing harvesters' targeting.
3. **Recon swarm + LOD virtualization (C4)** — delivers "pervasive presence"
   early and cheaply.
4. **Bore-vector digging (C1 variant b)** — shafts toward designated deposits
   before attempting general break-through pathfinding.
5. **Scaffolding + incremental construction (C2)** — parallel to 4; the
   mine-head entrance is a natural first blueprint.
6. **Outposts & depots (C3)** — composes 1+2+4+5.
7. **Material tree + capability-gated escalation (C6)** — last; converts all of
   the above into visible escalation. Overworld materials first.

*Interleave from backlog:* griefing gate + `mobGriefing` check **before** step
4; sound-attraction + spotted-stinger polish during step 3; base-structure
growth during step 5; Tier-3 cap + base attackability before step 7.

---

## E. Open questions for the owner (highest leverage first)
1. **Scaling approach:** capped live recon (~6–10/player) + cumulative datapad
   counter + distant fakery (recommended), vs. literal 36+ simultaneous
   entities (needs heavy virtualization, perf risk)?
2. **Virtualization stance:** OK with distant drones/mines/outposts as abstract
   tokens that materialize near players? (Required for any real scale.)
3. **"First day" pacing:** 36+ recon within the first day *after landfall*, or
   of the world? How fast should omnipresence arrive vs. the slow-dread opening?
4. **Terrain footprint at default config:** persistent player-explorable
   tunnels (recommended) + how aggressive is scaffolding near player territory?
5. **Missing materials:** if a world lacks amethyst/netherite, may orbital
   resupply trickle substitutes, or does escalation legitimately stall (a valid
   *deny-them-diamonds* strategy)?
6. **Depot economics:** per-site sub-ledgers (regional starvation, more
   bookkeeping) vs. one global ledger with delivery delays (simpler)?
7. **Datapad v2 sketch** still blocks the GUI — and the C5 intel map is its main
   data source, so it's doubly needed.
8. **Do stolen player items feed the material tree?** (Thematic gold; needs
   anti-degenerate rules.)
