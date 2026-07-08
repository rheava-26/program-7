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
5. **Works WITH the world — invasion, not overlay.** Vanilla mobs hurt
   drones. Drones use villagers, spawners, enchanting, redstone. Maximum mod
   compatibility: drones should *react to* modded enemies and work around
   them rather than ignoring them. It should feel like an invasion run by an
   adapting AI.

## The Fantasy / How a Playthrough Opens

- Player plays normally for a day or two.
- A drop pod lands violently several hundred blocks away — huge smoke column,
  roaring audible from the sky (VFX: vanilla explosion particles + smoke on
  impact; semi-realistic composition, Minecrafty ingredients).
- Investigating reveals a large square complex: mini-factories running,
  mining rollers and baby-laser drones extracting resources, obvious scan
  drones roaming, gun turrets emplaced, cargo crates being ferried around.
- Scan drones approach detected players and scan them over several seconds.
- The response depends on the scan (see Risk Tiers).

## The Main Pod, Territory & Outposts

The main drop pod is a **big fabricator** plus a starter kit:

- A small complement of drones, including a couple of medium ground-based
  mining drones.
- A stockpile of key resources: iron, copper, redstone, coal, gunpowder, etc.
- Autoturrets and **mortars** for base defense.

From there the Program **outlines a territory** and expands with **mini
outposts**. It reads terrain like a strategist:

- Uses natural features — mines into existing caves instead of always boring
  fresh shafts.
- Builds fortifications in naturally defensible positions: mountaintops,
  river crossings, chokepoints.

### Re-insertion (when a main outpost dies)

Destroying a main outpost is a real victory, but the orbital station answers.
After a substantial waiting period, a new pod comes down — always within a
few thousand blocks — and its placement is a read on how the war is going:

- **Player obliterated them decisively** → lands far away, likely with more
  resources committed.
- **Player is weak** → lands close to surviving network infrastructure so it
  can relink to the existing network.
- **Drones scouted richer ground** → lands on the better position.

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

## Detection, Stealth & Coexistence

You can *sort of* keep the peace with the Program — by running away, not
engaging, and hiding. But:

- **Anomaly detection:** drones notice blocks that shouldn't exist in an
  area (structures, torches, paths) and go searching for whoever made them.
- **The gunship doctrine:** once the Program knows a player exists, its
  preferred move is to quietly wait until it can field a gunship, then launch
  a sneak attack to wipe the player out quickly.
- Hiding works on **line of sight** — drones have visible view boxes /
  sight cones (think the search drones in *The Incredibles*); break the cone
  and you're dark.
- Surviving past **~7 in-game days** with the Program active earns an
  advancement.
- **Heat is a two-way dial, not a ratchet.** Aggression, anomalies, and kills
  raise the Program's heat toward you; sustained non-engagement — or *beating
  its forces into submission* until it judges you too costly — brings heat back
  down. Drive heat to the floor and the Program backs off into an uneasy
  neutrality for a while (all of it visible on the datapad). The ceasefire
  protocol is simply heat bottoming out by force rather than by hiding.

## Counter-Adaptation (weapon profiling)

The Program tracks how each player fights and fields counters:

- **Ranged (bow-heavy):** avoids medium/close skirmishes, swarms you with
  explosive drones to wear you down faster than you can defend.
- **Melee (sword-heavy):** long-range harasser drones.
- **Elytra:** instant-reaction weapons — lasers / particle beams / small
  deployable missiles that hit you mid-escape. Trident interception drones
  stored in water blocks that slam into escaping players.

Beyond weapon profiling, groups pick a **battlefield doctrine** and commit to
it — and because the datapad exposes the doctrine token, the player can read it
and counter. Examples the AI should be able to run: *flush to cover* (herd you
into a forest, snipe from the treeline, flee whoever you chase); *bombard &
suppress* (explosive harass on you and your fortifications from standoff);
*interdict escape* (cut your retreat, water/elytra interceptors); *overwhelm*
(mass suicide swarm). Reading "they're pushing me into the trees to potshot me"
→ *bring the bow*; "they're bombarding my walls" → *charge in and knock them
into orbit*. That read-and-counter loop is the strategy layer of the mod.

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

**Escalation is capability-driven, never calendar-driven.** The Program tiers
up when it has *mined and learned enough* to build the next class — not on a
day timer. It also adapts to what **it** considers threats (vanilla mobs,
other players, modded enemies, a rampaging wither storm), not only to you; the
invasion has its own life and would keep escalating on an empty-but-hostile
world. Drones are not player-reliant — that is the point of the mod.

