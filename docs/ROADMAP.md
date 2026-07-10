# Program 7 — Implementation Roadmap

Phases are ordered so every phase ships something playable, and the Director
(the per-world brain) accretes capability instead of being rewritten.

> **v1.0 scope:** ship the complete **Tier 1–3 probe invasion** — landing,
> the mined economy, weapon-profile adaptation, the datapad, base assault, and
> the first-base psionic payoff. **Tiers 4–5 and the anti-orbital finale are
> designed but post-v1.0 "act 2" content**, gated behind the first base kill
> rather than a calendar. This keeps v1.0 coherent and shippable without
> cutting the long-term vision — Tier 1–3 is already a complete game with a
> real win state (destroy a main base).

> **Living design docs (2026-07 session):** the mod's next act is captured in
> `RESEARCH_AND_LOGISTICS.md` (research tree + infrastructure/logistics +
> counter-logistics + the psionic research building), `EXPANSION_PLAN.md`
> (datapad v2, virtualization/LOD, acoustic intelligence, material tree), and
> the gunship silhouette in `UNITS.md`. These reshape Phases 2–4 below.

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
- [x] Pod descent simulation when no player is near the sky column: an
      unwitnessed pod no longer blinks a base into existence — it broadcasts
      the sky roar, takes a few seconds to descend, then booms on impact and
      forms the base (persisted across a restart so nothing gets stranded
      mid-flight)
- [ ] Custom sound recordings replace vanilla placeholders
- [x] Psionic interference HUD overlay: ambient hue/shift vignette whenever
      drones are within ~3 chunks, intensity scaled by the danger of nearby
      units (armed attack drone ≫ surveyor, closer presses harder); static
      audio bursts; directionality behind a config flag
- [x] Config file scaffold (`config/program7.json`, see table in DESIGN.md);
      terrain-destruction toggle wired into attack drone explosions

## Phase 1.5 — Multiloader ✅

- [x] Architectury restructure: `common` (all gameplay + assets) +
      `fabric` + `neoforge` modules; Yarn mappings kept everywhere via the
      NeoForge mappings patch; registration through DeferredRegister,
      events/networking through Architectury API
- [x] Both loaders ship from one codebase: `./gradlew :fabric:build` /
      `:neoforge:build`

## Phase 2 — The Base Lives

- [ ] Main pod becomes a fabricator block entity with starter stockpile
      (iron, copper, redstone, coal, gunpowder)
- [x] Harvester drone v1: wheeled ground unit that finds ore/logs (vanilla
      block tags, so modded ores in those tags count), grinds blocks out
      with visible crack stages and mining noise, hauls a 12-unit hopper
      home and deposits into the ledger; unarmed, panics when hurt; two
      arrive with every pod
- [x] Resource ledger v1: every pod lands with a stockpile (iron, copper,
      redstone, coal, gunpowder); attack drone dispatches cost real
      resources and simply don't launch when the ledger can't pay —
      starving the base already works (moved up)
- [ ] Resource ledger v2: appetite-driven targeting (seek what the Director
      is short on); harvester escorts; medium mining drones that bore into
      cave systems
- [ ] Territory outlining; terrain-aware expansion (mine into existing
      caves, fortify mountains/river crossings); mini outposts
- [ ] Base structure growth: assembly bank, storage deck, warehouse,
      autoturrets, mortars
- [ ] Wheeled logistics drone + crates (gold-striped priority cargo, escorts)
- [ ] Lockdown mode: 20/20/60 production split, high-ground fortification
- [x] Base is attackable: storage blocks mineable/stealable (the Storage Deck
      — planted at every base, seeded with the pod's stores, mine or right-click
      to loot; drones skip their own storage), base HP model (probe core is a
      block entity with HP + an under-attack signal, cracking it reports a
      destruction consequence to the Director), threat-proportional defense
      commitment (a core reading as under attack rallies fresh defenders from
      the ledger onto the nearest attacker — starve the ledger to throttle it).
      Re-insertion after a razing is the remaining piece
- [~] Re-insertion after main outpost destruction: DONE — razing the last
      core site schedules a fresh pod a few in-game days out, landing near a
      player like the opening drop (the relay stays severed and the first-kill
      payoff is one-time, so it's renewed pressure, not a reset; a re-inserted
      base razed again schedules the next). TODO: strength-scaled landing
      distance/resources (far when player dominant, near network-relink when
      weak) and better-scouted ground

## Phase 2.5 — Assembly & Tier 1 Roster (next)

- [x] Assembler structure (battery + assembler = Tier 1 production per
      UNITS.md): planted beside every probe core, watches the base's guard
      complement and builds replacements paid from the Director's ledger.
      Tiered assembler sizes (larger classes per tech tier) still to come
- [x] Support infrastructure v1 — the launch catapult: planted at every
      base, slings a fixed-wing Air UAV that circles and spots; break the
      catapult and nothing replaces a downed UAV (runways / loading docks /
      harbors for the bigger unit classes still to come)
