# Program 7 — Air Doctrine & the Gunship Apex

> The air layer as a *system*, not one silhouette. This doc frames how the
> Program's aircraft fly, how they're based and fed, and how those two things
> compose — with two existing frameworks already built for other purposes —
> into the **Tier 4 gunship**: the apex you're supposed to hear coming, see
> coming, and be afraid of. It does not spec a new engine. It does not spec
> the gunship's model — that's the owner's art plate; the silhouette already
> lives in `UNITS.md` and is only referenced here. This is the skeleton the
> gunship (and everything smaller that flies) hangs on.

## 1. The core idea

**There is no "flying" subsystem — there is movement (shipped), fire support
(shipped as a framework), and supply (shipped as a framework), and air units
are just the units that happen to use all three at once.** Nothing about being
airborne needs its own perception, economy, audio, or targeting model. The
air layer's entire job is to be the connective roster that rides those three,
plus one doctrine rule that's true of every unit in it: **air units are loud,
and they commit to turns instead of snapping onto a heading.**

That rule is what makes the sky readable. A plane banking in on a strafing
run, a helicopter rotating its whole body to reposition, a gunship's rotor
wash pounding the air a half-kilometer out — none of it is stealthy, all of
it is a **fair, audible, visible telegraph** before the threat arrives. The
Program does not field silent aircraft. Contrast with the ground layer, where
small drones can be genuinely sneaky: the sky is the one place the Program
chooses to be loud on purpose, because a hidden apex predator with wings would
not be a fair fight.

## 2. The air roster (classes, not a new taxonomy)

Every air unit is one row: what it flies like, what it's for, what it needs
to keep flying. No new class of behavior — each is an existing role wearing
an aircraft.

| Class | Existing unit(s) | Role | Fire type | Basing need |
|---|---|---|---|---|
| **Spotter** | `AirUAVEntity` | Loiters, feeds the perception layer (recon for artillery, datapad contacts) | none (observer only) | catapult launch, light/no upkeep |
| **Hunter** | `ReconHelicopterEntity` | Searches, fixes targets with a searchlight cone, hands off to guns/artillery | none (observer) | helipad, fuel upkeep |
| **Gunflyer** | `HeavyAttackDroneEntity` | Car-sized armed flier: direct-fire strafing, engages what it sees | direct (guns) | helipad, fuel + ammo upkeep |
| **Combat air / CAS jet** | *(planned, Tier 3-4)* | Fixed-wing strafing + bombing runs | direct (guns) + indirect (CAS bombs) | runway, fuel + ammo upkeep |
| **Transport / cargo plane** | *(planned, Tier 4)* | Airdrops crates/units to outposts | none | runway, fuel upkeep |
| **Gunship (apex)** | *(planned, Tier 4)* | Direct-fire belly cannon **and** indirect CAS bombing run, from a slow, terrifying, hard-to-miss platform | **both** — the only unit in the game that is simultaneously a direct-fire gun and an indirect-fire fire mission | airbase, fuel + ammo upkeep, the most demanding profile in the roster |

Spotter and hunter are already shipped and already prove the doctrine rule
(§3) and the perception hookup (§5) work. Gunflyer already proves an armed
flier degrading toward a rearm cycle. The gunship doesn't invent a new row of
this table — it's the row that finally needs *every* column filled in at
once, which is exactly why it's built last (§7).

## 3. How they fly (the doctrine rule)

Two move controls already ship and they are the entire air-movement engine.
Nothing in this doc adds a third:

