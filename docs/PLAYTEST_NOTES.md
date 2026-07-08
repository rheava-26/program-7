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