- [x] Movement inertia layer: InertialFlightMoveControl blends acceleration
      and clamps turn rate by airframe mass (surveyor 1.0 → transport 3.0 →
      heavy attack drone 4.0); no instant stops, heavy units telegraph
      every maneuver
- [x] Basic ground drone (tiny armored car with a hitscan turret — anti-mob
      perimeter patroller, only fights players in revenge) and basic
      autogun turret (bolted-down fixed defense, faster firing, helpless
      out of line of sight)
- [x] Logistics drones (flying + wheeled): every assembler build's payment
      now physically travels from the probe core as courier cargo —
      shoot the courier down and the resources spill out as yours; the
      Program writes off payments lost in transit
- [x] First Tier 2 units: medium attack drone (strafing gun flyer),
      long-range sniper drone (fragile standoff, backs away if you close),
      basic mortar (fixed emplacement, arcing shells that whistle before
      impact). NOTE: current impl gates Tier 2 waves on day 14 — a temporary
      stand-in; Phase 3 replaces it with capability/threat gating
- [x] Autogun v3 remodel: taller open skeletal frame — light turrets don't
      hide their ammo feed; shrouds are for the heavier tiers
- [x] Wreck blocks: destroyed drones leave a debris-pile block holding
      their loot-table salvage plus carried cargo; right-click to pull
      parts out (collapses when emptied), break to spill everything
- [x] Drone combat physics v1 (in the shared ProgramDroneEntity base):
      swords/axes deal 2.5x to fliers (usually one-hit), Knockback/Punch
      hits scramble fliers — stabilizers cut out, wild spin and drift, and
      terrain contact while tumbling is a crash
- [x] Drone combat physics v2: attack-drone rams are telegraphed (stop,
      15-tick locked aim with a particle warning, then a committed straight
      dash — strafe to dodge, bait it into a wall to kill it), and rotor
      quick-kills: hits landing in the top slice of a flier's hitbox do
      1.5x and instantly scramble it
- [x] First Tier 3 units (all player-height or bigger per design rule):
      heavy attack drone (car-sized burst-fire gun flyer), IFV (cannon +
      deploys a ground-drone fireteam from its bay), gunboat (toughest
      hull in the game, water-locked, helpless if beached; harbors gate
      its production later), recon helicopter (hunts you and pins a
      searchlight on you), mobile battery center (repair aura; brownout
      debuffs nearby units when killed)
- [x] Tier 3 rescale & differentiated movement: every Tier 3 hitbox enlarged
      (gunboat now a giant 4.5×3.0 hull), heavies knockback-immune; fixed-wings
      fly like planes (wide banking turns, minimum airspeed, no hover),
      helicopters pivot deliberately on heavier mass, tanks/boats carry
      momentum; models scaled to match and aircraft bank into their turns; loud
      constant engine/rotor ambience on every vehicle
- [x] Air UAV v2 remodel: modern military drone silhouette — long slim
      fuselage, satcom nose, high-aspect wing, V-tail, rear pusher prop
- [x] Tier system enforcement in the Director: waves fold in a Tier 3 heavy
      once Tier 2 is open and global threat crosses 30 — a recon helicopter to
      hunt a player on elytra, a heavy attack drone otherwise (both airborne so
      the aerial escort drop suits them). Tier 3 is the v1.0 ceiling; Tier 4+
      stays locked behind the first main-base kill, and heavy fabrication
      carries a steep post-relay skip once a base has fallen
- [x] Orbital resupply event: every 3 in-game days each surviving probe
      core calls down a capsule — thunder-loud, a glowing column visible
      from far off, a psionic interference spike pointing at the base, and
      a ledger refill. Bases left alone compound; raze them early
- [x] Second Tier 2 wave: medium mining drone (airborne harvester, double
      hopper), transport drone (two-crate heavy courier, sent for payments
      of 12+ units), anti-air turret (open-frame flak that only tracks
      airborne targets — landing breaks its lock), scout car (fast unarmed
      spotter that paints you for every idle combat unit within 48 blocks)

## Phase 3 — Adaptation & Counterplay

- [x] Weapon-profile counters: ranged → explosive-rusher swarms, melee →
      standoff harassers (sniper + gun drone), and elytra → instant-reaction
      interception (a fast medium-attack drone the moment Tier 2 is live, plus
      a recon-helicopter hunter at Tier 3) — all branched in executeDispatch
      off the scanned player's weaponProfile / elytra flag
- [x] Capability/threat escalation model: replaced the day-14 Tier 2 timer
      with `tier2Unlocked()` gating on accumulated threat/scans (globalThreat
      ≥ 8 or scans ≥ 3). Mined-economy gating and non-player threat adaptation
      (mobs, wither storm) are still future refinements
- [x] Typed-armor damage model: per-unit `ArmorProfile` resistance by damage
      type (ballistic / high-velocity impact / piercing / explosive /
      enchanted / melee / generic) — the mod-compat backbone (see DESIGN.md).
      Armored vehicles resist bullets, take extra from arrows/enchants; gunboat
      hull toughest; Tier-1 fliers unarmored
- [x] Heat is a two-way dial: threat decays after ~2 days of no contact into a
      DORMANT/NEUTRAL posture (stop engaging / beat them down → they stand
      down); posture surfaces on the Datapad (see DESIGN.md heat model)
