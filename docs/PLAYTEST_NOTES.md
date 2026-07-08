# Program 7 — Playtest Feedback

## Round 1 (first playable build, 1.21.1 Fabric)

Confirmed working: mod loads, `/program7` commands, advancements (tab reveals
on load, not instantly), datapad readout, mace-shatter.

Feedback, triaged:

### 🔧 Combat feel & audio (contained — first batch)
1. **Gun firing sounds + cadence + whistles.** Gun units need an audible shot
   each time they fire, keep firing several times/second even when you break
   line of sight (suppressing fire), and near-misses should whistle/graze.
4. **Approach whir.** Drones should whir/grow louder as they close on you.
7. **Debris falls.** A dead drone should *drop* to the ground and land with a
   small bang — the wreck block currently appears where it died (mid-air).
8. **Alarm on approach.** Gun emplacements/base should sound an alarm as you
   get near.

### 🔊 AI / behavior
9. **Drones react to sound cues.** Gunfire/explosions/noise should attract
   nearby units (the "sound attraction" pillar — not yet implemented).
3. **Base camera blocks.** The main base detects via placed **camera/sensor
   blocks**, so you're not just omnisciently targeted by drones — take out the
   cameras to move unseen.

### 🏗️ Base & units (bigger builds)
2. **Bigger base + psionic static.** The main assembler should be a real
   structure — e.g. a **3×3** core where the engine sits — not a few loose
   blocks. Also: a **static/interference visual** when they spot you.
5. **The first base is BIG and lands hard.** On landfall it should **crush
   wood/leaves and knock blocks down** in its footprint almost immediately.
   Logistics units should be able to **pick up and relocate the base's own
   blocks**.
10. **Fixed-wing drones: bigger + higher.** The current fixed-wing (Air UAV)
    should be larger and fly at higher altitude; a **small** fixed-wing frame
    can be an attack-drone variant.

### ✏️ Needs a spec from the player
6. **Datapad = a screen, not chat.** The datapad should open a **map-like GUI
   screen**, not print to chat. Player will sketch/specify the layout. (This is
   the deferred Datapad v2 — hold the GUI build until the sketch lands.)

## Round 2 — the DREAD pass

Core note from the player: the drones **aren't pervasive or scary enough yet**.
This is modelled on real drone warfare — the target feeling is *fear*: getting
ambushed should spike your pulse. Everything below serves that one goal.

- **D1. Drones are LOUD and freak out when they see you.** Spotting the player
  should trigger an alarmed, frantic reaction (audio + motion), not a calm scan.
- **D2. Explosions are loud, volumetric, and boomy.** Bigger particle volume,
  heavier low-end sound, real concussion.
- **D3. You can HIDE.** Line of sight is real — while they search, breaking LOS
  and staying behind cover actually loses them. (Ties to #3 camera blocks and
  the LOS-projection below.)
- **D4. Little drones whir and roll/hover over surfaces** — they *elevate over*
  terrain rather than hopping/jumping. Constant whir that rises as they close.
- **D5. Turret = a BLOCK, not an entity.** ⏳ IN PROGRESS. Port AutogunTurret to
  a block + block entity with its own targeting/fire tick.
- **D6. Guns overheat / limited ammo.** Attacking head-on shouldn't let them
  spam fire forever — they overheat (or burn an ammo belt) and must cool/reload,
  giving the player a window.
- **D7. Bigger, much faster drones + projected line of vision.** Scale models up,
  raise speed, and **draw the vision cone/beam** so you *feel* the moment you're
  spotted.
- **D8. Kill the "you've been spotted" text.** Replace the `scan_complete`
  action-bar line with: a loud stinger + a hard spike of psionic interference,
  naturally followed by the sound of incoming drones. Show, don't tell.
  (Anchor: `ScanPlayerGoal.completeScan()` line ~125.)

## Round 3 — presence & emergence

Theme: the fleet should feel like an ever-present occupying force, not a set of
encounters. Reach out, be loud, and *do things in the world on their own.*

- **R1. Longer engagement ranges.** ✅ DONE. Snipers acquire at 96 / fire to 90
  and crack loud (vol 5, ~80-block radius); Director sets them up at 60-90
  blocks; other tiers' acquisition +50%.
- **R2. Louder / more present.** Partial (sniper crack, whir, alarm). Ongoing:
  more ambient menace, distant fire audible, engine noise density.
- **R3. Emergent base behaviors** (the big next wave — needs building):
  - **Dig underground.** Mining/tunnelling units that carve into terrain
    (hardest: block-breaking pathfinding). There are already MEDIUM_MINING and
    HARVESTER drones registered — likely the hook.
  - **Steal resources.** Drones raid dropped items / unattended storage and
    haul it back to base (gear-theft is already a config toggle + partially
    designed; logistics/hauler units are the vehicle). High "threatening"
    value, most tractable — likely first.
  - **Build drones on the spot.** Field fabrication: a unit/beacon that spawns
    new drones away from the main base, so the threat regenerates in the field.
- **R4. Inventory drone parts → 3D.** ⏳ Preview sent for approval (voxel models
  for drone_core / power_bank / transmitter / gun_barrel / explosive_warhead /
  magazine). On approval: generate real MC `elements` models + face textures.
