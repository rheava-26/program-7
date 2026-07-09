# Program 7 — Unit Bible

Every Program unit, how it behaves, what beats it, and what it drops.
Units marked ✅ are implemented; the rest are specs for their phase.

## Two tier systems (don't confuse them)

- **Player risk tiers (1–3):** how dangerous the Program thinks *you* are
  (naked & dying = 1, iron-grade = 2, diamond+ = 3). Decides response size.
- **Program tech tiers (1–5, below):** how far the *Program* has escalated.
  Decides what units exist at all.

## Escalation rules

- **Tier 1** requires nothing more than a **battery and an assembler** — the
  landing kit can always rebuild it.
- **Tier 2** units are heavier versions of Tier 1 vehicles and demand more:
  some require **special landing pads**, some have **upkeep** (resources per
  unit time) or must self-sustain off their own power systems.
- **Tier 3+** each require standing infrastructure, banked resources, and
  threat pressure.
- **Bigger tiers need bigger assemblers.** Each tech tier requires a larger
  assembler class to produce — from the pad-sized Tier 1 assembler up to
  full fabricator complexes for Tier 5. Burning an assembler caps what the
  base can build until it's rebuilt.
- **Support infrastructure gates unit classes** — not just aircraft:
  fixed-wing aircraft need **runways** (UAVs need launch catapults), mechs
  need **loading docks**, warships need **harbors** (which is also where
  they repair). No standing support = the unit class can't deploy, and
  damaged units with nowhere to return degrade. Destroying support
  facilities strands the units that depend on them.
- **HARD CAP: the Program is locked to Tier 3 at maximum until the player
  destroys a main drone base for the first time.** Your first base kill is
  the door to Tiers 4–5 — for both of you.
- **Capability-gated, not calendar-gated:** a tier (and a Tier 2 wave in an
  early fight) appears when the Program has *mined and built enough* to field
  it and when threats — you or otherwise — justify it. Never on a day timer.
- Big bases receive **orbital resupply drops**: very loud, very bright
  streaks from the sky, felt as a psionic spike in the background even from
  far away. If you feel one land, you've located a major base.

## Common doctrine (applies to every unit)

- **View boxes:** sensor units perceive through visible sight cones. Break
  line of sight and you are dark — hiding is always legal.
- **Fragility is real:** small drones die to usually ONE hit from a sword or
  axe. The threat is numbers, speed and initiative — not bullet sponges.
- **Knockback/Punch enchants are anti-drone tech:** hitting a drone with
  knockback throws it off course, scrambles its stabilizers, and can send it
  crashing into terrain.
- **Ram attacks aim:** drones physically angle toward the player and lead
  their target when making ram/slam attacks — you can read the commit.
- **Motor hitboxes:** hitting a drone's motors/rotors is a quick kill even
  when the body would survive.
- **Wrecks, not item sprays:** destroyed drones crash and leave a small
  **wreck block** at the crash site containing their salvage — loot it like
  a container. Big units leave bigger wrecks.
- **Momentum is real:** units cannot come to a full stop instantly, and the
  bigger the unit the more it must commit to its movements. Warships have a
  slow turn rate; a gunship that commits to a strafe run overshoots; a
  ramming drone that misses has to swing back around. Baiting commitment is
  core counterplay.
- **Self-enchanting:** once the Program unlocks enchanting, drones apply
  enchantments to their *own* hardware — plating, weapons, rotors — not
  just to stolen player gear.
- **Sound attraction:** gunfire, explosions and machinery noise pull nearby
  units toward the source. Guns trade power for attention.
- **Self-preservation:** units below ~30% health disengage toward friendly
  forces (suicide-class units excepted).
- **Environment applies:** hostile mobs damage drones; poison does nothing
  (machines), wither damage tears through plating.
- **Typed armor:** hulls resist by damage type (full model in DESIGN.md).
  Bullets bounce off the sloped Tier 2+ plate that arrows punch through;
  crossbow AP and enchanted or psionic weapons bypass armor; Knockback/Punch
  disrupts rather than damages. Every frame lists its resistances in its spec —
  a gun-mod player and a bow player each get a real fight, just a different one.

---

## TIER 1 — PROBE KIT
*Requires: a battery + an assembler. Nothing else.*

