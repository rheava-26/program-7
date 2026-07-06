# Program 7 — Implementation Roadmap

Phases are ordered so every phase ships something playable, and the Director
(the per-world brain) accretes capability instead of being rewritten.

## Phase 0 — Foundation ✅ (current)

- [x] Fabric project scaffold (MC 1.21.1, Loom, Java 21)
- [x] Salvage items: drone core, power bank, transmitter, gun barrel,
      explosive warhead, magazine (+ creative tab)
- [x] Sound event registry with vanilla placeholder redirects
- [x] **Surveyor Drone**: flying scan unit — approaches, scans with
      accelerating beeps, applies psionic interference (darkness + sound +
      action-bar warning), files an intel report, retreats; drops salvage
- [x] **Program Director** world state: global threat, per-player intel
      (risk tier / weapon profile / elytra / deaths)
- [x] `/program7 status|assess|threat` debug commands
- [x] Placeholder textures + custom quad-rotor entity model

## Phase 1 — First Contact

- [ ] Drop pod: meteor-style descent (visible from hundreds of blocks, huge
      smoke column, roaring sky sound), lands 300–600 blocks from spawn after
      day 2–3; impact reads as vanilla explosion particles + smoke (see VFX
      Direction in DESIGN.md)
- [ ] Probe core block (the pod itself): heart of the future base, killable,
      drops early salvage; despawn/derelict rules
- [ ] Explosive attack drone (Tier 1 response unit, creeper-style approach)
- [ ] Director response loop: scan report → proportional unit dispatch
- [ ] Custom sound recordings replace vanilla placeholders
- [ ] Psionic interference HUD overlay: ambient hue/shift vignette whenever
      drones are within ~3 chunks, intensity scaled by the danger of nearby
      units (surveyor = faint flicker, skirmisher pack = heavy wash)

## Phase 2 — The Base Lives

- [ ] Harvester drone (actually breaks ore blocks, needs them, hauls them)
- [ ] Resource ledger: units cost real mined resources; iron/copper/redstone
      appetite drives harvester targeting
- [ ] Base structure growth: assembly bank, storage deck, warehouse, turret
      emplacements (small-caliber autoturrets)
- [ ] Wheeled logistics drone + crates (gold-striped priority cargo, escorts)
- [ ] Lockdown mode: 20/20/60 production split, high-ground fortification
- [ ] Base is attackable: storage blocks mineable/stealable, base HP model,
      threat-proportional defense commitment

## Phase 3 — Adaptation & Counterplay

- [ ] Weapon-profile counters: ranged → explosive swarms; melee → ranged
      harassers; elytra → instant-reaction interception
- [ ] Skirmisher drone (car-sized: HMG + grenade launcher) and light attack
      drone
- [ ] Component hitboxes on larger drones (snipe a rotor / diamond hardpoints)
- [ ] Drones flee fights they can't win; environmental damage tuning
      (withers and golem armies are viable base-killers through mid game)
- [ ] Gear theft raids on unattended player storage

## Phase 4 — Reverse Engineering (player tech tree)

- [ ] Reverse-engineering bench: salvage → schematics
- [ ] Player drones (dyeable, banner patterns)
- [ ] Hand-built firearms/rifles from barrels + magazines
- [ ] Scanner systems: soft X-ray ore overlay from drone scouting, GPR
- [ ] Automatic MG turret, surface-to-surface missile rack, laser drill,
      thermal vision
- [ ] Cold fusion engine drop from destroyed bases → player automation

## Phase 5 — World Integration & Endgame

- [ ] Villager trading, remote-controlled iron golems, anti-pillager ops
- [ ] Nether expansion via ruined portals
- [ ] Minecraft-tech adoption: enchanting, sculk sensors, spawner farms,
      totems on heavies, trident interceptors, arrow-battery launchers
- [ ] Era escalation: bore tunnelers, gunships, orbital launches, ballistic
      missiles
- [ ] Psionic interception (player mob-control tool)
- [ ] Mod compat pass: Guard Villagers, Cracker's Wither Storm, Create
      coexistence
- [ ] CurseForge release packaging

## Release checklist (CurseForge)

- [ ] License decision (currently All Rights Reserved)
- [ ] Real textures/models/sounds pass
- [ ] Config file (landing delay/distance, difficulty scalars, feature toggles)
- [ ] Multiplayer balance review (per-player intel already supports it)
- [ ] Performance budget: drone AI tick cost, cap simultaneous active units
