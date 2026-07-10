# Program 7 — Artillery & Indirect Fire Framework

> How the Program reaches you where you thought you were safe. This is the
> framework for every unit that lobs, arcs, or drops munitions onto a target
> it can't (or doesn't need to) look at directly — mortars, howitzers, rocket
> artillery, close air support, naval gunfire, and the terror weapons above
> them. It defines the shared **fire-mission** machinery first, then the
> per-type roster that plugs into it. Specifics (each unit's stats, model,
> sounds) come later; this is the skeleton they hang on.

## 1. The core idea

**Indirect fire = a weapon that puts munitions on a target through a separate
sensing step, arriving after a travel delay, with an audible warning.** It is
the Program's answer to the player who fortifies, digs in, or hides behind a
wall — its primary **area-denial and "you are not safe here" tool.** Direct-fire
guns (autoguns, the gunship's belly cannon) shoot what they see; indirect fire
shoots a *place*, found for it by someone else.

That one distinction — **the shooter and the eyes are different things** — is
the whole design. Everything fair and everything terrifying about artillery
falls out of it:

- It can hit you with no line of sight → dread, no safe wall.
- It needs an **observer** to be accurate → a concrete thing to kill.
- It **announces itself** (whistle-in, ranging shots) → fair telegraph, not a
  silent one-shot.
- It is **ammo-hungry and supply-bound** → starving it is real counterplay.
- It is **expensive**, so the Program is **economical with it** → it does not
  waste standard artillery on harmless threats (a lone zombie, a passing
  low-threat player). Shells are reserved for real targets: fortified players,
  massed threats, base assaults. Economy of fire is both a balance lever and a
  characterization — the Program is a cold quartermaster, not trigger-happy.

Also fixed in the fiction: **artillery fires from almost anywhere** (any
emplaced or mobile platform with the range), it is **always visible on the
datapad map** once its fire is detected, and its rounds have **real travel
time** — you hear the distant *bang* of the tube, then the *whizzing* of the
inbound before it lands. Firing position is a place you can go find and kill.

## 2. The fire-mission loop (the heart of it)

Every indirect weapon runs the same five-beat loop. A unit or the Director-side
`FireMissionManager` (see §8) owns a mission through these beats:

1. **Acquisition.** An **observer** with eyes on the target fixes a world
   position. Observers are the recon layer we already have: surveyor, Air UAV,
   scout car, recon helicopter — anything carrying the `AlertState` ramp and
   LOS-gated detection. The firing unit itself can self-observe if it has line
   of sight (a mortar on a ridge). **No observer with current eyes on → the
   guns fire on your LAST KNOWN position.** Breaking contact and moving makes
   you a *worse* target; standing still to shoot back makes you a better one.

2. **Fire mission.** The mission is assigned: target position, unit, round
   count, spread. **Ammunition is debited from the ledger up front.** If the
   ledger can't pay, the mission is scrubbed before a shot is fired — the guns
   simply stay silent. (Same "starve the base" lever as unit dispatch.)

3. **Ranging / adjustment.** The first round(s) land **wide** — a ranging shot
   with a large error radius. As long as an observer keeps eyes on *and the
   target stays roughly put*, fire **walks onto** the target: each subsequent
   shot tightens the error radius. This is the tense part — you hear the first
   one land 20 blocks off, then closer, then closer.

4. **Fire for effect.** Once ranged, the full salvo drops at minimum spread.
   This is the payload beat — the one that levels cover and forces you to move.

5. **Displacement ("shoot and scoot").** Mobile artillery repositions after
   firing to dodge counter-battery. Fixed emplacements can't — which is why
   they're cheaper and why finding one means you can kill it.

**Losing the observer at any point kicks the mission back toward step 3** (fire
goes stale and wide again). This is the elegant counterplay knot: kill the eyes
*or* kill the tubes *or* move *or* cut the ammo — four independent answers.

## 3. Accuracy model (CEP)

Each shot lands at the target plus a random offset inside a **circular error
radius** (borrowing the artillery term *CEP*). The radius is:

```
error = baseSpread[type]
      * rangeFactor      (grows with distance / max range)
      * stalenessFactor  (grows the longer since the observer last had eyes on)
      * movementFactor    (grows if the target has moved since acquisition)
      / rangingProgress   (shrinks 1→N as fire is walked in)
```

- **baseSpread** is the type's signature — a rocket battery is a shotgun, a
  howitzer is a rifle (see §4).
- **rangingProgress** is what steps 3→4 advance; it resets when the observer
  loses eyes on.
- The Program **never needs perfect accuracy** — near-misses that chew up your
  cover and kick up debris are the point. Suppression, not precision.

**Spotting vs range — the accuracy tug-of-war.** A **UAV (or any observer) with
eyes on tightens accuracy** — better spotting shrinks `stalenessFactor` and
feeds `rangingProgress`. But **range always fights back**: `rangeFactor` grows
with distance and is a **hard floor that spotting can never fully cancel**, so
even a perfectly-spotted target at extreme range still eats a spread. Long shots
are inherently loose; that's why the scariest-accurate fire is *close* fire, and
why pushing the guns back out of spotting range is itself a defense. Guided
munitions (§4 missiles) are the exception that pays through the nose to beat the
floor.

**Economy of fire (targeting discipline).** The Director does **not** open a fire
mission on a harmless target — expensive shells aren't spent on a lone mob or a
low-threat player who could be handled by a cheap direct-fire unit. A target has
to clear a **threat/value bar** (fortified, massed, dug-in, or a base assault)
before it's worth a tube. This reads as the Program being deliberate and cold,
and keeps artillery a *response to real pressure*, not ambient noise.

## 4. The artillery roster (types framework)

Every type is one row in the same schema, so adding one is data, not new
systems:

| Field | Meaning |
|---|---|
| **arc** | steep (clears walls, drops vertically) ↔ flat (long reach, needs open lane) |
| **rangeBand** | close / medium / long / strategic — feeds the sound tiers |
| **cadence** | rounds per burst + reload time |
| **baseSpread** | signature CEP — precision vs saturation |
| **munition** | single shell / stick of bombs / rocket ripple / heavy shell / guided missile |
| **platform** | **emplaced** or **mobile** — the defining split, see below |
| **supplyCost** | shells per mission — **all indirect fire is expensive**; heavier munitions cost progressively more |
| **audio** | launch report + inbound whistle profile per range tier |
| **tier** | when it unlocks (ties to UNITS.md tiers) |

### Platform classes — emplaced vs mobile (the defining split)

Every artillery unit is one of two platform classes, and the choice shapes its
whole feel and counterplay:

- **Emplaced (set positions)** — dug-in, static tubes. **Upsides:** can fire as
  a **battery** (several tubes ranging and firing *together* on one target —
  faster ranging, denser fire-for-effect), carries **more ammo on hand** (a
  stocked position sustains a long bombardment), and is **more defendable**
  (bunkered, escorted, part of a fortified outpost). **Downside:** it **can't
  shoot and scoot** — once you find it, it's there to be assaulted and killed.
  These are the siege guns you go take out.

- **Mobile (self-propelled)** — self-propelled mortars, tanks firing indirect,
  warships. **Upsides:** **shoot and scoot** (displaces after firing to dodge
  counter-battery), goes where the front is, harder to pin. **Downsides:**
  **less ammo on hand** (must return to a depot to rearm — ties straight into
  SupplyNetwork), usually fires alone rather than as a massed battery, and is
  more fragile than a bunkered position. These are the guns that hunt you.

The two classes are a **tempo dial**: emplaced batteries are the slow,
overwhelming siege; mobile pieces are the mobile, harassing threat that's always
somewhere new. A fortified Program base fields emplaced batteries; an advancing
one leans on mobile guns and warships.

The roster spans a **caliber spectrum** (mortar → standard artillery → heavy
gun) crossed with the **platform split** (emplaced/mobile) and the **munition
axis** (dumb shell → unguided rocket → guided missile). All the same loop.

- **Mortar** — *Tier 2, already in game (`MortarEmplacementEntity` +
  `MortarShellEntity`).* Steep arc, short–medium range, single arcing shell,
  whistle-in. Comes **emplaced** (the current unit) **and mobile**
  (self-propelled mortar — the reference mobile platform). Cheapest indirect;
  the reference implementation everything else generalizes from.

- **Standard field artillery / howitzer** — *Tier 3.* Medium arc, longer range,
  **heavy shell** with real block-breaking bite (the **wall-breaker** — cracks
  the fort you built), slow cadence. Usually **emplaced as a battery** (several
  tubes ranging together) at a fortified base; a towed variant can reposition
  slowly. The unit that makes "hide behind stone" stop working.

- **Tank / AFV bombardment** — *Tier 3, mobile.* An armored vehicle firing its
  main gun **indirect** (arced onto a spotted position) rather than direct. Less
  ammo on hand than an emplaced battery and fires alone, but it's **mobile and
  armored** — it rolls up with the advance, lobs a few, and moves. Ties the tank
  line into siege work without needing a dedicated artillery unit.

- **Naval bombardment** — *Tier 3, the gunboat/warship (mobile).* The warship's
  main gun as **shore bombardment** when an inland observer feeds it a target.
  Long reach from the water and free to reposition along the coast; this is what
  ties the boats into land sieges instead of leaving them at the shoreline.

- **Unguided rocket artillery / MLRS** — *Tier 3–4.* The **cheap, inaccurate**
  end of the munition axis: a **ripple of many unguided rockets saturating an
  area** at once. Wide baseSpread by design — *area denial, not precision* —
  huge suppression, long reload. Comes emplaced (a massed launcher battery) or
  mobile (a launch vehicle). Its audio is the scariest in the game: a stutter of
  launches, a few seconds of silence, then a wall of impacts arriving together.

- **Guided missiles** — *Tier 4–5.* **Same framework, different travel path and
  cost.** A missile flies a **guided path** and **beats the range accuracy
  floor** (the one munition that stays precise at long range), but it is **far
  more expensive** per shot. The deliberate inversion of unguided rockets:
  rockets are cheap-and-loose, missiles are dear-and-exact. Reserved for
  high-value targets where precision is worth the price (and, later, the
  anti-mod / anti-boss role — a missile answer to something huge from far off).

- **Close air support (CAS) bombing** — *Tier 3–4, airborne.* A fixed-wing or
  the gunship makes a **bombing run**: a stick of bombs walked along a line
  across the target on a diving pass. Telegraphed by the approaching engine note
  and the visible run-in — the one indirect attack you can see coming and sprint
  out of the lane of.

- **Gunship belly autocannon** — *NOT indirect; listed for contrast.* Direct
  fire from altitude (line of sight straight down). It traverses fluidly and
  hoses — no arc, no observer. The gunship's *bombs* (CAS) are indirect; its
  *cannon* is not.

- **Ballistic missile / orbital strike** — *Tier 5 / finale.* The extreme end of
  the guided-missile line: strategic range, near-precision or pure terror
  weapon, rare, enormous supply cost — the thing the anti-orbital endgame is a
  race against. Frameworked here so the ladder is complete; built last.

## 5. Warning & counterplay (the fairness contract)

Indirect fire is only fair if it always announces itself. Non-negotiable:

- **Inbound warning.** Every round has a whistle/scream routed through
  `ProgramAcoustics`, with **lead time scaled by range** — a long-range shell
  screams for longer before it lands than a close mortar. Uses the three-tier
  sound model from `SOUND_DESIGN.md`.
- **Ranging tell.** The walk-in *is* a warning: the first wide round says "they
  have a battery on you, move" before fire-for-effect arrives.
- **Impact.** Area damage + **block destruction gated by blast resistance**
  (mobGriefing-respecting, `HitscanImpact`-style, already the pattern for our
  gun rounds) + crater particles and debris kicked up.
- **The four answers** (any one works): kill the **observer** (fire goes stale),
  kill the **tubes** (mission ends), **move** out of the beaten zone (ranging
  resets), or **cut the ammo** (supply). Overhead cover defeats high-arc fire;
  distance and terrain defeat flat-arc fire. There is always a lever.

## 6. Supply & the ledger

Shells are **the most ammo-hungry thing the Program fields.** Sustained
bombardment drains the ledger far faster than fielding drones, so an
**ammunition plant / forward resupply** becomes the real throttle (this is the
artillery-shaped hole in the logistics framework — see
`RESEARCH_AND_LOGISTICS.md`). Cut the supply line and the guns fall silent even
if the tubes survive. Heavier munitions (howitzer, rocket, missile) cost
progressively more, so the scariest fire is also the most starvable.

## 7. Off-screen / statistical resolution

Bombardment must work in unloaded chunks (the Program shells a base you're not
standing in). Riding on the acoustic-intelligence / abstract-combat design
(`EXPANSION_PLAN.md` C7):

- A mission in an unloaded area **resolves statistically** — the Director rolls
  the shots, records intended impacts, and applies block/entity damage if and
  when that area loads.
- It still **broadcasts the audio** so a distant player hears a **barrage over
  the horizon** and can *gestimate the battery's bearing and origin* on the
  datapad. A far-off war you can hear but not see is exactly the intended
  dread.

## 8. Director-side architecture

A new **`FireMissionManager`**, a sibling of `VirtualFleet` and the dispatch
queue inside `ProgramDirectorState`, owns indirect fire end to end:

- Holds **active missions** (target pos, firing unit/site, observer ref, rounds
  left, current error radius, ranging progress, next-shot tick).
- **Ticks** each mission through the §2 loop: advance ranging while an observer
  has eyes on, spawn the shell entity (loaded chunk) or resolve statistically
  (unloaded), debit ammo, schedule the inbound-whistle audio via
  `ProgramAcoustics` at the right lead time.
- **Observers feed it** target positions off the existing `AlertState` /
  perception layer — no new sensing system, it reuses recon.
- **Gated by tier + supply** exactly like unit dispatch: `tier2Unlocked()`
  fields mortars, higher gates field the heavies, and an empty ledger scrubs
  missions.
- Persists in-flight missions in NBT so a restart mid-barrage resolves rather
  than dropping shells into the void (same discipline as the pod-descent and
  virtualization work).

Shells themselves generalize `MortarShellEntity` into a small family
(arc/spread/payload parameters) rather than a new class per type.

## 9. Datapad / acoustic-intelligence hooks

Artillery is loud and directional, so it's the richest input to the acoustic
datapad:

- **"Sustained heavy impacts, bearing X"** → the datapad triangulates repeated
  impacts over time into an **estimated battery location**, gestimated the same
  way base locations are gestimated from gunfire.
- A **ranging-then-fire-for-effect** signature reads differently from a lone
  mortar or a rocket ripple — the acoustic classifier can *guess the type* from
  cadence and weight (and can be **bluffed**, per the deception layer).
- Counter-battery becomes a player fantasy the datapad enables: hear it, place
  it, go kill it.

## 10. Build order (when we do specifics)

1. Generalize `MortarShellEntity` → a parameterized shell family (arc, spread,
   payload, travel path). *Low risk, unlocks everything below.*
2. Stand up `FireMissionManager` with the **emplaced mortar** as the first
   client (ranging, ammo debit, audio lead time, the threat/value bar) — proves
   the loop on a unit that already exists.
3. Wire **observers** (UAV/recon feed target positions; range floor on accuracy)
   so accuracy responds to spotting *and* distance, and killing the eyes hurts.
4. Add the **platform split**: a **mobile self-propelled mortar** (shoot-and-
   scoot, rearms at a depot) and **battery fire** for emplaced tubes (several
   ranging/firing together) — proves both tempos.
5. Add **standard artillery/howitzer** (wall-breaker), **naval bombardment**
   (warship), and **tank/AFV indirect** — same loop, new rows across the caliber
   and platform axes.
6. Add the **munition axis**: **unguided rocket artillery** (cheap saturation +
   the wall-of-impacts audio) and **guided missiles** (expensive, beats the
   range floor) — the set-piece scares and the precision option.
7. Off-screen statistical resolution + datapad triangulation — the "war over
   the horizon" layer.
8. Ballistic/orbital terror weapon — last, with the finale.

Nothing here is fielded before its tier, and nothing fires without supply. The
guns are only ever as loud as the war economy behind them.
