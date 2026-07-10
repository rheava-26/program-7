# Program 7 — Abstract Combat Framework (off-screen / statistical resolution)

> The war does not pause when you leave the chunk. This is the framework for
> the **off-screen resolution** backbone named in `FRAMEWORKS.md`: how the
> Program's units keep fighting, moving, and shelling in chunks nobody is
> standing in, without simulating a single tick of real combat there. It is
> the general engine that `ARTILLERY_AND_INDIRECT_FIRE.md` §7 and
> `SUPPLY_NETWORK.md` §5.4 both lean on instead of inventing their own paper
> math. Vision lives in `EXPANSION_PLAN.md` C7; this is the architecture that
> makes it real.

## 1. The core idea

**Distance from a player is a fidelity dial, not an off switch.** Close up,
Program units are real entities doing real pathing and real damage. Past
`VirtualFleet`'s `DEMATERIALIZE_RANGE` (96 blocks), they become `DroneToken`s —
inert data today. This framework gives those tokens (and the depots/missions
tied to their region) **something to do while nobody's watching**: a coarse,
cheap, honest statistical model that keeps producing the *consequences* of
combat — losses, loot, drained ammo, craters — without ever spawning an entity
or running a tick of pathfinding for it.

The payoff is the horror-fantasy premise stated up top: **the invasion is
everywhere**, not just wherever the player happens to be rendering. A player
who never visits the far side of the map should still be able to *find out*,
by ear and by walking into the aftermath, that a battle happened there.

Three things ride this one engine (§5):
- **Combat** — the fleet fighting mobs, raids, and (later) modded threats.
- **Supply movement** — convoys and depots draining/refilling on paper
  (`SUPPLY_NETWORK.md`).
- **Bombardment** — fire missions completing in unloaded chunks
  (`ARTILLERY_AND_INDIRECT_FIRE.md` §7).

None of them get their own off-screen math. They all call the same resolver.

## 2. Which backbones this reuses

Per the `FRAMEWORKS.md` rule — *"nothing is allowed to invent its own private
perception, economy, audio, or off-screen path"* — this framework is itself
the off-screen backbone, and it still must not invent anything **beneath**
itself:

