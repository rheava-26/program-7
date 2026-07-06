# Program 7 — Design Bible

> A testing ground for an automated probe system designed to mine resources
> while its crew sits in cryogenesis, adapting automatically to environmental
> factors. A station in orbit drops pod packages onto the surface to begin
> extraction — capable of functioning in extreme environments by leveraging
> the **psionic signature** of the sleeping crew, enabling esoteric responses
> to threats that aren't traditionally understood, and powering void engines
> that produce nearly unlimited electricity as long as a mind is connected.

## The Five Pillars

1. **Stressful, not cheap-scary.** Intense like Cracker's Wither Storm, but
   never designed to simply murder the player instantly.
2. **Difficulty WITH rewards.** Every Program unit is killable and drops
   salvage. Beating risky things must feel rewarding — no invincible threats,
   no narrow scripted vulnerability windows.
3. **Advanced AI.** The Program should read as smart — as smart as the player
   or smarter. It runs from fights it can't win and gets better over time.
4. **Inhuman sound design.** Whirring, roaring engines, alarms, frantic
   beeping, psionic interference artifacts on the player's screen.
5. **Works WITH the world.** Vanilla mobs hurt drones. Drones use villagers,
   spawners, enchanting, redstone. Broad mod compatibility.

## The Fantasy / How a Playthrough Opens

- Player plays normally for a day or two.
- A drop pod lands violently several hundred blocks away — huge smoke trail,
  whirring audible from a distance.
- Investigating reveals a large square complex: mini-factories running,
  mining rollers and baby-laser drones extracting resources, obvious scan
  drones roaming, gun turrets emplaced, cargo crates being ferried around.
- Scan drones approach detected players and scan them over several seconds.
- The response depends on the scan (see Risk Tiers).

## Risk Tiers (scan-driven response)

The Program is resource-constrained — it responds *proportionally*, based on
player stats, inventory, and total deaths in the world:

| Tier | Player profile | Program response |
|------|----------------|------------------|
| 1 — Low | No armor, little gear, many deaths | 1 automated explosive attack drone |
| 2 — Medium | Iron armor, real weapons, a few deaths | A couple of drones + base opens small-caliber turret fire. The base avoids getting hurt. |
| 3 — High | Diamond armor and above | Full defense network: skirmisher drones (car-sized, HMGs + grenade launchers) and explosive drones (small, hyper-maneuverable, creeper-style ambushers) |

After killing or chasing off the player, the base enters **lockdown mode**:
mass production of fortifications and drones, favoring high ground, with a
**20 / 20 / 60** production split:
explosive suicide drones / heavy assault drones / light attack drones.

## Counter-Adaptation (weapon profiling)

The Program tracks how each player fights and fields counters:

- **Ranged (bow-heavy):** avoids medium/close skirmishes, swarms you with
  explosive drones to wear you down faster than you can defend.
- **Melee (sword-heavy):** long-range harasser drones.
- **Elytra:** instant-reaction weapons — lasers / particle beams / small
  deployable missiles that hit you mid-escape. Trident interception drones
  stored in water blocks that slam into escaping players.

## The Economy: The Program Mines Like a Player

The Program NEEDS resources the way a player does — copper, redstone, iron —
and seeks them out. It must *actually* mine every ore, chop wood, smelt, and
learn these processes, building its own smelting systems over time. Nothing is
conjured from nowhere.

### Logistics chain

1. **Harvester drones** collect resources and hand off to minor logistical
   drones — wheeled, all-terrain "lovable idiots" that lay slabs and viable
   paths into caves and ferry crates. High-value cargo (iron, coal) gets gold
   striping and armed escorts.
2. **Wheeled logistical drones** deliver containers to base: unloaded at a
   storage deck (organized by secondary drones) or warehoused for low-value
   goods.
3. **Flying secondary logistics** (cheaper, quicker, more vulnerable — fine,
   they live at base) route materials to whichever assembly line needs them.
4. **Micro assembly drones** build everything, scaling up to build faster and
   faster at an "assembly bank" linked to storage.

### Escalation over time

