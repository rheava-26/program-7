# Program 7 — Backlog & Session Handoff

Single source of truth for outstanding work. **Read this first when resuming** — it saves re-deriving state from chat history. Complements `ROADMAP.md` (the phase plan) and the design docs. Last updated 2026-07-13.

## Current branch / state

- Working branch: `claude/backlog-review-jg7xm6`
- CI is **compile-only** — gradle can't run in this environment, so there is no runtime verification; changes below are hand-reasoned for compile correctness but unverified at runtime.
- Recent commits (newest first):
  - shared shell base (`AbstractShellEntity`) + drag-aware `BallisticSolver` shared by howitzer and mortar + `IndirectFireUnit`-generalized `FireMissionManager` with battery fire (see "Shipped this session" below)
  - `7b61ab9` — review fixes: drag-aware howitzer ballistics, mission persistence on unloaded chunk, no free rearm on a damage-flee
  - `e8e4791` — stand up the Director `FireMissionManager`, wire the howitzer as its first client
  - `2ccbb54` — detailed M109 howitzer model + extended range
  - `208d0fe` — crate unpacking, combat ammo/power retreat, carry-to-capacity logistics
  - `1514986` / `d029350` — cargo/ammunition block family + art passes

## Shipped this session (artillery + trees batch)

- **Shared shell base** (`AbstractShellEntity`, `common`): `HowitzerShellEntity` and `MortarShellEntity` now extend one abstract `ThrownEntity` base that owns the smoke trail, falling whistle, and impact-sound-then-explosion sequence; subclasses only supply gravity, warhead power, sounds, and a couple of presentation knobs. Unlocks every artillery type added below without copy-pasting a shell class.
- **Mortar drag-aware ballistics fix** (the "review findings not yet fixed" item): `BallisticSolver` (`entity/ai`) factors the howitzer's drag-aware trajectory sim into a shared static helper; both `HowitzerAttackGoal` and `MortarAttackGoal` now call it instead of the mortar's old naive `dx / FLIGHT_TICKS` constant-speed backfill, which landed rounds short at range.
- **`IndirectFireUnit` interface** (`entity`): marks any `MobEntity` as a `FireMissionManager` client (`indirectMinRange`/`indirectMaxRange`/`indirectBaseSpread`/`battery()`). `HowitzerEntity` and `MortarEmplacementEntity` both implement it now; `FireMissionManager`'s scan pass and `updateSelfObserved`/`missionFor` are generic over it instead of hardcoded to `HowitzerEntity`, so the mortar is now a full manager client (self-observed + assigned, ranging walk-in, CEP) — not just the howitzer.
- **Battery fire**: `FireMissionManager` shares one `FireMission` object across same-`battery()`-key tubes within 32 blocks targeting the same player, so several howitzers (or mortars) parked near each other range in together and fire for effect denser than any one tube alone. NOTE: battery-sharing is a live-scan optimization only — a world restart flattens shared missions back to independent per-tube copies, which the next scan re-shares if they're still on the same target. Not yet exercised by any in-game battery placement (no wave/dispatch code groups tubes together); proven by the mechanism, not by content.
- **Datapad inbound-fire warning generalized**: `DatapadItem.incomingFor` now scans for any `AbstractShellEntity` (not just `HowitzerShellEntity`), so the ETA/bearing warning will pick up every new munition family below once they exist.
- **MLRS / rocket artillery** (`MlrsLauncherEntity` + `MlrsRocketEntity` + `MlrsAttackGoal`): mobile Tier 3-4 rocket artillery, a `FireMissionManager` client with a wide `indirectBaseSpread` (3x the howitzer's) and `battery()` opted out (mobile pieces fire alone per the doc's platform split). Fires a 6-rocket ripple staggered 4 ticks apart, each independently scattered inside a widened spread, then a long reload — the doc's "stutter of launches... wall of impacts arriving together." Registered end to end (entity/attributes, spawn egg, lang, loot table); model/renderer **reuse the howitzer chassis and howitzer-shell geometry as a placeholder** (documented in the model classes) rather than a bespoke rocket-rack/finned-rocket silhouette — a real art pass is still open.
- **Guided missiles** (`MissileLauncherEntity` + `GuidedMissileEntity` + `MissileAttackGoal`): Tier 4-5 mobile precision artillery — the doc's "deliberate inversion of unguided rockets." Fires a near-flat, low-gravity missile that steers its own velocity toward a live target each tick within a limited turn rate (`GuidedMissileEntity.steerToward`), but **only when the firing mission was currently spotted** (`FireMission.targetPlayerId()`, a new getter) — an unobserved mission still fires the missile dumb at the stale point, so losing the observer degrades a missile exactly like every cheaper tube. Tiny 4-round magazine, ~20s reload, opts out of battery sharing (rare and expensive, fires alone). Same placeholder-geometry approach as the MLRS (reuses the howitzer chassis + howitzer-shell model), registered end to end.
- **Naval bombardment**: `GunboatEntity` now implements `IndirectFireUnit` and gets a new `NavalBombardmentGoal` (lower priority than its existing `DeckGunAttackGoal`, so it only actually starts when the deck gun has no line-of-sight target of its own — the `LOOK`-control conflict does the arbitration). Fires the same `HowitzerShellEntity` the deck gun's self-observed arc mode already uses, through the full `FireMissionManager` loop (ranging, CEP, battery-opt-out since it's a mobile hull that "reposition[s] along the coast"). **Bonus fix while in this file**: `DeckGunAttackGoal.fireArcingShell`'s own self-observed mode also had the naive `dx / FLIGHT_TICKS` ballistic backfill (the same flaw the mortar had) — switched to the shared `BallisticSolver` too, so both the manager-assigned and self-observed naval bombardment paths land correctly at range.
- **Close air support** (`BombEntity` + `CasBombingGoal`, wired onto the existing `GunshipEntity`): a new `FireMissionManager` client that, whenever the belly cannon has no live target of its own, flies to a point above a purely indirect designation and releases a 4-bomb stick a few ticks apart, offset along its own current heading so the stick walks a line across the target rather than clustering — the doc's "a stick of bombs walked along a line across the target on a diving pass." No new aircraft entity: the doc explicitly frames CAS as "a fixed-wing or the gunship," and `GunshipEntity`'s own class doc already flagged the bombing run as wired in later — this is that pass. Bombs draw from the gunship's existing cannon magazine. Lower priority than the belly cannon's `BurstGunAttackGoal` (same arbitration pattern as the other new goals this session).
- **Terrain saturation / erosion accumulator** (`TerrainSaturation`, a new `ProgramDirectorState` sibling organ next to `FireMissionManager`, ticked and NBT-persisted the same way): the doc's §5a cumulative-erosion model. Every shell impact (`AbstractShellEntity.onImpact`) feeds a coarse, lazily-decaying per-column grid (4x4-block cells, decay computed on read rather than swept every tick) via a `saturationWeight()` hook — 1.0 for mortar/rocket/bomb, 2.0 for the howitzer/missile ("heavier munitions add more saturation per hit"). Once a cell's saturation crosses a block's own blast-resistance-scaled threshold, a small footprint around the impact (3x3 columns, 2 Y layers) steps **at most one stage per block per impact** through a staged downgrade table (stone→cobblestone→gravel→sand→gone, grass/dirt→coarse dirt→sand→gone, snow→gone) — restrained per round, only sustained fire on one spot reaches the late stages, matching the doc's "no instant craters" rule. **Scope limit (documented in the class doc):** the downgrade table only covers the common stone/soil families; an unlisted block falls back to a coarse "pulverize past a high threshold" rule rather than a full staged chain — a fuller per-block table is a later content pass. Untested for real bombardment feel/pacing (thresholds are a first guess, same "needs playtesting" caveat as the CEP constants).