**Fabricated, not unpacked (tone at scale).** The pod barely brings anything —
everything heavier is *built on-planet from mined material, salvaged Minecraft
blocks, and psionic cores.* So heavy units must never read as human military
hardware. A "tank" is an up-armored ore hauler; a "gunship" is a heavy-lift
cargo flier with weapons welded to its lift arms; a siege engine is a
repurposed bore tunneler. You can always see the extraction machine under the
armor. This is what keeps a house-sized war engine reading as *an industrial
probe that entrenched for months* rather than an invading army — the rule that
lets the late-game arsenal grow without breaking the probe fantasy.

### Minecraft item integrations

Drones *discover* the world's materials and fold them into their arsenal
(full list in UNITS.md): amethyst → spotter optics and tempered glass;
glowstone → searchlights and glowing-tag dust coaters; ender pearls /
chorus fruit → dodge blinks and teleport logistics; potions → dropped on
players or mounted as ship defenses; TNT → CAS bomb drops and TNT-carrier
drones (much stronger than standard suicide attacks); blaze powder →
weapon and rocket fuel.

### World integration (mid/late game)

- Trades with villagers for emeralds; builds remote-controlled iron golems
  (special identifier hardpoints on their heads). Fights pillager outposts and
  patrols to protect "their" villagers.
- Explores the Nether after finding ruined portals; extends the network there.
- Integrates Minecraft tech: diamond hardpoints on dronecraft (component
  hitboxes — snipe a rotor to down a small drone), enchanting (including
  **self-enchanting their own plating and weapons**), sculk sensors,
  spawner mob farms, totems of undying on large drones, guardian beams,
  enchanted-trident interceptors stored in water, dispenser-style mass arrow
  launchers. Old equipment (bullets) is expensive but powerful — used
  sparingly as they "drop down to your level".

**Adaptation set pieces (the "it's *thinking*" moments).** The invasion should
occasionally do something that reads as genuinely clever, not scripted:
firing a cruise missile at a rampaging wither storm because it registers as a
bigger threat than you; **bribing villagers to stop trading with you** (out-
buying your economy) or fighting to protect "their" villagers; working out
that piglins take gold and trading it for **ender pearls** to fuel teleport
logistics — the same pearls it turns into an **ender-pearl gun** (blink
rounds) you can later salvage. These are rare, expensive, and reactive — the
payoff for the "advanced AI" pillar, surfaced to the player through the
datapad's development tracker.

## Drone Combat Feel

- **Small drones are one-hit kills** to a sword or axe — the horror is
  numbers and initiative, never HP sponges.
- **Knockback/Punch enchantments throw drones off course**, scramble their
  flight, and can crash them outright.
- Drones **physically angle toward the player and lead their aim** during
  ram attacks — the commit is readable and dodgeable.
- **Motor/rotor hitboxes** give quick kills on bigger frames.
- Dead drones **crash into small wreck blocks** holding their salvage —
  looted like containers instead of item sprays on the ground.
- **Momentum:** no unit stops on a dime; large units (warships especially)
  turn slowly and must commit to maneuvers the player can read and punish.
- The Program is **capped at Tier 3 until the player destroys a main base**
  for the first time (see UNITS.md for the full five-tier arsenal).
- **Orbital resupply** drops at large bases are loud, bright, and felt as a
  background psionic spike — a beacon telling you where the big base is.

## Fighting Back (the reward loop)

- Drones drop salvage: **power banks, drone cores, transmitters, gun barrels,
  explosive warheads, magazines**.
- Reverse-engineer salvage into your own tech tree: player drones, hand-built
  firearms and rifles, laser drills, thermal vision, scanner systems (your
  drones scout and soft-X-ray ore locations), ground-penetrating radar,
  surface-to-surface missiles, automatic MG turrets.
- **Electronics / intel gear:** tracking chips (mark items or targets),
  **glowstone illuminators** (tracking chips that apply the glowing effect to
  mobs), tracking displays, and the **datapad** — the core intel artifact and
  the mod's signature tool (its own section below).
- The Program steals back: leave gear in the open and a drone may take it.
- Bases are attackable with a real chance of success: controlled withers, a
  micro-drone fleet, gunships, missile volleys, iron golem armies, zombified
  piglin swarms in the Nether. Base turrets handle small threats but the
  Program won't commit serious resources until the base itself takes damage.
  Damage it with plain swings or by mining out specific storage blocks.
- Killing a main base yields a **cold fusion engine** — the key to producing
  your own fully automated systems — and its **data files** (see Lore).
- **The first main-base kill is the hinge of the whole game.** It *severs the
  psionic relay* that chained the base's drones to orbital command, and that
  break pays out three ways at once: a huge windfall of banked resources spills
  out (you can finally build past light vehicles), you seize part of that
  psionic power as your own **psionic unlock**, and the surviving fleet's heavy
  fabrication degrades. This is the moment both sides "go total" — the door to
  Tier 4–5 opens for the Program *and* for you.
