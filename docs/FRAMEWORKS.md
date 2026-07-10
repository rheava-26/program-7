# Program 7 — Systems Framework (the skeleton before the specifics)

> The owner's directive: **framework everything first, then do specifics.**
> This is the architecture-of-architectures — it names every remaining major
> system, the shared backbones they all plug into, the Director-side owner for
> each, and the order we flesh them out. Detailed design lives in the
> per-system docs; this is the map that keeps them one coherent machine instead
> of a pile of features.

## The one big idea: shared backbones, thin systems

Almost everything left to build is a **thin system riding on a small set of
shared backbones.** Get the backbones right and each new system (artillery,
gunships, research, supply) is mostly data + one manager, not a new engine.
The backbones:

| Backbone | What it is | Already exists as |
|---|---|---|
| **Perception / observer** | LOS-gated detection + the `AlertState` ramp; the "eyes" that find targets and feed indirect fire, recon, and the datapad | `ProgramDroneEntity.AlertState`, `InvestigateDisturbanceGoal`, recon units |
| **Supply / ledger** | The resource economy; *everything* costs the ledger, and an empty ledger stops it — the universal off-switch | `ProgramDirectorState` ledger, `tryConsume`, courier logistics |
| **Acoustic / datapad** | Sound as physics + as intelligence; travel time, muffling, and the radar that lets the player read it all | `ProgramAcoustics`, `DatapadScreen`, `DatapadSnapshotPayload` |
| **Off-screen resolution** | Virtualized units + statistical combat so the war continues in unloaded chunks | `VirtualFleet`, `DroneToken`; statistical layer frameworked in `ABSTRACT_COMBAT.md` |
| **Tier gating** | Capability unlocks by earned threat, hard-capped until the first base kill | `tier2Unlocked()`, `tier3Unlocked()`, `currentTierEstimate()` |
| **The manager pattern** | Each system is a sibling manager inside/around the Director, ticked once per server tick, NBT-persisted | Director dispatch queue, `VirtualFleet`, (planned) `FireMissionManager` |
| **Persistence discipline** | Any in-flight state (a falling pod, a virtual drone, a live fire mission) persists so a restart resolves it, never strands it | pod-descent, virtualization NBT |

**Rule of thumb:** before building any system below, ask *which backbones does
it reuse?* If it needs a new backbone, that's the real work; if it only needs a
new manager + data, it's cheap. Nothing below is allowed to invent its own
private perception, economy, audio, or off-screen path.

## The manager pattern (how every system attaches)

Each major system is a **manager**: a class owning its own state, exposed
through the Director, ticked once per overworld tick, saved/loaded in NBT.
The Director stays the single brain; managers are its organs.

```
ProgramDirectorState (the brain, PersistentState)
├── dispatch queue        — mustering & escalation waves        [exists]
├── VirtualFleet          — off-screen unit tokens              [exists]
├── FireMissionManager    — indirect fire (§ Artillery)         [planned]
├── SupplyNetwork         — infrastructure, depots, supply lines [planned]
├── ResearchTree          — what the Program can build/unlock    [planned]
└── (each) ticks + reads the shared backbones, never duplicates them
```

New systems add an organ; they do not rewrite the brain.

## The remaining major systems

Each entry: **what it is · owner · depends on · doc · framework status.**

### Fire support (artillery & indirect fire)
- **What:** every unit that arcs/lobs/drops munitions onto a target it doesn't
  directly see — mortars → howitzers → rocket artillery → CAS → naval gunfire →
  ballistic/orbital. The Program's area-denial and anti-fortification answer.
- **Owner:** `FireMissionManager` (planned).
- **Depends on:** perception (observers), supply (shells), acoustic (whistle-in
  + triangulation), off-screen (barrage over the horizon), tier gating.
- **Doc:** `ARTILLERY_AND_INDIRECT_FIRE.md` ✅ **frameworked.**
- **Status:** framework done; generalize `MortarShellEntity` first when we build.

### Air doctrine & the apex (gunship)
- **What:** the air layer as a *system*, not one silhouette — fixed-wings fly
  like planes, helicopters pivot on mass, and the **Tier 4 gunship** (dual
  tilting ducted rotors + tail rotor + fluid belly autocannon + CAS bombs) is
  the apex you hear and fear from far off. Air units need helipads/airbases to
  sustain (supply), and the gunship's bombs are *indirect fire* (it plugs into
  `FireMissionManager`) while its cannon is direct.
- **Owner:** entity AI + move controls (mostly exist) + `SupplyNetwork` for
  basing + `FireMissionManager` for its bombs.
- **Depends on:** supply (airbases/fuel), fire support (CAS), acoustic (heard
  from far off), tier gating (Tier 4 = post-first-kill).
- **Doc:** `AIR_DOCTRINE.md` ✅ **frameworked** (air layer as a system + the
  gunship as an explicit composition) + silhouette/tiers in `UNITS.md`.
- **Status:** frameworked — the doctrine (loud + pivoting), basing, and the
  gunship-as-composition are speced; the blockout **model is the owner's art
  plate** and the entity is a specific to build after `FireMissionManager` +
  `SupplyNetwork` exist.

### Supply lines & infrastructure
- **What:** units require infrastructure to *build* and to *sustain* —
  helipads/airbases, repair units, fuel (blaze→high-power, food→biofuel), ammo
  plants. T1 is self-sufficient off the void engine; each tier adds a supply
  dependency; T4 is the peak everyday threat; T5 is exceptional. **Supply lines
  dictate everything above T3, which is TIME-gated.** Roads/railways, defensible
  outposts, forward depots.