- [~] Datapad v1 (pulled forward from Phase 4 — the mod's identity and the fix
      for "adaptation is invisible"): DONE — right-click chat readout of
      posture/heat, nearby unit count + nearest bearing, nearest base bearing.
      TODO v2: full GUI screen designed this session — chunk-grid radar,
      coverage-limited fog, alert-ramp tooltip states, two-sense acoustic intel,
      research-progress + fleet/heat panels (see EXPANSION_PLAN C7 +
      RESEARCH_AND_LOGISTICS). SHIPPED: the v2 radar screen — reading the
      datapad pushes a server snapshot to a client Screen with a status rail
      (posture, heat bar, off-grid fleet estimate vs in-range count, escalation
      tier, nearest-base bearing) beside a north-up chunk-grid radar; contacts
      are blips coloured by their AlertState toward you, a slow sweep + ~1.5s
      beep rakes the grid, and hovering gives a per-contact read. TODO: live
      refresh (currently a frozen snapshot), acoustic deception/bluffing, and
      research-progress panel
- [x] Onboarding advancements: "Uninvited Guests" (first salvage) and "Know
      Your Enemy" (carry a datapad) — addresses the no-onboarding review gap
      (needs an in-game check; advancement JSON isn't compile-verifiable)
- [~] Anomaly detection & recon perception: DONE — recon spotters carry an
      AlertState ramp (UNAWARE→SUSPICIOUS→SEARCHING→TRACKING→ENGAGING),
      LOS-gated detection to ~160 blocks (break line-of-sight to hide), and an
      InvestigateDisturbanceGoal that catches *movement* (yours or a passing
      mob's), swells psionic interference into a spotted crack, darts over to
      look, and can mistake a mob for you before peeling off — all soundborne,
      no text. Sound-attraction now ships too: breaking a hard block (stone and
      up) registers a loudness-scaled noise, and combat drones within earshot
      hold a psionic-buildup beat then drift over to investigate the source
      ("something heard me"). TODO: player-placed-block anomaly triggers in
      wilderness; full persistent view-cone render
- [x] Suppressive fire + world-chewing rounds: hitscan firearms break softer
      cover after a few hits (blast-resistance scaled, mobGriefing-gated) and
      kick up dirt/stone/water on impact; past 60% of range, or at a lost
      target's last-seen spot, mounts switch to loud, wide, pressuring
      suppressive fire — snipers stay precise
- [ ] Gunship doctrine: known players get a delayed, deliberate sneak attack
      once a gunship is available
- [x] "Still Here" advancement for surviving seven days after landfall
      (moved up — already implemented)
- [ ] Skirmisher drone (car-sized: HMG + grenade launcher) and light attack
      drone
- [ ] Component hitboxes on larger drones (snipe a rotor / diamond hardpoints)
- [~] Drones flee fights they can't win: a `fleeHealthFraction()` hook on the
      shared drone base breaks a unit off toward its attacker (via the existing
      RetreatGoal at priority 0) once its health drops to the threshold — the
      medium attack drone peels off at 30%, the heavy limps away at 15% ("it's
      running, finish it"). TODO: environmental damage tuning (withers and
      golem armies as viable base-killers through mid game)
- [x] Gear theft v1: surveyors snatch dropped item stacks (config-gated)
      and carry them as cargo — killing the thief spills everything back
      out (moved up)
- [ ] Gear theft v2: raids on unattended player storage, hauling cargo to
      base storage, tracking-chip recovery (keep-inventory recommended
      default)

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
- [ ] First main-base kill payoff: sever the psionic relay → resource windfall
      + player **psionic unlock** + degrade the fleet's heavy fabrication;
      this is what opens Tier 4–5 for both sides (see DESIGN.md)
- [ ] Charge laser + ender-pearl gun + AP crossbow bolts / heavy arrows: the
      typed anti-armor answers so every build has a lane (see UNITS.md salvage)

## Phase 5 — World Integration

- [ ] Villager trading, remote-controlled iron golems, anti-pillager ops
- [ ] Ceasefire protocol + heat model: heat is a two-way dial (aggression and
      anomalies raise it; sustained non-engagement — or beating the fleet into
      submission — lower it). Flooring heat by force earns an uneasy neutrality;
      after discovering villagers a weakened Program may offer a timed truce
      (config-gated). Heat surfaces on the datapad
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
      *full-campaign* victory, kills the orbital station and ends the Program.
      NOTE: v1.0's shippable win is destroying a main base (unlocks psionics +
      Tier 4–5); this finale is post-v1.0 and not a v1.0 gate
- [ ] Endless waves config: re-insertion never stops
- [ ] CurseForge release packaging

## Release checklist (CurseForge)

- [ ] License decision (currently All Rights Reserved)
- [ ] Real textures/models/sounds pass
- [ ] Config file complete (see DESIGN.md table)
- [ ] Multiplayer balance review (per-player intel already supports it)
- [ ] Performance budget: drone AI tick cost, cap simultaneous active units