## Shipped this session

- **Cargo/ammunition block family** (`common`): `CrateBlock`, `AmmoBoxBlock`, `AutocannonMagazineBlock`, `ArtilleryShellBlock`, `PowerCellBlock`. Sea-pickle-style clustered stacking (`COUNT` 1–4). Cook-off: ignites and explodes from flame projectiles, lava, and fire neighbours, with blast power scaling per unit in the cluster. Recipes (gunpowder + iron; power cell = redstone + copper + iron), loot tables, lang entries. 3D outlined models with the faction palette + cyan accents; the crate is metal, rectangular, and shorter than the others.
- **`CrateBlockEntity`**: 5-slot inventory, an unpack-over-time countdown, folds its contents into an adjacent `StorageDeckBlockEntity` (new `offer()`) or scatters them. NOTE: nothing fills a crate yet — see Outstanding.
- **Combat drone ammo/power retreat gate**: onboard magazine (`getMagazineSize`/`getRounds`/`consumeDroneRound`/`refillRounds`/`hasAmmo`) plus battery charge; a unit that runs out of ammo or sags to low charge breaks off to resupply and returns topped up. Gated units: Medium / Heavy / Sniper fliers. A get-hit flee does NOT earn the free resupply (fixed this session).
- **Logistics carry-to-capacity**: `AmmoRunGoal` batches up to 3 loads per sortie.
- **Howitzer**: detailed M109 Paladin model (58 cuboids); gun range 112 blocks (~7 chunks), follow range 120.
- **Artillery fire-mission system** (`FireMissionManager`, a Director sibling): three target sources — observer relay (recon `AlertState` TRACKING/ENGAGING), a dwell heatmap (per-chunk decaying grid of where players linger), and counter-battery (a far-off player who shot a drone). Accuracy is a range-floored CEP scaled by chunk distance, tightened by spotting and a per-shot ranging walk-in. Missions and the dwell grid persist in NBT. The datapad shows an incoming-fire warning (bearing + ETA).