| Unit | Role | Status |
|---|---|---|
| **Light recon drone** (Surveyor) | Flying scanner: approaches, profiles the player over ~3.5 s of accelerating beeps, files intel, retreats; steals unattended item drops as cargo | ✅ |
| **Light combat drone** (Attack Drone) | Fast suicide flyer: chases, arms at contact range, detonates; committed once armed | ✅ |
| **Basic mining drone** (Harvester) | Wheeled hauler: mines ore/logs by tag, banks a 12-unit hopper into the ledger; unarmed, panics | ✅ |
| **Logistics drone** | Small flyer ferrying parts between assembler and storage inside the perimeter | ✅ |
| **Wheeled logistics drone** | Ground hauler moving crates between harvest sites and base; lays slab paths; gold stripes = priority cargo | ✅ (paths later) |
| **Basic ground drone** | Tiny armored car with a small gun — perimeter pest control, built to fight off zombies and skeletons, not players | ✅ |
| **Basic autogun** | Fixed small-caliber turret with a visible sweep cone; limited ammo feed | ✅ |

## TIER 2 — FOOTHOLD
*Heavier Tier 1 evolutions. Some need landing pads; some cost upkeep per
unit time or self-sustain off their own power.*

| Unit | Role | Status |
|---|---|---|
| **Medium attack drone** | The workhorse gun flyer: strafing runs, retreats to rearm | ✅ (rearm later) |
| **Long-range sniper drone** | Highly fragile platform with a big, slow-firing weapon — glass cannon standoff | ✅ |
| **Medium mining drone** | Bores into cave systems following ore density; deploys light beacons | ✅ (boring/beacons later) |
| **Laser mining carrier** | Drone carrying crates + multiple mining lasers — mobile strip-mine | — |
| **Medium transport drone** | Carries two crates of resources at once | ✅ |
| **Air UAV drone** | Simple winged propeller drone; requires a **launch catapult** structure; loiters and spots for the base | ✅ |
| **Basic mortar** | Fixed indirect fire at spotted static targets; shells whistle before landing | ✅ |
| **Self-propelled mortar drone** | The mortar, mobile — repositions between volleys | — |
| **Light anti-tank gun/emplacement** | High single-shot damage vs. golems, vehicles, and armored players | — |
| **Unarmed car** | Fast ground scout/courier | ✅ (Scout Car) |
| **Unarmed speedboat** | Water logistics and scouting | — |
| **Underwater mining craft** | Harvests seabed resources (and finds guardians the hard way) | — |
| **Light anti-air drone / ground AA** | Counters elytra players and (later) player drones | ✅ (ground AA) |

## TIER 3 — WAR ECONOMY
*Counter-adaptation live. This is the wall until you kill a main base.*

| Unit | Role | Status |
|---|---|---|
| **Heavy attack drone** | Car-sized, heavily armored: machine guns, grenade launchers, OR quad light missile launchers; costs serious resources; engages from far away | ✅ (burst MGs; launchers later) |
| **Basic IFV** | Infantry-fighting-vehicle analog: carries light drones forward, fire support | ✅ |
| **Mobile battery center** | Rolling power bank — extends operations far from base; killing it browns-out local units | ✅ |
| **Heavy boring/mining drones** | Industrial extraction; tunnel networks between sites | — |
| **Cruise missile launcher** | Long-range strike at scouted static targets; missiles are interceptable | — |
| **Artillery & larger SAMs** | Area bombardment; serious anti-air coverage | — |
| **Small gunboats & corvettes** | Armed water presence | ✅ (gunboat; corvettes later) |
| **Ground-penetrating radar scanner** | Finds YOUR underground base | — |
| **Combat air drone** | Fixed-wing unit strafing ground targets with light guns | — |
| **Recon helicopter** | Fast aerial spotter with a searchlight cone | ✅ |
| **Repair/excavation mech** | Fixes structures and units, digs fortification lines | — |

## TIER 4 — DOMINION
*Unlocked only after the Program loses (and replaces) a main base. Everything
here is **fabricated on-planet from repurposed extraction gear + salvage +
psionic cores** — you can see the mining machine under the armor. Never clean
human military hardware (see DESIGN.md "Fabricated, not unpacked"): the "tank"
is an up-armored ore hauler, the "gunship" a weaponised heavy-lift flier.*