| Backbone | How abstract combat uses it |
|---|---|
| **Off-screen substrate** (`VirtualFleet`/`DroneToken`) | The unit census. A `DroneToken`'s position and saved NBT (health, alert state, loadout) are the "how many, how strong" input to every roll. This framework is what finally gives tokens behaviour. |
| **Supply / ledger** (`ProgramDirectorState` `tryConsume`/`getResource`) | Every resolved event is a ledger transaction: ammo debited, loot credited, upkeep drained. No parallel currency. |
| **Acoustic** (`ProgramAcoustics.emit`) | Every resolved event that would have made noise emits through the normal travel-time/distance/occlusion pipeline, from the *real* world position of the event — a distant player hears it exactly as if it happened in a loaded chunk. |
| **Datapad** (`DatapadSnapshotPayload`) | Off-screen activity is *gestimated*, never precisely reported — a bearing and a fuzzy label, the same epistemics as acoustic contacts, not a new radar layer. |
| **Persistence discipline** | Resolved-but-unmaterialized outcomes (a crater, a casualty, a depot's paper stock) persist in NBT exactly like pod-descent state and virtualization tokens, so a server restart never strands or drops them. |
| **Tier gating** | Off-screen engagements only field what the Program could actually field on-screen at that tier/threat — no phantom Tier 4 fleets in regions that haven't earned them. |

It invents exactly one new thing: the **resolver** itself (§3) and its data
model (§4). That's the "new manager + data, not a new engine" the bar in
`FRAMEWORKS.md` asks for.

## 3. The manager: `AbstractCombatResolver`

A Director organ, sibling of `VirtualFleet` and (planned) `FireMissionManager`
and `SupplyNetwork`, owned by `ProgramDirectorState`:

```
ProgramDirectorState (the brain, PersistentState)
├── VirtualFleet             — off-screen unit tokens              [exists]
├── AbstractCombatResolver   — statistical resolution engine        [planned, this doc]
├── FireMissionManager       — indirect fire                        [planned]
├── SupplyNetwork            — infrastructure, depots, supply lines [planned]
└── ...
```

**State it holds:**
- A **region index**: unloaded chunks grouped into coarse cells (e.g. 4×4
  chunks, matching the granularity used for region-level checks elsewhere) that
  currently contain at least one `DroneToken`, an active off-screen supply link,
  or an in-flight fire mission targeting them. Cells with nothing in them are
  never visited — this is a sparse index, not a world-wide grid scan.
- Per active cell: a **last-resolved tick**, so the resolver knows how much
  paper time has elapsed since it last ran that cell and can roll the right
  number of cycles instead of drifting.
- **Pending materialization records** (§6): the outcomes it decided but hasn't
  applied to the real world yet, keyed by chunk, so a chunk load can find and
  apply them.

**How it ticks (coarse, cheap by design):**
- Runs off the Director's own tick, but **throttled hard** — once every few
  seconds per cell (same order of magnitude as `VirtualFleet.SCAN_INTERVAL`,
  40 ticks), and even then only cells that actually have something in them get
  visited. A cell with tokens but no threat and no mission just has its
  last-resolved tick bumped and is skipped — nothing to roll.
- Never touches chunk loading. Every read (token position, "is this area
  dark") uses data already carried by the token or cheap world queries that
  don't force a load — never `getChunk(..., true)`.
- **NBT-persisted**: the region index, per-cell timestamps, and pending
  materialization records all round-trip in `ProgramDirectorState`'s NBT,
  written and read the same pass as `VirtualFleet` (see `writeNbt`/`fromNbt`
  around line 1156/1227) — an in-flight off-screen skirmish or an unmaterialized
  crater is never dropped by a restart, exactly the persistence discipline
  applied to pod-descent state.

## 4. The statistical resolution model

For one active cell, one resolver pass runs a fixed five-stage pipeline. This
*is* the "one engine, three clients" promise — combat, supply, and
bombardment all enter at a different stage but share stages 1–2 and the
output shape of stages 3–5.

| Stage | Question | Output |
|---|---|---|
| **1. Spawn-check** | Can hostiles even exist here right now? | pass/fail gate |
| **2. Muster** | How many forces on each side? | force counts |
| **3. Resolve** | Who wins, what's the cost? | losses per side |
| **4. Produce** | What does the world owe as a result? | loot/damage/consumption record |
| **5. Broadcast** | What would this have sounded like? | one `ProgramAcoustics.emit` call |

### 4.1 Spawn-check

Before rolling a combat engagement, the resolver asks the same question
vanilla asks before placing a hostile mob: **light level, Y, biome, and
time-of-day** at the token's (or the cell's representative) position. This
reuses vanilla's own spawn-legality logic (`SpawnHelper`-style checks —
light ≤ threshold, valid block below, not inside a solid block) evaluated
against cached chunk data, not a live mob-spawn attempt. If the position
wouldn't legally spawn a hostile mob, there is **no mob threat to fight** —
the stage short-circuits and the cell has nothing to resolve this pass beyond
bumping its timestamp. This one gate is what keeps the resolver from
"fighting" imaginary threats in well-lit, mob-proofed areas and keeps the cost
near zero for cells that are actually quiet.