- **Owner:** `SupplyNetwork` (planned) — depots, links, per-unit upkeep.
- **Depends on:** ledger (it *is* the economy's spatial layer), perception
  (convoys are visible/killable), off-screen (supply moves in unloaded chunks).
- **Doc:** `SUPPLY_NETWORK.md` ✅ **frameworked** (manager architecture) +
  `RESEARCH_AND_LOGISTICS.md` (layer 2, vision) + `EXPANSION_PLAN.md` C3/C6.
- **Status:** frameworked — the upkeep model, depot/link data model, and tick
  loop are speced; it's the keystone artillery/air both lean on. Ready for
  specifics.

### Research & the psionic building
- **What:** an adaptive research tree at the main base gating *what the Program
  can build*, physically embodied in a **psionic research building** that's
  disruptable — hit it and progression stalls. The player-side mirror is the
  **first-base-kill psionic unlock** (already the payoff hook).
- **Owner:** `ResearchTree` (planned) + a research block entity.
- **Depends on:** ledger (research costs), perception/datapad (the player can
  *see* research progress and target the building), tier gating (research is
  how higher tiers actually open).
- **Doc:** `RESEARCH_TREE.md` ✅ **frameworked** (manager, node schema,
  research building, gating hook) + `RESEARCH_AND_LOGISTICS.md` (layer 1,
  vision) + `DESIGN.md`.
- **Status:** frameworked — the `ResearchTree` manager, the node/adaptive-focus
  model, the disruptable `PsionicResearchBlock`, and the `tier*Unlocked()`
  gating hook are speced. Ready for specifics.

### Sound assets (real audio pass)
- **What:** replace vanilla placeholders with real, **CC0/public-domain,
  redistributable** recordings across the three range tiers per weapon/unit
  genre; add a `CREDITS.txt`. The *engine* (travel time, muffling, tiers,
  deception) already exists and is asset-agnostic.
- **Owner:** `ProgramAcoustics` (exists) — assets slot in without code change.
- **Depends on:** nothing new — it's a content pass on a finished backbone.
- **Doc:** `SOUND_DESIGN.md`.
- **Status:** engine frameworked + shipped; a license-vetted shortlist exists;
  this is an **audition-and-wire content task**, not a framework gap.

### Underground mining / tunnelling
- **What:** the Program mines below the surface, bores into cave systems,
  tunnels toward the player. The single hardest item (pathing + world edits at
  depth).
- **Owner:** harvester/mining AI + a tunnelling planner.
- **Depends on:** perception (scouting priorities), supply (mined resources feed
  the ledger), off-screen (mining in unloaded chunks).
- **Doc:** `EXPANSION_PLAN.md` C1 (marked XL).
- **Status:** vision only; **defer** — highest cost, not on the v1.0 critical
  path.

## Dependency order of the frameworks

Build the *frameworks* (and later the specifics) so each rests on a finished
backbone beneath it:

```
        [ shared backbones — mostly DONE ]
   perception · ledger · acoustic/datapad · off-screen · tiers
                        │
          ┌─────────────┼───────────────┐
          ▼             ▼               ▼
   SupplyNetwork   FireMissionMgr    ResearchTree
   (upkeep/depots)  (indirect fire)   (build gating)
          │             │               │
          └──────┬──────┴───────┬───────┘
                 ▼              ▼
           Air doctrine     Terror weapons
           + Gunship        (ballistic/orbital)
                 │
                 ▼
        Underground / tunnelling (deferred, XL)
```

- **SupplyNetwork is the keystone** — artillery, air, and research all lean on
  it for upkeep/basing/cost. Framework it next in depth (the manager, not just
  the vision).
- **FireMissionManager** can start in parallel on the mortar (it already
  exists) and only needs SupplyNetwork for the *ammo-plant* throttle later.
- **ResearchTree** is what actually opens Tier 4+, so it precedes the gunship
  and terror weapons.
- **The gunship is a composition**, not an engine: (air movement, done) + (CAS
  via FireMissionManager) + (basing via SupplyNetwork) + a model. Don't build it
  until its two managers exist, or it grows its own private versions of them.

## What "frameworked" means before we cut code

A system is ready for specifics when its doc answers all of:
1. **Which backbones does it reuse?** (and it invents no new private ones)
2. **What manager owns it, and what state does that manager hold + persist?**
3. **How does the player perceive and counter it?** (the fairness contract)
4. **What's the cheapest first client** that proves the loop on something that
   already exists? (mortar for fire support, the existing air units for
   doctrine, the probe-core payoff for research)

Artillery (`ARTILLERY_AND_INDIRECT_FIRE.md`) is the worked example of a
fully-frameworked system. **The framework set is now complete** — SupplyNetwork
(`SUPPLY_NETWORK.md`), ResearchTree (`RESEARCH_TREE.md`), air doctrine + the
gunship (`AIR_DOCTRINE.md`), and off-screen statistical combat
(`ABSTRACT_COMBAT.md`) all sit at the same bar. The skeleton holds together;
what remains is **specifics** — models, stats, sounds, and the code that fills
in each manager, dropped onto a frame that already agrees with itself. The
recommended first specific is the cheapest proven client of the keystone:
generalize the existing mortar into the fire-mission loop.