- Environment hurts them too: a wither tossed at a base can wipe it, an iron
  golem army is a legitimate mid-game strategy.

### Weapon balance philosophy

- **Bows** stay relevant: far cheaper ammo, faster to fire than the more
  advanced guns; firearms require quite a bit of build-up. Bows are accurate
  at range; Punch and Flame are already strong enough that they need no help.
- **Autodrones are armored against firearms** — especially weak ones.
  **Enchanted weapons are great at bypassing drone armor.**
- Early-game weapons salvaged from basic light attack drones sit around the
  level of a Power II–III bow, with better range.
- **Guns are loud.** Gunfire attracts the drone network. Every shot is a
  trade.
- **Cool tools are rewards, not crutches.** Every player gadget must *feel
  good to use on its own terms* — a ground-penetrating scanner that actually
  finds caves and ore, not a "so you don't die to the one thing we added."
  The charge laser, datapad, tracking chips, and player drones all pull their
  weight as tools you'd want even without the invasion.

### Damage & armor is typed (the compatibility thesis)

Program armor resists *by damage type*, so every player build has a lane and a
wall — and a player running a realistic gun mod faces the same *shape* of
challenge as a vanilla player, just with different texture. This is what makes
the mod-compat pillar honest instead of "gun mod = easy/hard mode."