| Unit | Role |
|---|---|
| **Gunship** | The apex terror asset. A house-sized airship on **twin pivoting rotors**, hung with a **giant autocannon that tracks and traverses very fluidly**, plus rockets + searchlight. You are *supposed* to be scared of it: seen and heard from far off, it is the sneak-attack doctrine weapon and the reason to fear the sky. Movement and gun must feel weighty and smooth, never twitchy. |
| **APC** | Armored drone-carrier; deploys squads at contact |
| **Tank** | Direct-fire armor; breaches walls |
| **Attack helicopter** | Fast rotary gun/rocket platform |
| **Medium warship** | Naval gun platform + drone tender |
| **Large cruise missile arrays** | Saturation strikes on fixed positions |
| **Mobile command center** | Forward Director node — killing it lobotomizes local coordination |
| **Self-propelled artillery** | Mobile big guns |
| **MRLS artillery** | Rocket salvos over a wide area |
| **Drone fabricator** | Field factory — produces Tier 1–2 units away from main base |
| **Mining submarine** | Deep-water extraction |
| **Light/medium combat mech** | Walking weapons platforms; terrain-agnostic |
| **Heavy ground fortifications** | Bunker lines, walls, layered turret positions |
| **CAS jet** | Close-air-support: TNT/bomb drops on marked targets |
| **Cargo plane** | Long-haul logistics; airdrops crates/units to outposts |

## TIER 5 — ASCENSION
*The endgame arsenal.*

| Unit | Role |
|---|---|
| **Large warships** | Capital naval units |
| **Massive emplacements** | Superheavy fixed defenses |
| **Superheavy artillery & SAM sites** | Map-scale reach; near-total air denial |
| **Ballistic missile silos** | Strategic strikes; loud, visible, interceptable launches |
| **Supersonic fighter jets** | Air supremacy; nearly uninterceptable without SAMs of your own |
| **Large airships & carriers** | Flying bases launching combat air drones |
| **Massive command centers / mobile bases** | The Program's crown pieces |
| **Orbital satellites** | Scan coverage + resupply targeting; a prerequisite target for the anti-orbital endgame |
| **Heavy combat mechs & tanks** | The final ground escalation |

---

## Minecraft integrations (things drones discover and adopt)

1. **Amethyst** → spyglass optics for long-range spotters, tempered glass
   armor panels.
2. **Glowstone** → enhanced searchlights/illumination projects; dust
   coaters that tag targets with the glowing effect.
3. **Ender pearls & chorus fruit** → emergency dodge blinks and
   long-distance teleport logistics.
4. **Potions** → dropped on players as area denial, or fitted to larger
   ships as defensive dispensers.
5. **TNT** → CAS/jet bomb drops; TNT-carrier drones that hit far harder
   than standard suicide drones.
6. **Blaze powder** → weapon fuel and rocket propellant.

Plus the earlier set: villager trade networks, thrall golems, sculk sensor
webs, spawner farms, totems on heavies, trident interceptors, enchanting.

## Salvage → player tech map (quick reference)

| Part | Dropped by (in wrecks) | Reverse-engineers into |
|---|---|---|
| Power bank | Everything | Battery tech, energy weapons |
| Drone core | Recon, mining units, cores | Player drones, automation |
| Transmitter | Recon/spotter units | Tracking chips, datapad, remote control |
| Gun barrel | Gun-armed units | Firearms, turrets |
| Explosive warhead | Suicide/missile units | Missiles, mining charges |
| Magazine | Gun-armed units | Ammo crafting |
| Cold fusion engine | Main base kill | Player automation endgame + psionic unlock |

Early anti-armor answers the salvage tree should surface so no build is
walled out: **armor-piercing crossbow bolts** and **reinforced/heavy arrows**
(the high-velocity impact line vs. vehicles), an **ender-pearl gun** (blink
rounds, reverse-engineered from drones that learned teleport logistics), and
the **charge laser** — a mining-laser-derived energy rifle that must be
charged, costs expensive amethyst/diamond parts, hits devastatingly hard, and
so forces you to fight the horde for the materials to keep it fed. Bows stay
the cheap always-on baseline; these are the commitments.
