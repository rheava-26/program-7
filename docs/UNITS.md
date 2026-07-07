# Program 7 — Unit Bible

Every Program unit, how it behaves, what beats it, and what it drops.
Units marked ✅ are implemented; the rest are specs for their phase.

## Two tier systems (don't confuse them)

- **Player risk tiers (1–3):** how dangerous the Program thinks *you* are
  (naked & dying = 1, iron-grade = 2, diamond+ = 3). Decides response size.
- **Program tech tiers (1–5, below):** how far the *Program* has escalated.
  Decides what units exist at all. You fight Tier 1 hardware on day 3 and
  Tier 5 hardware in the endgame.

## Escalation: how the Program climbs tiers

Tier N+1 unlocks when the Program has **all three**:

1. **Resources banked** — tiers are paid for out of the ledger; a starved
   base cannot escalate.
2. **Infrastructure built** — each tier needs its buildings standing
   (assembly bank → outposts → integration sites → launch pads).
3. **Pressure** — global threat level thresholds (player kills, base damage,
   scans of high-tier players) push it to *want* the next tier.

This means the player controls the clock: raid the ledger, burn the
buildings, or lie low, and escalation stalls. Killing a main base can knock
the Program back a full tier until re-insertion.

---

## Common doctrine (applies to every unit)

- **View boxes:** sensor units perceive through visible sight cones. Break
  line of sight and you are dark — hiding is always legal.
- **Sound attraction:** gunfire, explosions and machinery noise pull nearby
  units toward the source. Guns trade power for attention.
- **Self-preservation:** any unit below ~30% health, outnumbered, or
  outgunned attempts to disengage toward friendly forces. Suicide-class
  units are the explicit exception.
- **Environment applies:** hostile mobs damage drones; drones fight back or
  route around. Poison does nothing (machines), wither damage does.
- **Everything drops salvage.** No unit is a wasted kill.

---

## TIER 1 — PROBE (landing kit, days 2–7)

The contents of the first pod. Cheap, curious, replaceable.

### Surveyor Drone ✅
| | |
|---|---|
| Role | Recon / intel — the Program's eyes |
| Chassis | Small quad-rotor flyer, red sensor bar |
| HP / Speed | 12 / fast (flying 0.6) |
| Armament | None |
| Salvage | Power bank, transmitter, drone core (35%) |

**Behavior loop:** patrol territory → detect player within 24 blocks (line
of sight) → approach to 5-block standoff → scan for ~3.5 s with accelerating
beeps → file threat profile (risk tier, weapon profile, elytra, deaths) →
psionic interference spike → hard retreat, 20 s scan cooldown.
**Also:** steals unattended item drops (≤3 stacks of cargo) ✅; killing the
thief spills everything back out.
**Counters:** break line of sight mid-scan (interrupts the report), any
ranged weapon (it never fights back), or just let it see nothing valuable.

### Attack Drone ✅
| | |
|---|---|
| Role | Tier-1 response / suicide interceptor |
| Chassis | Quad-rotor, red hazard chevrons |
| HP / Speed | 8 / very fast (flying 0.9) |
| Armament | Contact-fused warhead (power ~2 explosion) |
| Salvage | Gunpowder, explosive warhead (40%), power bank (50%) |

**Behavior loop:** launched by the Director in response to a filed scan
(count = player risk tier, capped by the ledger — 4 iron + 4 gunpowder +
2 redstone each) → beeline to target → arm at ~2.75 blocks → 1.5 s
accelerating fuse → detonate. **Once armed it never defuses.**
**Counters:** shoot it down before it arms (full salvage), kite the armed
drone into terrain, water, or other enemies; shields block the blast.

### Harvester Drone ✅
| | |
|---|---|
| Role | Resource extraction — the economy on wheels |
| Chassis | Wheeled all-terrain box with a cargo hopper, gold stripes |
| HP / Speed | 16 / slow (0.25), high step height |
| Armament | None — panics and flees when hurt |
| Salvage | Iron ingots, power bank, drone core (20%) |

**Behavior loop:** scan ~20 blocks for ore (iron/copper/redstone/coal) or
logs → drive adjacent → grind through the block over ~4 s (visible cracks,
mining noise) → bank the yield as cargo → when full, drive home and deposit
into the Director's ledger → repeat.
**Counters:** it's defenseless — but killing harvesters is *economic*
warfare: every dead hauler is a dispatch the Director can't afford later.
Escorts arrive in Tier 2.

### Probe Core ✅ *(structure)*
The fabricator seed. Iron-pick tier, very tough, glows. Cracking it drops
the heavy salvage (drone cores, power banks, transmitters, iron) and — once
main bases are real — ends this insertion.

---

## TIER 2 — FOOTHOLD (industrial, first fortifications)

The base becomes a *place*: defined territory, defended logistics.

### Light Attack Drone
Small quad-rotor with a single light autogun. HP ~14. Strafing runs at
medium range, retreats to rearm after 2 magazines. The 60% of lockdown
production. **Counter:** shields eat the small-caliber fire; snipe the rotor.

### Wheeled Logistics Drone ("the lovable idiot")
Unarmed hauler that ferries crates between harvest sites and base; lays
slab paths and carves cave ramps as it goes. Gold-striped crates = priority
cargo (iron/coal) and get an escort of 1–2 light attack drones.
**Counter:** ambush the convoy, steal the crate (it's a placeable container
holding real ledger resources).