Non-mob threats (a raid, later a modded boss) skip the spawn-check — they have
their own existence condition (an active raid flag, a boss's own presence) —
and go straight to muster.

### 4.2 Muster (count the forces)

- **Program side:** every `DroneToken` in the cell that is combat-capable
  (its saved NBT carries a weapon/ranged-attacker flag — the same predicate
  `ProgramDirectorState` already uses live, e.g. `isRangedAttacker()`), summed
  by rough class (gun/heavy/recon — recon doesn't fight).
- **Threat side:**
  - *Mobs:* an **expected count**, not real entities — a statistical estimate
    from local difficulty, biome, and time-of-day (more at night, more in
    dark biomes), the same inputs vanilla's spawn cap uses, scaled down for
    the area's `DEMATERIALIZE_RANGE`-sized footprint. No entity is ever
    created to be counted.
  - *A raid/other structured threat:* its own tracked wave size, already data
    (a raid already carries a wave count) — the resolver just reads it instead
    of re-deriving it.

If the Program side is empty (no combat drones in the cell), there's nothing
to resolve — mobs simply exist unopposed on paper, which is correct: the
Program isn't fighting a war in a cell it hasn't sent anyone to.

### 4.3 Resolve (outcome)

A single weighted roll per pass, not a tick-by-tick sim:

```
programPower = Σ (unitClass.power * unitClass.count) * supplyFactor
threatPower  = Σ (threatClass.power * threatClass.count)

winProbability = programPower / (programPower + threatPower)
```

- `supplyFactor` is **read from the ledger/SupplyNetwork upkeep state**, not
  re-derived: a token whose owning unit class is currently starved of ammo
  (per `SUPPLY_NETWORK.md` §2 degradation) fights at reduced power here too —
  the same brownout, just expressed statistically instead of as a slower
  cadence. This is the concrete tie between the two frameworks: starve supply
  and off-screen fights start going worse for the Program, same as on-screen.
- One Bernoulli-style roll (or a small number of rounds for a multi-wave raid)
  decides the outcome; **losses on each side are proportional to the loser's
  power deficit**, not all-or-nothing — a narrow win still costs the Program
  a drone or two, a narrow loss still thins the mob count. This keeps outcomes
  legible ("the garrison held, but took losses") instead of coin-flip swingy.
- Losses apply directly to the token list: a losing `DroneToken` is removed
  from `VirtualFleet` (it died off-screen — it will not materialize later).
  A winning side's survivors stay as tokens, ready to fight the next pass or
  eventually materialize when a player wanders in.

### 4.4 Produce (results)

Every resolved pass writes one **outcome record**, the shared payload shape
for all three clients (§5):

```
OutcomeRecord {
  cellPos, tick
  ledgerDelta      // ammo consumed (debit), loot/salvage recovered (credit)
  casualties       // Program tokens removed
  threatsCleared   // mobs/raid waves statistically killed
  materialization  // what to place when the chunk loads (§6)
  soundProfile     // what stage 5 should emit
}
```

- **Ledger:** ammo spent by the winning side's power draw is **debited from
  the ledger up front**, exactly like a live fire mission (§2 of the artillery
  doc) — an off-screen skirmish that the ledger can't afford to arm simply
  doesn't happen (the Program token sits idle, unresolved, until supply
  recovers). Loot from cleared mobs is **credited to the ledger directly**,
  never spawned as real item entities — this is the loot-banking rule from
  `EXPANSION_PLAN.md` C7: off-screen kills bank straight to the economy, and
  only a *player physically present* at the moment of a kill sees a ground
  drop. This sidesteps the dupe/despawn/lag problem of item entities piling up
  in chunks nobody visits.
- **Casualties/threats** feed straight back into stage 2's inputs for the next
  pass (fewer tokens next time; threat count decays if the Program won).

### 4.5 Broadcast (heard, not seen)

See §7 — every produced outcome that would have made noise gets exactly one
`ProgramAcoustics.emit` call from its real world position.

## 5. The three off-screen clients, one engine

| Client | Enters at | Threat side | Ledger effect | Materializes as |
|---|---|---|---|---|
| **Combat** (fleet vs. mobs/raids) | Stage 1 (spawn-check) | mobs, raid waves, later modded bosses | ammo debit, loot credit | corpses/blood(none — banked loot only), thinned token list |
| **Supply movement** (convoys/depots) | Stage 3 directly (no spawn-check — nothing to fight unless a convoy is ambushed) | none, or an ambushing threat if one musters | stock moves depot→depot on paper; a successful ambush credits the raider and debits the link | a convoy caught mid-route materializes wherever it paper-was; a depot's stock level is just correct when loaded |
| **Bombardment** (fire missions completing off-screen) | Stage 4 directly (the mission already resolved *who* and *where* — see artillery §2; the resolver just applies impacts on paper) | the mission's existing target | ammo debit already happened at mission start (artillery §2 step 2) | craters/block damage per the terrain-erosion model (artillery §5a), applied on load |

The row differences are just **which stage a client enters at** — a fire
mission has already done its own targeting (it doesn't need a spawn-check or
a muster, it needs stages 4–5 applied to unloaded terrain), while a
combat skirmish needs the full pipeline. Nothing here is a second engine;
it's the same `AbstractCombatResolver.resolve(cell)` call with different
entry points, matching how `FRAMEWORKS.md` describes every new system as
"mostly data + one manager, not a new engine."

## 6. Consistency on load (paper and reality must agree)

When a player approaches a cell with pending `OutcomeRecord`s:

- On **chunk load** (hooked the same way `VirtualFleet.materializeNearby`
  already checks `world.getChunk(..., FULL, false)` before acting), the
  resolver **applies the backlog** for that chunk in tick order before
  anything else touches it:
  1. Terrain damage (craters, scarring) is applied via the existing block-set
     path (same primitive the artillery erosion model uses), so a crater a
     player walks up to *is already there* the instant the chunk finishes
     loading, not built in front of them.
  2. Any surviving `DroneToken`s materialize normally through `VirtualFleet`
     (unchanged — this framework never touches that round trip, only what the
     tokens *did* before materializing).
  3. Casualty tokens do **not** materialize (they're gone) but may drop a
     cosmetic wreck/decal if the outcome record flags visible aftermath —
     optional polish, not required for correctness.
  4. Ledger effects (ammo/loot) were already applied at resolution time
     (§4.4), not deferred to load — the economy must stay correct even if the
     player *never* visits the cell.
- Pending records are removed from the NBT-persisted backlog once applied,
  same discipline as any other in-flight state: **nothing observable is ever
  double-applied or silently dropped** across a save/load boundary.
- This is the "paper and reality agree" contract: nothing the resolver decided
  is allowed to be contradicted by what the player actually finds.

## 7. Audio over the horizon (the payoff)

This is the reason the whole framework exists: **you should be able to hear a
war you can't see.**

- Every produced outcome with a `soundProfile` calls `ProgramAcoustics.emit`
  from the event's **real world position** — a firefight resolved 400 blocks
  away still goes through the same travel-time delay, distance-band shaping
  (close/medium/far), and rock-occlusion muffling as anything happening in a
  loaded chunk (`ProgramAcoustics.emit`, `PLAYER_SEARCH_RADIUS` = 220 already
  covers "far enough to matter, not so far it's wasted work").
- **Sustained** combat (several passes in a row resolving hostile) escalates
  to the rhythmic-night-fire signature `EXPANSION_PLAN.md` C7 calls out —
  the datapad's acoustic classifier reads repeated resolver emissions the
  same way it reads repeated live gunfire, no special-casing needed because
  it's the *same* `ProgramAcoustics` call site.
- A resolved **bombardment** pass emits the deep-boom profile per
  `ARTILLERY_AND_INDIRECT_FIRE.md` §7 — "a barrage over the horizon."
- **Datapad gestimation:** the datapad never receives an exact record. It
  receives what it always receives — an acoustic event with a bearing and a
  coarse type guess — and applies the existing "probably X, maybe Y"
  ambiguity from `SOUND_DESIGN.md` §4. An off-screen skirmish is
  indistinguishable, from the player's epistemic position, from an on-screen
  one they simply haven't walked up on yet. That indistinguishability *is*
  the feature — there is no tell that reveals "this one was faked by
  statistics."

## 8. Fidelity vs. cost (the performance budget)

The entire point is that this must be **cheap enough to run for the whole
world, forever, in the background.** Concrete budget rules:

- **Sparse indexing, not a world scan.** Only cells holding a token, an active
  link, or a mission are ever visited (§3) — a fully quiet world costs
  nothing per tick beyond an empty-set check.
- **Coarser the farther out.** Cells with a player anywhere near (inside
  `VirtualFleet.MATERIALIZE_RANGE` plus a margin) aren't resolved by this
  system at all — that's live combat's job. Beyond that, resolution interval
  can **widen with distance from the nearest player** (e.g. every 40 ticks
  just past the dematerialize boundary, stretching to every few hundred ticks
  for a cell no player has been near in real-world hours) — distant cells get
  a coarser, less frequent roll, never a per-tick one: simulation gets less
  accurate the less anyone could possibly notice.
- **No entity creation, no pathfinding, no chunk-loading side effects,** ever,
  in the resolution path — a violation of any of these three is a bug, not a
  tuning choice, per the C7 gotcha ("never scan unloaded chunks synchronously").
- **One roll per pass**, not a tick-by-tick sim — multi-round combat is
  represented by scaling losses/duration in the roll's math (§4.3), not by
  looping ticks.
- **Backlog is bounded**, not infinite: an outcome record older than some
  ceiling (e.g. a real-world week of never being visited) can be compacted
  into the cell's ambient state (a rougher "this area is contested" flag)
  rather than kept as a growing list of individual events — keeps NBT size
  bounded even on a server that never gets fully explored.

## 9. Build order

1. **`AbstractCombatResolver` skeleton** — the region index, per-cell
   timestamp, NBT round-trip, wired into `ProgramDirectorState` as a sibling
   of `VirtualFleet`, doing *nothing* yet but proving the persistence and
   the sparse-scan discipline.
2. **Off-screen combat, first client** — spawn-check + muster + resolve +
   produce for fleet-vs-mob skirmishes only (no raids, no bosses yet), with
   ledger ammo/loot wired and one `ProgramAcoustics.emit` per resolved pass.
   This alone delivers the headline promise: hear the fleet before you see
   it. Prove it works, tune the win-probability math, before adding anything
   else.
3. **Materialization on load (§6)** — apply casualty/terrain backlog when a
   resolved cell's chunk loads, so paper and reality agree from day one of
   this system rather than being bolted on later.
4. **Supply movement client** — off-screen depot stock and convoy paper
   movement (`SUPPLY_NETWORK.md` §5.4), entering at stage 3, including the
   ambush case (ties a threat muster into an otherwise threat-free client).
5. **Bombardment client** — fire missions in unloaded chunks
   (`ARTILLERY_AND_INDIRECT_FIRE.md` §7), entering at stage 4, reusing the
   terrain-erosion materialization path from §6.
6. **Datapad gestimation polish** — sustained-signature detection (rhythmic
   fire → "base defended here" per C7), raid/structured-threat musters, and
   the distance-scaled resolution interval from §8 once there's enough real
   traffic through the resolver to tune against.

Nothing above is a new engine. Steps 4 and 5 are proof that the framework
holds: the same resolver, entered at a different stage, is the entire cost of
adding a new off-screen client.

## Open questions

- **Cell size.** This doc assumes a coarse multi-chunk cell for the region
  index; the exact size (4×4 chunks vs. tied to existing region/scan-record
  granularity in `RiskAssessment`/`ScanRecord`) should match whatever grouping
  those already use, to avoid a second competing notion of "region."
- **Raid integration.** Vanilla raids already carry their own wave/state
  machine; whether the resolver reads that directly or a thin adapter
  translates it into a muster count is a specifics-phase decision, not a
  framework one.
- **Visible aftermath cosmetics** (wreck decals, blood, disturbed terrain from
  a resolved skirmish with no destroyed blocks) are explicitly optional
  polish (§6) — the framework's correctness never depends on them existing.
- **Cross-dimension cells.** `DroneToken` already tracks dimension; the region
  index needs to key on dimension too so a Nether cell and an overworld cell
  at the "same" coordinates never collide — worth confirming against how
  `SUPPLY_NETWORK.md`'s depot list plans to handle dimensions.