## Outstanding work

### Review findings not yet fixed (2026-07 Fable sweep)

- **`AmmoRunGoal` depot-stock leak** (SUSPECTED, low): a batched sortie debits the `SupplyNetwork` ledger per load during `PICKING_UP`, but if the goal stops mid-sortie (drone flees, target weapon dies/unloads, goal preempted), `stop()`/`start()` discard `carriedLoads` without crediting the depot back — silent stock loss (up to 3 loads now, vs 1 before batching). Fix: return unspent `carriedLoads` to the depot in `stop()`.
- **Datapad incoming-fire ETA reads early** (COSMETIC): `DatapadItem.incomingFor` computes the closest-approach time from the shell's current velocity as if constant, but horizontal velocity decays 1%/tick, so true arrival is later than shown. Display-only.

### Deferred integrations

- **Crates aren't drone-filled**: `CrateBlockEntity.setCargo` has no callers and nothing places a `CrateBlock` programmatically, so the unpack → StorageDeck pipeline never runs in-game. Needs a logistics drone to place and fill crates on arrival (ties into ROADMAP Phase 2 "wheeled logistics drone + crates").
- **Ammo/power gate for ground gun units**: the IFV and GroundDrone are ungated (`getMagazineSize() == 0`); only Medium/Heavy/Sniper fliers carry an onboard magazine + retreat gate. Decide whether ground units should too.

### Deferred artillery slices (see `ARTILLERY_AND_INDIRECT_FIRE.md`)

- Off-screen statistical resolution: resolve fire missions abstractly in unloaded chunks (no player near).

### Other long-standing items

- Full audio SFX set wired into `sounds.json` (custom recordings replacing the vanilla placeholders — also ROADMAP Phase 1).
- Gunship detail-model pass (bring it up to the IFV / howitzer detail level).
- Research tree (player reverse-engineering; see `RESEARCH_TREE.md` / `RESEARCH_AND_LOGISTICS.md`; ROADMAP Phase 4).

## Environment notes for resuming

- Gradle can't run locally (the proxy blocks the distribution download). CI is compile-only — verify via GitHub Actions on the branch, never locally.
- 1.21.1 Yarn API gotchas: `onStateReplaced` is the 5-arg overload; recipe result is `{"id","count"}` and ingredients use the object form `{"item"}` / `{"tag"}`; data folders are singular (`recipe` / `loot_table` / `advancement`); `DustParticleEffect(Vector3f, float)`.
- Model pipeline: scratchpad Python generators (`gunship_sculpt.py` / `detailed_ifv.py` / `detailed_cargo.py`) → source geometry + UV atlas + texture + Java `getTexturedModelData()`. Front = -Z, up = -Y (Minecraft convention).
- Commit trailer convention is set by the environment; never put a model identifier in commits, PRs, or code.
