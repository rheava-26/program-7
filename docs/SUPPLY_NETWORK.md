# Program 7 — Supply Network Framework (the keystone)

> `FRAMEWORKS.md` names SupplyNetwork the keystone: artillery, air, and research
> all lean on it for upkeep, basing, and cost. This is its **buildable manager
> architecture** — the upkeep model, the depot/link data model, the tick loop,
> and how it plugs into the Director. The *vision* (why supply lines dictate
> everything, the fuel/ammo fantasy) lives in `RESEARCH_AND_LOGISTICS.md`; this
> is the engineering skeleton that makes it real.

## 1. The one rule

**Units cost to *build* AND to *sustain*.** Building is the one-time ledger
debit we already have (dispatch/assembler). **Upkeep** is the new idea: a unit
above Tier 1 consumes supply *over time* from a nearby source, and when that
source is gone, it **degrades** — it doesn't vanish, it browns out. Tier 1 is
self-sufficient off the void engine; every tier above adds a dependency. **This
is the whole system:** find the thing that feeds the scary unit, kill it, watch
the scary unit falter.

## 2. Upkeep model (per-unit)

Each unit type declares an **upkeep profile**:

```
UpkeepProfile {
  supplyType   // NONE (T1), FUEL, AMMO, or both
  drainPerCycle // how much it consumes each upkeep cycle
  graceCycles   // how long it runs on internal reserves before degrading
  degraded      // what "starved" looks like for this unit (see below)
}
```

- **Cycle**: a coarse tick (e.g. every few seconds), not per-frame — upkeep is
  economy, not physics.
- **Sourcing**: on each cycle a unit tries to draw `drainPerCycle` of its
  `supplyType` from the **nearest depot in range** (see §3). Success → topped
  up. Failure → burn a grace cycle.
- **Degradation** (grace exhausted) is per-class and *readable*, reusing tells
  we already have:
  - Fliers: reduced speed / can't hold altitude (lean on the scramble/inertia
    systems).
  - Gun units: slower cadence / shorter engagement (a brownout, like the
    autogun overheat lockout reads).
  - Artillery: **missions scrub** (no shells = silence — already the ledger
    rule, now spatial).
  - The precedent already in-game: the **mobile battery center's brownout
    debuff** when killed. Generalize that into the upkeep model.
- **Recovery**: re-enter a supplied radius → grace refills, degradation lifts.
  Supply is a leash, not a death sentence.

## 3. The network data model

Three entities, all owned by `SupplyNetwork`:

- **Depot** — a fixed point that *provides* a supply type within a radius:
  - `pos`, `supplyType`, `stock` (drawn from / refilled by the ledger),
    `radius`.
  - Embodied by real blocks so they're targetable: **fuel plant** (blaze →
    high-power fuel; food → biofuel), **ammo plant** (shells), **helipad /
    airbase** (air basing + fuel), **repair depot** (generalized battery
    center). Break the block → the depot is gone → everything it fed starves.
- **SupplyLink** — a logical edge from the **main base / production** to a
  forward **depot**, carrying stock outward. A link is *physically* a convoy
  route (couriers we already have) and/or road/rail. **Interdicting the link**
  (killing convoys, cutting the road) throttles the depot's refill even if the
  depot block survives — the two-layer counterplay: hit the depot *or* its
  supply line.
- **Convoy** — the moving embodiment of a link in loaded chunks: a courier/
  hauler carrying stock from base to depot. Already exists (`CourierUnit`,
  `LogisticsDroneEntity`, haulers); the network just *schedules* them and
  spills their cargo when shot down (already implemented).

## 4. Tier → supply escalation

The dependency deepens with power (from `RESEARCH_AND_LOGISTICS.md`, now as the
upkeep schema):