### Autoturret *(structure)*
Fixed small-caliber emplacement with a visible sweep cone. Engages players
and hostile mobs inside territory. Limited ammo feed — sustained assault can
run a turret dry. **Counter:** approach outside the cone; break its ammo
feeder block.

### Mortar Emplacement *(structure)*
Indirect fire against static targets it cannot see (called in by surveyors).
Slow, loud, telegraphed — shells whistle before landing. **Counter:** keep
moving; kill the spotter and the mortar goes blind.

### Medium Mining Drone
The two ground borers from the pod, upgraded role: tunnel into cave systems
following ore density, deploying torch-like beacons. Creates the mine shafts
the logistics drones service. **Counter:** collapse/flood the shaft; it digs
predictable bores.

---

## TIER 3 — EXPANSION (war economy, counter-adaptation live)

The Program now *reads* you. Weapon-profile counters activate.

### Harasser Drone
Anti-melee counter. Flying gun platform that maintains 15–25 block standoff,
never closes. Sent against sword-profile players. **Counter:** bows/crossbows
(it counters melee, not ranged), enclosed spaces where standoff is impossible.

### Skirmisher Drone
Car-sized ground unit: heavy machine gun + grenade launcher. HP ~60 with
armored plating (firearm-resistant; enchanted weapons bypass). The Tier-3
response backbone. Uses cover, suppresses while explosive drones flank.
**Counter:** component hitboxes — track wheels and gun mount are soft spots;
high ground it can't path to.

### Interceptor Drone
Anti-elytra counter. Water-docked, trident-armed; launches on flight
detection and slams escaping players mid-air. **Counter:** don't fly in a
straight line; bait the launch, then land — it must return to its dock.

### Flying Logistics Drone
Fast, fragile base-internal courier feeding assembly lines from storage.
Lives inside the perimeter. **Counter:** killing them during a raid starves
the assembly bank mid-fight — production stalls.

### Micro-Assembly Swarm *(structure-adjacent)*
The build system: clouds of tiny units that erect fortifications and
assemble drones at the assembly bank. Scale up over time (buildings go up
faster and faster). Individually trivial to kill; the swarm reforms unless
the assembly bank itself is destroyed.

---

## TIER 4 — DOMINION (world integration)

The Program uses *Minecraft* against you.

### Gunship
House-sized rotor craft: dual autoguns + rocket pods + searchlight cone.
The sneak-attack doctrine weapon — once the Program knows you exist, it
waits for one of these, then comes at night. Component hitboxes: rotors
(2), gun mounts, fuel tank (catastrophic). Totem of undying in late Tier 4
(survives one kill). **Counter:** rotor-sniping drops it out of the sky;
fight it under tree cover where the searchlight can't track.

### Heavy Assault Drone
Walking weapons platform, diamond-hardpoint armor. Slow, methodical,
breaches walls. The 20% of lockdown production. **Counter:** it commits to
straight lines — TNT mines, lava moats, iron golem gang-ups.

### Thrall Golem
Village-built iron golem with a control hardpoint on its head. Fights for
the Program. **Counter:** snipe the (visible, glowing) hardpoint to free
the golem — it immediately turns on its handlers.

### Sensor Web *(structure)*
Sculk-sensor pylons wired across territory: silent, no view cone, hears
footsteps/blocks/gunfire. **Counter:** wool-muffled movement, sneaking —
vanilla sculk rules honored.

---

## TIER 5 — ASCENSION (endgame)

### Bore Tunneler
Building-sized subterranean platform that digs highway tunnels between
outposts and *under* player bases (breaching floors during assaults).
Component hitboxes: drill head, drive segments. **Counter:** fight it in
its own tunnel where it can't turn; collapse charges.

### Ballistic Missile Battery *(structure)*
Long-range bombardment of fixed player structures the Program has scouted.
Launches are loud, visible for hundreds of blocks, and interceptable
(shoot the missile, Tier-4+ player AA works). **Counter:** mobile bases,
interception, or killing the spotter network so it has stale coordinates.

### Orbital Relay *(structure)*
The uplink pads that talk to the station: enable re-insertion pods, orbital
scans, and (endless-waves mode) the wave cycle. **Destroying every relay is
a prerequisite for the anti-orbital endgame shot.**

### Void Engine Construct
The psionic endgame guardian: a drone built around a crew-linked void
engine — esoteric weaponry (guardian-beam arrays, psionic interference
projection at weaponized intensity). Drops the **cold fusion engine**.
**Counter:** it is powered by a *mind* — the datapad reveals its link
window; strike during relink.

---

## Salvage → player tech map (quick reference)

| Part | Dropped by | Reverse-engineers into |
|---|---|---|
| Power bank | Everything | Battery tech, energy weapons |
| Drone core | Surveyor, harvester, cores | Player drones, automation |
| Transmitter | Surveyor | Tracking chips, datapad, remote control |
| Gun barrel | Gun-armed units | Firearms, turrets |
| Explosive warhead | Attack drones, skirmishers | Missiles, mining charges |
| Magazine | Gun-armed units | Ammo crafting |
| Cold fusion engine | Void Engine Construct / main base | Player automation endgame |
