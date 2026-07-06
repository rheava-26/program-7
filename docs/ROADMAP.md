# Program 7 — Implementation Roadmap

Phases are ordered so every phase ships something playable, and the Director
(the per-world brain) accretes capability instead of being rewritten.

## Phase 0 — Foundation ✅

- [x] Fabric project scaffold (MC 1.21.1, Loom, Java 21)
- [x] Salvage items: drone core, power bank, transmitter, gun barrel,
      explosive warhead, magazine (+ creative tab)
- [x] Sound event registry with vanilla placeholder redirects
- [x] **Surveyor Drone**: flying scan unit — approaches, scans with
      accelerating beeps, files an intel report, retreats; drops salvage
- [x] **Program Director** world state: global threat, per-player intel
      (risk tier / weapon profile / elytra / deaths)
- [x] `/program7 status|assess|threat` debug commands
- [x] Placeholder textures + custom quad-rotor entity model

## Phase 1 — First Contact (in progress)

- [x] Drop pod v1: scheduled insertion 2–3 in-game days in, lands 300–600
      blocks from a random player; live descent entity (smoke column, flame
      trail, sky roar) when witnessed, instant landing + distant boom when
      not; vanilla explosion particles on impact per VFX direction
- [x] Probe core block v1: tough (iron-pick tier), luminous, drops heavy
      salvage + iron when cracked
- [x] Explosive attack drone (Tier 1 response unit): chases, arms at contact
      range with accelerating beeps, committed once armed — kite it;
      explosion respects mobGriefing; shot down = warhead salvage
- [x] Director response loop v1: filed scan → mustering delay → tier-scaled
      attack drone dispatch on the scanned player
- [x] `/program7 land [distance]` test command
- [ ] Long-range visibility trickery for the descent (fake far-render /
      skybox streak so the landing reads from 300+ blocks)
- [ ] Pod descent simulation when no player is near the sky column
      (currently resolves instantly instead)
- [ ] Custom sound recordings replace vanilla placeholders
- [x] Psionic interference HUD overlay: ambient hue/shift vignette whenever
      drones are within ~3 chunks, intensity scaled by the danger of nearby
      units (armed attack drone ≫ surveyor, closer presses harder); static
      audio bursts; directionality behind a config flag
- [x] Config file scaffold (`config/program7.json`, see table in DESIGN.md);
      terrain-destruction toggle wired into attack drone explosions

## Phase 2 — The Base Lives

- [ ] Main pod becomes a fabricator block entity with starter stockpile
      (iron, copper, redstone, coal, gunpowder)
- [ ] Harvester drone (actually breaks ore blocks, needs them, hauls them);
      medium ground-based mining drones from the starter complement
- [ ] Resource ledger: units cost real mined resources; iron/copper/redstone
      appetite drives harvester targeting
- [ ] Territory outlining; terrain-aware expansion (mine into existing
      caves, fortify mountains/river crossings); mini outposts
- [ ] Base structure growth: assembly bank, storage deck, warehouse,
      autoturrets, mortars
- [ ] Wheeled logistics drone + crates (gold-striped priority cargo, escorts)
- [ ] Lockdown mode: 20/20/60 production split, high-ground fortification
- [ ] Base is attackable: storage blocks mineable/stealable, base HP model,
      threat-proportional defense commitment
- [ ] Re-insertion after main outpost destruction: long wait, then far/more
      resources (player dominant), near network relink (player weak), or
      better scouted ground — always within a few thousand blocks

## Phase 3 — Adaptation & Counterplay

- [ ] Weapon-profile counters: ranged → explosive swarms; melee → ranged
      harassers; elytra → instant-reaction interception
- [ ] Anomaly detection: player-placed blocks in wilderness trigger search
      patterns; visible view cones on sensor drones (hide by breaking LoS)
- [ ] Gunship doctrine: known players get a delayed, deliberate sneak attack
      once a gunship is available
- [x] "Still Here" advancement for surviving seven days after landfall
      (moved up — already implemented)
- [ ] Skirmisher drone (car-sized: HMG + grenade launcher) and light attack
      drone
- [ ] Component hitboxes on larger drones (snipe a rotor / diamond hardpoints)
- [ ] Drones flee fights they can't win; environmental damage tuning
      (withers and golem armies are viable base-killers through mid game)
- [ ] Gear theft raids on unattended player storage (config), death-drop
      hauling to base storage (keep-inventory recommended default)

## Phase 4 — Reverse Engineering (player tech tree)

- [ ] Reverse-engineering bench: salvage → schematics
- [ ] Player drones (dyeable, banner patterns)
- [ ] Hand-built firearms/rifles: loud (gunfire attracts the network),
      ammo-hungry, slower build-up than bows; early salvage guns ≈ Power
      II–III bow with better range; drones resist firearms, enchanted
      weapons bypass drone armor
- [ ] Electronics: tracking chips (mark stolen items), glowstone
      illuminators (glowing on mobs), tracking displays, datapad
      drone-sensor (heartbeat-monitor style)
- [ ] Scanner systems: soft X-ray ore overlay from drone scouting, GPR
- [ ] Automatic MG turret, surface-to-surface missile rack, laser drill,
      thermal vision
- [ ] Cold fusion engine drop from destroyed bases → player automation

## Phase 5 — World Integration

- [ ] Villager trading, remote-controlled iron golems, anti-pillager ops
- [ ] Ceasefire protocol: after discovering villagers, a weakened Program
      (or one facing a repeatedly-dying player) may offer a timed truce
      (config-gated)
- [ ] Nether expansion via ruined portals
- [ ] Minecraft-tech adoption: enchanting, sculk sensors, spawner farms,
      totems on heavies, trident interceptors, arrow-battery launchers
- [ ] Era escalation: bore tunnelers, gunships, orbital launches, ballistic
      missiles
- [ ] Psionic interception (player mob-control tool)
- [ ] Mod compat pass: Guard Villagers, Cracker's Wither Storm, Create
      coexistence; drones react to modded enemies

## Phase 6 — Endgame & Release

- [ ] Data files loot from defeated main bases: scouted-area map + founders'
      logs (why the fleet is here)
- [ ] Anti-orbital weapon (missile battery / railgun — design TBD): the
      intended victory, kills the orbital station and ends the Program
- [ ] Endless waves config: re-insertion never stops
- [ ] CurseForge release packaging

## Release checklist (CurseForge)

- [ ] License decision (currently All Rights Reserved)
- [ ] Real textures/models/sounds pass
- [ ] Config file complete (see DESIGN.md table)
- [ ] Multiplayer balance review (per-player intel already supports it)
- [ ] Performance budget: drone AI tick cost, cap simultaneous active units
