# Program 7 — Sound Design

*The horror-audio bible (2026-07-09). The goal: **realistic, threatening,
persistent** audio that makes hostile drones feel **omnipresent and pervasive**.
Build suspense, not jumpscares — the player mining ore in silence should hear a
faint static creep in and *wonder* if they're about to be attacked. Pull from
how horror games build dread: sharp trills on a sighting, threat sounds pushed
forward against the ambient bed, uncertainty about what's actually out there.*

---

## 1. Ranged sound tiers — every weapon and unit genre has three
Each **gun** (and each unit **genre** — small gun, MG/autocannon, sniper,
explosion, rotor, drill, tracks) gets three distance layers:
1. **Close** — actively attacking you.
2. **Medium** — roughly beyond ~80 blocks.
3. **Far** — distant.
A distant machine-gun is a different *sound*, not just a quieter one — flatter,
boomier, its transient softened. Close is sharp and immediate.

## 2. Physical realism
- **Travel time.** Sound arrives *after* a delay set by distance (speed of sound
  ≈ 17 blocks/tick). You see/feel the event, then hear it — the crack-then-thump
  of real distant gunfire.
- **Distance shaping.** Far = deeper, muffled, boomy; close = sharp and bright.
- **Rock muffling.** Rock transmits far less than air: a sound separated from you
  by stone is **muffled** (quieter, low-passed / lower-pitched). Mining in a
  sealed cave, most of the surface war is a dull, directionless murmur.
- **Cave echo.** Drones in the **same enclosed space** as you reverberate — that
  cave you *guaranteed* was safe suddenly has a wet, ringing thump in it.
- **Loud, mechanical, persistent.** Helicopters whir **constantly** and carry
  from far off — distant rotor whir layered with the bang of their guns.

## 3. Interference as an audio spotlight
- **Static = "a drone is near."** It rises with the screen-edge blur.
- **Music cuts** (shipped) so the Program's own sound *becomes* the score.
- **Vanilla sounds feel louder / highlighted** under interference — your own
  sword swing on a zombie, your footsteps, bridging out over a gap all read
  sharper and more *alerting*.
- **You mistake footsteps for drones.** Silence in a cave → faint static + blur →
  dread, before anything is even there.
- **Sharp trill on a sighting** — the moment you spot a drone, an edge-inducing
  stinger, so threat sounds stand out hard against the background bed.
- **Intent: suspense, never a cheap jumpscare** — "am I about to be attacked?"

## 4. Ambiguity & deception — you don't know what it is
- **Sound alone can't ID the unit.** A light whir + a small gun *might* be a gun
  drone. Maybe several. Maybe a heavy. Maybe just a **logistics drone playing
  fake threat sounds to scare you off.** You can tell it *may* be hostile or
  harmless — never for certain.
- **The datapad only *guesses*** from context clues (sound profile, count,
  time of day, direction) — a probability, not a readout.
- **Signature cues:** a faint whir (a drone, or a drill); the deep **thump of a
  ground-penetrating scanner** when you stumble into "safe" ground.

---

## 5. Build decomposition
**Buildable now — the acoustics engine (existing placeholder sounds):**
- A per-world **scheduled-sound queue** (travel-time delay).
- **Distance-band** volume/pitch shaping (close/medium/far) off the source→player
  distance.
- **Basic occlusion muffling** — raycast source→player, count solid blocks, drop
  volume + pitch when rock is in the way.
- Route every Program sound emit through one `ProgramAcoustics` helper so all of
  the above applies uniformly; the real 3-variant selection slots into the same
  call once assets land.

**Needs assets (the CC0/public-domain shortlist):** the actual close/medium/far
recorded variants per weapon; drone whirs, drill loops, scanner thump, spot
trills, richer static.

**Needs datapad + acoustic-intel (C7) + virtualization:** audio-based unit
*guessing*, logistics fake-sound **deception**, hearing **off-screen** fights
(the two-sense datapad), the scanner-thump-on-cave-entry event.

**Extends interference (partly shipped):** music-duck ✅; vanilla-sound
highlighting, spot trills, and per-action reinforcement under interference next.

## 6. Engine implementation notes
- Speed of sound ≈ **17 blocks/tick** → travel delay = round(distance / 17).
- Per-world scheduled queue ticked from the existing `SERVER_LEVEL_POST` hook.
- Occlusion: raycast source→player, count `COLLIDER`-solid blocks between.
- **Per-player** occlusion/volume needs per-player sound packets
  (`PlaySoundS2CPacket`) — that's v2; v1 shapes off the nearest player and is
  correct in singleplayer.
- Distance bands (starting point): close < 20, medium 20–80, far > 80; far gets
  higher base volume (to carry) but lower pitch (to read as distant).