- **`FixedWingMoveControl`** — planes. Very slow turn rate, a hard minimum
  airspeed (can't hover, can't stop), and visible banking into turns. A
  fixed-wing that wants to hit you has to commit to a long final approach and
  cannot correct sharply — baiting the overshoot is real counterplay, same as
  the momentum doctrine in `UNITS.md` ("a gunship that commits to a strafe run
  overshoots").
- **`InertialFlightMoveControl`** — mass-based acceleration and turn caps.
  This is what makes helicopters (and the gunship) **pivot their whole body
  to change heading** rather than strafing sideways like a video-game drone.
  The heavier the unit, the more deliberate the pivot. A recon helicopter
  swings its nose before it moves; the gunship, being the heaviest thing that
  flies, telegraphs a heading change like a ship coming about.

**The rule this doc adds on top of both:** every air unit runs **loud,
continuous ambience keyed to its own engine/rotor signature**, routed through
`ProgramAcoustics` exactly like the ground and artillery layers (see
`ARTILLERY_AND_INDIRECT_FIRE.md` §5, §9). A UAV's electric whine, a recon
helicopter's blade chop, and the gunship's dual-rotor downwash are three
different sounds so the datapad's acoustic classifier can already guess
*what's inbound* before it's in view — the same "guess the type from cadence
and weight" trick artillery does with ranging shots. **No air unit is ever
silent-approach.** If a design ever wants a sneaky flier, it is not this
roster; it's a different, explicitly-called-out exception, not a variant of
the doctrine.

## 4. Basing & sustainment (the leash)

Air units are the **most demanding client** of `SUPPLY_NETWORK.md` in the
whole game, and that's deliberate — the sky is the thing you can't otherwise
touch, so its cost is paid entirely in logistics instead of in reachability.

- **Depot type:** helipad / airbase, per `SUPPLY_NETWORK.md` §3 — a real,
  targetable block. Catapults (`LaunchCatapultBlock`) already give UAVs a
  minimal version of this (a structure gates the unit class, per `UNITS.md`
  "Support infrastructure gates unit classes"). Helipads and airbases
  generalize that into a full depot: **basing** (a place the unit returns to
  and launches from) plus **fuel** (an upkeep draw, per `SUPPLY_NETWORK.md`
  §2's `UpkeepProfile`).
- **No base in range → no sustained presence.** A spotter or hunter with no
  helipad nearby runs on `graceCycles` and then degrades exactly like any
  other upkeep-bound unit: reduced speed, can't hold altitude, eventually
  grounds. This is not a special air rule; it's `SUPPLY_NETWORK.md` §2 applied
  to a flier, which the framework already names as the first client to prove
  the upkeep leash on (§9, build order item 1).
- **The counter this buys the player:** you cannot shoot down the sky. You can
  **starve it.** Find the airbase, kill it (or cut its `SupplyLink` convoy
  route per `SUPPLY_NETWORK.md` §3), and every aircraft that depended on it
  browns out on a readable timer. This is true at every tier, and it's *the*
  reason a gunship is "only terrifying with its logistics intact"
  (`SUPPLY_NETWORK.md` §4, T4 row) rather than an unkillable roaming threat.
- **Escalation ties to `UNITS.md`'s tier table:** T2 air (UAV, catapult) is
  light upkeep; T3 (recon helicopter, gunflyer) needs real fuel/ammo depots;
  T4 (gunship, CAS jet, cargo plane) needs an **airbase, actively linked** —
  the heaviest supply requirement in the game, matching its role as the
  apex.

## 5. The gunship as a composition (the centerpiece)

**The gunship is not a new engine. It is four things we already have (or
have already frameworked), stacked:**

```
Gunship =
    (air movement)        InertialFlightMoveControl, heaviest-mass profile
  + (CAS bombing run)      indirect fire — a FireMissionManager client
  + (belly autocannon)     direct fire — fluid-traverse hitscan, no observer
  + (basing/fuel/ammo)     SupplyNetwork airbase depot, the T4 upkeep row
  + (a model)              owner's art plate — silhouette already in UNITS.md
```

Take any one piece away and it's a different, already-built thing: movement
alone is a recon helicopter; movement + direct fire alone is a gunflyer;
movement + basing alone is a cargo plane. The gunship is what you get when a
single airframe is asked to carry **every column of the air roster table at
once** — which is exactly why nothing about it is a new system, and exactly
why it's expensive and rare.

### 5a. The bombs are indirect fire

The gunship's bombing run is a **CAS fire mission**, full stop — it plugs
directly into `ARTILLERY_AND_INDIRECT_FIRE.md`'s existing five-beat loop
(§2 of that doc) via `FireMissionManager`:

1. **Acquisition** — the gunship (or any observer feeding it) fixes a target
   position. It can self-observe if it has LOS, same as a mortar on a ridge.
2. **Fire mission** — a stick of bombs is assigned; ammo debits from the
   ledger up front, same as any other mission.
3. **Ranging** — largely skipped in practice (a diving run is close-range and
   fast), but the *telegraph* replaces ranging shots: the approaching engine
   note and the visible run-in are the warning, called out explicitly in the
   artillery doc's roster (§4, "Close air support (CAS) bombing... the one
   indirect attack you can see coming and sprint out of the lane of").
4. **Fire for effect** — the stick walks along a line across the target,
   same block-erosion/saturation model as any other munition (§5a of the
   artillery doc): loud and violent per-bomb, but sustained bombing is what
   actually grinds terrain down.
5. **Displacement** — the gunship *is* mobile artillery in this beat; it
   banks off and comes back around for another pass rather than loitering
   over the target.

No new munition system. `FireMissionManager` gets a new *row* (gunship CAS),
not a new mission type.

### 5b. The autocannon is direct fire

The belly-mounted autocannon (per `UNITS.md`'s silhouette: fluid-traversing,
tracks smoothly) is explicitly the artillery doc's contrast case (§4): **"NOT
indirect... direct fire from altitude (line of sight straight down). It
traverses fluidly and hoses — no arc, no observer."** It needs nothing from
`FireMissionManager` at all — it's a hitscan gun on a turret, the same family
as a ground autogun or `HeavyAttackDroneEntity`'s guns, just mounted on a
slower, heavier, higher platform. The turret's fluid traverse is a stat
(rotation speed) on an existing turret-aim component, not new code.

### 5c. Basing makes it conditional, not permanent

The gunship carries the **heaviest upkeep profile in the game** (§4 above,
and `SUPPLY_NETWORK.md` §4's T4 row): airbase + fuel + ammo, all actively
linked. It is never a standing, unconditional threat — it's a threat that
exists *because* a base built and defended the logistics to field it. Ground
the airbase and the gunship that flies from it degrades on the same readable
timer as every other starved unit. The terror is real; the terror is also
rentable, and you can evict the landlord.

### 5d. The model

The fourth term is **owner's art plate**: the fat Osprey-derived fuselage,
twin ducted rotors on tilting stub pylons, tail rotor, belly turret — already
fully described in `UNITS.md`'s Tier 4 table and not re-specified here. This
doc treats the model as a black box that the other three systems attach to;
nothing above depends on its geometry, only on it existing as an
`InertialFlightMoveControl` entity with a belly-turret mount point and a bomb
hardpoint.

## 6. Gunship doctrine (how it behaves, not how it's built)

- **Tier 4, post-first-base-kill.** Gated by `tier3Unlocked()`'s successor —
  the Tier 4 unlock fields only after `firstBaseKilled` per `UNITS.md`'s hard
  cap and `ProgramDirectorState`'s tier machinery. It cannot appear before you
  have already won once.
- **Set-piece, not ambient.** It is not part of routine waves. It is
  dispatched deliberately against a target that has *earned* it — a
  fortified base, a player who has already destroyed Program assets, a siege
  in progress — mirroring the artillery doc's "economy of fire" targeting
  discipline (§3 of that doc): expensive assets aren't spent on harmless
  targets.
- **Delayed, deliberate approach.** A known-dangerous player does not get
  ambushed by a gunship appearing overhead. It is dispatched from its
  airbase, flies the distance at its heavy `InertialFlightMoveControl` pace,
  and is **audible from far off** before it's visible — the acoustic
  travel-time model artillery already established (§9 of the artillery doc:
  "a barrage over the horizon") applied to an aircraft instead of a shell.
  You get real warning time to prepare, retreat, or go hunt its airbase
  first.
- **Weighty, never twitchy.** Per `UNITS.md`'s own line on the unit: movement
  and gun both read as heavy and smooth. This is an `InertialFlightMoveControl`
  tuning question (high mass, high turn-commit) — not a new behavior, just
  the existing control at its heaviest setting.

## 7. Perception & counterplay (the fairness contract)

Every answer here already exists somewhere else in the framework; the
gunship doesn't need new ones invented, only assembled:

- **Hear it first.** `ProgramAcoustics` broadcasts the dual-rotor signature at
  long range with real travel time, same mechanism as artillery's inbound
  whistle (`ARTILLERY_AND_INDIRECT_FIRE.md` §5). The datapad's acoustic
  classifier flags it distinctly from a recon helicopter's lighter blade chop
  — you should be able to tell "gunship inbound" from "just a scout" before
  either is in view.
- **See it on the datapad.** Once detected, it's a tracked contact like any
  other unit (`DatapadScreen`), and its bombing runs are visible on approach
  (the CAS "you can see the lane and sprint out of it" contract, §5a above).
- **Fight its soft points, not its hit-points bar.**
  - **Rotors/engines are a quick kill** even on a body that would otherwise
    survive — `UNITS.md`'s common doctrine ("Motor hitboxes: hitting a
    drone's motors/rotors is a quick kill even when the body would survive")
    applies at gunship scale, just behind thicker armor.
  - **Knockback/Punch scrambles it** — the same anti-drone tech that throws a
    small flier off course applies to the gunship's stabilizers too, per the
    common doctrine; at its mass this reads as a stagger/shove rather than a
    crash, but it interrupts a bombing run or gun pass.
  - **Kill the airbase.** The supply answer (§4, §5c): starve its fuel/ammo
    and it browns out — slower turret traverse, grounded sorties, an eventual
    return-to-base it can't complete. This is the *reliable* counter, the one
    that doesn't require winning a dogfight.
  - **Break contact during a bombing run.** Per the CAS entry in the
    artillery doc, the run is telegraphed and has a lane — moving out of it
    is a complete answer to that beat, same as dodging any other indirect
    fire.
- **No single counter is mandatory.** Exactly like artillery's "four answers"
  (§5 of that doc), the gunship has independent levers — rotors, knockback,
  airbase, positioning — and any one of them degrades it a different way.

## 8. Which backbones it reuses (the 4-question bar)

Per `FRAMEWORKS.md`'s definition of "frameworked":

1. **Which backbones does it reuse?** Perception (`AlertState`/observers feed
   its targets, same as artillery), acoustic/datapad (`ProgramAcoustics`,
   `DatapadScreen`), supply/ledger (`SupplyNetwork` airbase depot,
   `UpkeepProfile`), tier gating (`tier3Unlocked()` → T4 successor gated on
   `firstBaseKilled`), off-screen resolution (a gunship dispatched toward an
   unloaded sector resolves the same way a fire mission or a virtualized
   fleet unit does). It invents none of these.
2. **What manager owns it, and what state does it hold?** No new manager.
   The gunship entity is dispatched like any air unit (Director dispatch
   queue), its bombing runs are owned by `FireMissionManager` as a CAS-type
   mission, and its basing/upkeep is owned by `SupplyNetwork`. The entity
   itself holds only what any aircraft holds: position, move-control state,
   current mission reference, upkeep profile reference.
3. **How does the player perceive and counter it?** §7 above — hear it,
   track it, and hit one of four independent soft points.
4. **What's the cheapest first client that proves the loop on something that
   already exists?** Not the gunship itself. The cheapest proof is: (a) put
   `HeavyAttackDroneEntity` or `ReconHelicopterEntity` on an `UpkeepProfile`
   against a helipad depot (proves air basing, `SUPPLY_NETWORK.md` build
   order item 1 already names this), and (b) give an existing fixed-wing or
   the combat air drone a CAS bombing run through `FireMissionManager`
   (proves air-as-a-FireMissionManager-client). The gunship is what's left
   once both proofs exist — see §9.

## 9. Build order

Air doctrine is deliberately **not** a single build item — it's a sequence
that front-loads the cheap, general pieces and leaves the gunship as the
final assembly step, per `FRAMEWORKS.md`'s dependency graph
(`SupplyNetwork` + `FireMissionManager` → Air doctrine + Gunship).

1. **The loud-and-pivoting rule, formalized.** Audit `AirUAVEntity`,
   `ReconHelicopterEntity`, and `HeavyAttackDroneEntity` against §3: distinct
   per-unit acoustic signature through `ProgramAcoustics`, confirm
   `FixedWingMoveControl`/`InertialFlightMoveControl` are the only two move
   paths in use. Cheap — mostly wiring existing entities to existing audio.
2. **Air basing on an existing unit.** Put one shipped flier (recon
   helicopter is the natural pick — heli, not glider, matches the eventual
   gunship's control scheme) on a helipad `UpkeepProfile` per
   `SUPPLY_NETWORK.md` build order item 1. Proves "kill the depot, ground the
   air" end to end before the gunship exists to need it.
3. **CAS as a `FireMissionManager` client.** Give the planned combat air
   drone / CAS jet (`UNITS.md` Tier 3-4 roster) a bombing-run mission type in
   `FireMissionManager` — the visible-lane, no-ranging CAS variant described
   in §5a. Proves air-as-indirect-fire on a cheaper airframe first.
4. **Airbase depot (full).** Extend the helipad proof (step 2) into the full
   `SUPPLY_NETWORK.md` airbase depot — fuel + ammo, actively linked — per
   that doc's build order item 5.
5. **The gunship composition.** Only now: the Tier 4 entity, gated on
   `firstBaseKilled`, wired to (2)'s basing, (3)'s CAS mission type, a direct
   belly-turret hitscan gun, and `InertialFlightMoveControl` tuned to its
   (owner-authored) mass. Nothing here is new machinery — it's the assembly
   step FRAMEWORKS.md calls out explicitly: *"don't build it until its two
   managers exist, or it grows its own private versions of them."*
6. **Doctrine tuning pass.** Delayed-approach timing, economy-of-fire
   targeting thresholds (§6), and the soft-point balance (§7) — numbers, not
   systems, and the only part of this build order that's genuinely
   gunship-specific.

Everything before step 5 is useful on its own (basing benefits every flier;
CAS benefits any bomber). The gunship is the point where all of it converges,
which is exactly why it ships last.

## 10. Open questions (for specifics)

- **Bomb hardpoint vs. turret mount** on the model (owner's art plate,
  step 5) — how many bomb-run passes per sortie before it needs to return to
  rearm, and whether that's a fixed magazine or continuous ammo draw against
  the airbase depot.
- **CAS mission variant tuning**: does the gunship's bombing run get its own
  `baseSpread`/`cadence` row in the artillery roster table (§4 of that doc),
  or does it inherit the fixed-wing CAS row wholesale? (Lean: its own row —
  it's a heavier, slower platform than a jet, and the doc's roster schema
  already expects one row per type.)
- **Escort doctrine**: does a gunship ever fly with an escort (gunflyers,
  ground AA suppression) as part of the same dispatch, or always alone as a
  lone apex asset? Affects whether "kill the gunship" fights are 1-on-1 or
  combined-arms.
- **Grace tuning for T4 upkeep**: `SUPPLY_NETWORK.md` §8 already flags this
  generally; the gunship, as the heaviest consumer, is the sharpest test of
  "long enough not to be punishing, short enough that interdiction feels
  effective."