| Tier | Self-sufficient? | Needs | If starved |
|---|---|---|---|
| **T1** | Yes (void engine) | nothing | unaffected — the baseline threat |
| **T2** | Mostly | light resupply | mild slowdown |
| **T3** | No | fuel + ammo depots | real degradation; **time-gated above here** |
| **T4** | No | airbase + fuel + ammo, actively linked | apex only while fully supplied — the gunship is only terrifying with its logistics intact |
| **T5** | Exceptional | 3000 iron / netherite power bank / copper superconductors | fielded only under extreme provocation; falls apart fast unsupplied |

**Above T3 is TIME-gated, not just threat-gated** — the supply chain has to be
*built and maintained*, which takes real in-world time and space the player can
contest. This is why razing depots and cutting convoys is the mid/late-game
player fantasy.

## 5. The manager

`SupplyNetwork`, a Director organ (sibling of `VirtualFleet` / dispatch /
`FireMissionManager`):

- **State**: the depot list, active links, and a light per-unit upkeep index
  (or units self-report their profile and query the network — see open
  question). All NBT-persisted.
- **Tick** (coarse, per upkeep cycle):
  1. Depots draw refill from the ledger / arriving convoys.
  2. Units in the world sample their nearest in-range depot and top up or burn
     grace; degradation flags update.
  3. Links schedule convoys when a forward depot runs low (reuses courier
     dispatch).
  4. Off-screen (unloaded) depots/links resolve **statistically** — stock moves
     and drains on paper, materializing only when chunks load (rides the
     `VirtualFleet` / abstract-combat backbone).
- **Reads the backbones, owns none**: economy = the ledger; movement = existing
  couriers; off-screen = VirtualFleet; perception = depots/convoys are just
  visible entities/blocks the player already sees.

## 6. Integration points (why it's the keystone)

- **Artillery** (`FireMissionManager`): an ammo plant is the shell throttle —
  no ammo depot in supply → missions scrub. Directly wires §2's "artillery
  starved = silence."
- **Air doctrine / gunship**: airbases/helipads are basing depots; without one
  in range, air units can't sustain sorties. The gunship's terror is
  *conditional on its logistics*, exactly as intended.
- **Research**: the psionic research building is itself a high-value node whose
  output (unlocks) the network's existence justifies building toward.
- **Existing systems**: orbital resupply already refills base stock; the
  battery center already previews the brownout tell; couriers already move
  cargo and spill when killed. SupplyNetwork is mostly *connective tissue over
  parts that exist.*

## 7. Counterplay & datapad

- **Interdiction is the fantasy**: find the fuel plant / ammo plant / airbase /
  convoy route feeding the unit that's hurting you, and cut it. The unit
  degrades on a readable timer, not instantly — you *see* your interdiction
  working.
- **Datapad**: surfaces supply state — "hostile units in this sector are
  running on reserves" after you kill a depot, convoy bearings, depot locations
  once scouted. Turns logistics into legible targets (rides the datapad
  backbone; a supply panel is a later specific).

## 8. Open questions (for specifics)

- **Upkeep indexing**: do units push their profile into a network index, or does
  the network query units in loaded chunks each cycle? (Lean: query in loaded
  chunks + statistical for the rest — avoids a stale index.)
- **Depot radius vs link range**: one global radius per depot type, or scaled by
  tier? (Lean: per depot type.)
- **Grace tuning**: long enough that a brief supply gap isn't punishing, short
  enough that interdiction *feels* effective.

## 9. Build order

1. **UpkeepProfile + degradation** on one class (start with air, since the
   battery-center brownout is the closest precedent) — proves the leash.
2. **Depot block** (fuel plant) + nearest-in-range sourcing — proves "kill the
   depot, starve the unit."
3. **SupplyLink + convoy scheduling** over existing couriers — proves
   interdiction of the *line*, not just the depot.
4. **Ammo plant** → wire into `FireMissionManager` (artillery throttle).
5. **Airbase** → wire into air basing (unlocks the sustained gunship).
6. **Off-screen statistical supply** + datapad supply panel — the legibility
   layer.

Build SupplyNetwork before the gunship and heavy artillery; otherwise each
grows its own private version of upkeep and the keystone never sets.