Small light drones → massive bore tunnelers → house-sized gunships → rockets
launched back to orbit → ballistic missiles. They should never remain static,
boring threats — the player develops, and the Program develops alongside.

### World integration (mid/late game)

- Trades with villagers for emeralds; builds remote-controlled iron golems
  (special identifier hardpoints on their heads). Fights pillager outposts and
  patrols to protect "their" villagers.
- Explores the Nether after finding ruined portals; extends the network there.
- Integrates Minecraft tech: diamond hardpoints on dronecraft (component
  hitboxes — snipe a rotor to down a small drone), enchanting, sculk sensors,
  spawner mob farms, totems of undying on large drones, guardian beams,
  enchanted-trident interceptors stored in water, dispenser-style mass arrow
  launchers. Old equipment (bullets) is expensive but powerful — used
  sparingly as they "drop down to your level".

## Fighting Back (the reward loop)

- Drones drop salvage: **power banks, drone cores, transmitters, gun barrels,
  explosive warheads, magazines**.
- Reverse-engineer salvage into your own tech tree: player drones, hand-built
  firearms and rifles, laser drills, thermal vision, scanner systems (your
  drones scout and soft-X-ray ore locations), ground-penetrating radar,
  surface-to-surface missiles, automatic MG turrets.
- The Program steals back: leave gear in the open and a drone may take it.
- Bases are attackable with a real chance of success: controlled withers, a
  micro-drone fleet, gunships, missile volleys, iron golem armies, zombified
  piglin swarms in the Nether. Base turrets handle small threats but the
  Program won't commit serious resources until the base itself takes damage.
  Damage it with plain swings or by mining out specific storage blocks.
- Killing a base yields a **cold fusion engine** — the key to producing your
  own fully automated systems.
- Environment hurts them too: a wither tossed at a base can wipe it, an iron
  golem army is a legitimate mid-game strategy.

## Small Things That Matter

1. Player drones can be dyed and carry banner patterns.
2. Larger drones support different weapon/turret loadouts (heavy assault,
   gunship variants).
3. Mod compat: Guard Villagers, Cracker's Wither Storm; Create blocks
   (including moving contraptions) should be attackable/interactable by drone
   swarms — no deep integration required, just coexistence.
4. Psionic interception: a mid/late-game player tool to control mobs.

## Audio / Horror Direction

The horror is mechanical and procedural, not jump-scare:

- Drones beep like crazy and whirr intensely when they find you.
- Aggressive, personalized tracking pressure based on your weapon profile.
- Alarms, roaring engines — inhuman sounds.

### Psionic interference (proximity warning)

Interference is an *ambient early-warning system*, not a one-off scan effect.
Whenever Program drones are within roughly **3 chunks (~48 blocks)** of the
player, a faint shift and hue distortion appears around the edges of the HUD.
It **grows in intensity with how dangerous the nearby drones are** — one
surveyor is a barely-perceptible flicker; a skirmisher pack closing in washes
the screen edges hard. Players learn to read it: presence, escalation, and
(by paying attention) roughly how bad the situation is before they ever see
or hear the unit.

## VFX Direction

Weapons and events use a **Minecrafty particle language while staying
semi-realistic in composition**. Effects are built from vanilla-style
particles (explosion puffs, smoke, flashes, sparks) arranged realistically —
scale, trails, timing — rather than from custom hyper-real effects.
Reference case: the drop pod entry produces a huge smoke column and a roaring
sound in the sky, but the impact itself still reads as classic Minecraft
explosion particles and smoke.

## Technical Architecture (implementation view)

- **Program Director** (`ProgramDirectorState`, persisted per world): the
  single brain. Holds global threat posture, per-player intel files (risk
  tier, weapon profile, elytra flag, deaths), and later: base registry,
  resource ledger, production queues, era progression.
- **Risk assessment** (`RiskAssessment`): armor + weapon scoring, death-count
  demotion — the tier table above.
- **Units** query the Director for orders; the Director spends a real,
  mined-resource budget to field them. This is what makes "starve the base"
  and "raid their storage" real strategies.
- **Sound events** are registered up-front with vanilla placeholder
  redirects (`sounds.json`), so custom recordings can drop in later without
  code changes.