- **Ballistic / kinetic** (bullets — gun mods, and the drones' own MGs): Tier
  2+ hulls are sloped and plated *specifically* against this. Ball ammo chips
  slowly at armor a vanilla arrow punches through. Gun players adapt with
  armor-piercing rounds or by switching tools — never locked out, just can't
  faceroll.
- **High-velocity impact** (arrows, tridents — the "sticks that slam into
  them"): hulls are NOT optimized for it; impact transfers stagger and
  knockback, scrambles flight, cracks exposed rotors. Why the bow stays viable
  against even heavy vehicles when raw bullets stall out.
- **Piercing** (crossbow + Piercing / AP bolts): the dedicated anti-vehicle
  line — punches the heavy plate bullets bounce off.
- **Explosive** (TNT, warheads, salvaged missiles): area and fortification
  breach — what you bring *to* a base, not to a dogfight.
- **Enchanted / psionic** (Sharpness, the charge laser, post-base psionic
  weapons): bypasses plating outright — the premium answer, expensive, the
  reward for engaging the horde for parts.
- **Knockback / Punch:** not damage but *disruption* — throws units off
  course, crashes small frames outright.

No single tool answers everything. Bows are cheap and always-relevant, guns a
loud commitment, crossbow AP the anti-armor specialist, enchants and psionic
weapons the premium bypass. Each unit lists its resistances in its spec.

## The Datapad — Your Window Into the Program

The single most important player tool and the mod's visual signature (its
answer to Cracker's Wither Storm amulet). It is craftable **early** from basic
salvaged electronics and **upgrades with more salvage** into a full
intelligence suite. It is what makes you a *partisan reading a machine enemy*
rather than prey hiding in a hole — and it is the in-fiction UI for everything
the Program does that would otherwise be invisible:

- **Proximity blare + bearing:** warns when drones close in; rough direction
  and range to known bases and large unit movements.
- **Heat meters (per base and global):** how much the Program cares about you
  right now, rising and falling with your actions — so you see escalation
  *before* it arrives at your door, and see neutrality when you earn it.
- **Doctrine / intent readout:** hover a drone group to see the plan its AI has
  actually committed to — "flush you into cover and snipe," "bombard your
  fortifications," "cut off your retreat," "overwhelm" — plus composition and
  stats. You read the intent and pick the counter: bow the flushers, charge
  the bombardiers, knock the rammers out of the sky. This is where the
  Program's adaptation becomes *legible* — you watch it choose.
- **Development tracking:** as it upgrades, shows how their doctrine and tech
  are progressing in real time, and their resource picture.
- **Remote control:** pilot your own salvaged drones from the tablet.

Design rule: escalation you can *see coming* is tense; escalation you can't is
just unfair. The datapad is how the "smart, adapting invasion" fantasy reaches
the player instead of staying buried in the Director's code.

### Death & theft

- Designed to be played with **keep inventory on**.
- Without it: drones haul your death-drops and stolen gear to their storage;
  **tracking chips** let you mark valuables and follow them — every theft
  becomes a raid objective. Item stealing can be disabled in config.

## Endgame & Victory

Two clear win states, so the player always knows what they're fighting toward:

- **v1.0 shippable victory — destroy a main base.** This is a complete,
  satisfying arc on its own: land → survive → arm up → raid the base → sever
  the psionic relay for the resource windfall and your psionic unlock. A
  player who does this has *won the game we ship first.*
- **Full-campaign victory (post-v1.0) — the anti-orbital weapon.** A missile
  battery / railgun (specifics TBD) takes down the orbital station itself and
  ends the Program for good. This is the long-term finale, unlocked after the
  first base kill opens Tier 4–5; it is designed here but is not a v1.0 gate.

For players who don't want it to end, a config option turns re-insertion into
**endless waves**.

## Lore Delivery & The Program's "Voice"

The Program is **silent**. No taunts, no narration — the player infers
everything from behavior. Two exceptions, both diegetic:

- **Data files:** defeating a main base lets you rummage through its data —
  a full listing and map of every area it scouted, plus founders' logs
  explaining *why* the fleet is here: the planet is resource-rich and perfect
  for long-distance programs.
- **Ceasefire protocol** (config-gated, unlocks only after the Program has
  discovered villagers — i.e., learned that negotiation exists): when the
  drones are badly weakened, or the player keeps dying repeatedly, the
  Program may contact the player to offer a timed ceasefire.

## Audio / Horror Direction

The horror is mechanical and procedural, not jump-scare:

- Drones beep like crazy and whirr intensely when they find you.
- Aggressive, personalized tracking pressure based on your weapon profile.
- Alarms, roaring engines — inhuman sounds. A general static/interference
  audio layer accompanies psionic proximity.

### Psionic interference (proximity warning)

Interference is an *ambient early-warning system*, not a one-off scan effect.
Whenever Program drones are within roughly **3 chunks (~48 blocks)** of the
player, a faint shift and hue distortion appears around the edges of the HUD.
It **grows in intensity with how dangerous the nearby drones are** — one
surveyor is a barely-perceptible flicker; a skirmisher pack closing in washes
the screen edges hard. Players learn to read it: presence, escalation, and
(by paying attention) roughly how bad the situation is before they ever see
or hear the unit.

**Directionality** (interference biased toward the screen edge facing the
threat) is available as a config option.

## VFX Direction

Weapons and events use a **Minecrafty particle language while staying
semi-realistic in composition**. Effects are built from vanilla-style
particles (explosion puffs, smoke, flashes, sparks) arranged realistically —
scale, trails, timing — rather than from custom hyper-real effects.
Reference case: the drop pod entry produces a huge smoke column and a roaring
sound in the sky, but the impact itself still reads as classic Minecraft
explosion particles and smoke.

## Visual Identity

- Realistic drone silhouettes built from blocky Minecraft language.
- Palette: gunmetal hulls, **red = sensors/weapons**, **cyan = psionics and
  power systems**.
- **Visible view boxes** on sensor units — you can see what they see, and
  hide from it.

## Configuration (planned)

| Setting | Default | Notes |
|---------|---------|-------|
| Interfere with player structures | **off** | Drones avoid player builds unless in active conflict. Explosive weapons don't care either way. |
| Terrain destruction by explosions | on (gamerule-linked) | Whether Program explosions break terrain. |
| Item stealing | on | Drones loot unattended gear / death drops. |
| Interference directionality | off | HUD warning biased toward threat direction. |
| Ceasefire protocol | on | Only after the Program discovers villagers. |
| Endless waves | off | Re-insertion never stops; anti-orbital victory disabled. |

## Small Things That Matter

1. Player drones can be dyed and carry banner patterns.
2. Larger drones support different weapon/turret loadouts (heavy assault,
   gunship variants).
3. Mod compat: Guard Villagers, Cracker's Wither Storm; Create blocks
   (including moving contraptions) should be attackable/interactable by drone
   swarms — no deep integration required, just coexistence.
4. Psionic interception: a mid/late-game player tool to control mobs.

## Technical Architecture (implementation view)

- **Program Director** (`ProgramDirectorState`, persisted per world): the
  single brain. Holds global threat posture, per-player intel files (risk
  tier, weapon profile, elytra flag, deaths), landing/re-insertion schedule,
  probe/base positions, pending unit dispatches, and later: base registry,
  resource ledger, production queues, era progression.
- **Risk assessment** (`RiskAssessment`): armor + weapon scoring, death-count
  demotion — the tier table above.
- **Units** query the Director for orders; the Director spends a real,
  mined-resource budget to field them. This is what makes "starve the base"
  and "raid their storage" real strategies.
- **Sound events** are registered up-front with vanilla placeholder
  redirects (`sounds.json`), so custom recordings can drop in later without
  code changes.
- **Target version:** Minecraft 1.21.1, **multiloader** (Fabric + NeoForge
  via Architectury): all gameplay code lives in the loader-agnostic `common`
  module; `fabric/` and `neoforge/` are thin entrypoint shims. Chosen so
  compat targets on either loader (Cracker's Wither Storm, Create, Guard
  Villagers) are reachable without maintaining two codebases.
