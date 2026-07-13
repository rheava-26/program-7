# Program 7 — Backlog & Session Handoff

Single source of truth for outstanding work. **Read this first when resuming** — it saves re-deriving state from chat history. Complements `ROADMAP.md` (the phase plan) and the design docs. Last updated 2026-07-13.

## Current branch / state

- Working branch: `claude/minecraft-fabric-mod-gb9ys2`
- All work is CI-green. CI is **compile-only** — gradle can't run in this environment, so there is no runtime verification.
- Recent commits (newest first):
  - `7b61ab9` — review fixes: drag-aware howitzer ballistics, mission persistence on unloaded chunk, no free rearm on a damage-flee
  - `e8e4791` — stand up the Director `FireMissionManager`, wire the howitzer as its first client
  - `2ccbb54` — detailed M109 howitzer model + extended range
  - `208d0fe` — crate unpacking, combat ammo/power retreat, carry-to-capacity logistics
  - `1514986` / `d029350` — cargo/ammunition block family + art passes

## Shipped this session

- **Cargo/ammunition block family** (`common`): `CrateBlock`, `AmmoBoxBlock`, `AutocannonMagazineBlock`, `ArtilleryShellBlock`, `PowerCellBlock`. Sea-pickle-style clustered stacking (`COUNT` 1–4). Cook-off: ignites and explodes from flame projectiles, lava, and fire neighbours, with blast power scaling per unit in the cluster. Recipes (gunpowder + iron; power cell = redstone + copper + iron), loot tables, lang entries. 3D outlined models with the faction palette + cyan accents; the crate is metal, rectangular, and shorter than the others.
- **`CrateBlockEntity`**: 5-slot inventory, an unpack-over-time countdown, folds its contents into an adjacent `StorageDeckBlockEntity` (new `offer()`) or scatters them. NOTE: nothing fills a crate yet — see Outstanding.
- **Combat drone ammo/power retreat gate**: onboard magazine (`getMagazineSize`/`getRounds`/`consumeDroneRound`/`refillRounds`/`hasAmmo`) plus battery charge; a unit that runs out of ammo or sags to low charge breaks off to resupply and returns topped up. Gated units: Medium / Heavy / Sniper fliers. A get-hit flee does NOT earn the free resupply (fixed this session).
- **Logistics carry-to-capacity**: `AmmoRunGoal` batches up to 3 loads per sortie.
- **Howitzer**: detailed M109 Paladin model (58 cuboids); gun range 112 blocks (~7 chunks), follow range 120.
- **Artillery fire-mission system** (`FireMissionManager`, a Director sibling): three target sources — observer relay (recon `AlertState` TRACKING/ENGAGING), a dwell heatmap (per-chunk decaying grid of where players linger), and counter-battery (a far-off player who shot a drone). Accuracy is a range-floored CEP scaled by chunk distance, tightened by spotting and a per-shot ranging walk-in. Missions and the dwell grid persist in NBT. The datapad shows an incoming-fire warning (bearing + ETA).

## Outstanding work

### Review findings not yet fixed (2026-07 Fable sweep)

- **Mortar shares the old drag flaw** (LOW): `MortarAttackGoal` still uses the naive `dx / FLIGHT_TICKS` constant-speed backfill and lands ~5–8 blocks short at its 40-block range. The howitzer's fix (a drag-aware ballistic solve in `HowitzerAttackGoal.fire`) could be factored into a shared helper and applied to the mortar. Less visible than on the howitzer because of the short range.
- **`AmmoRunGoal` depot-stock leak** (SUSPECTED, low): a batched sortie debits the `SupplyNetwork` ledger per load during `PICKING_UP`, but if the goal stops mid-sortie (drone flees, target weapon dies/unloads, goal preempted), `stop()`/`start()` discard `carriedLoads` without crediting the depot back — silent stock loss (up to 3 loads now, vs 1 before batching). Fix: return unspent `carriedLoads` to the depot in `stop()`.
- **Datapad incoming-fire ETA reads early** (COSMETIC): `DatapadItem.incomingFor` computes the closest-approach time from the shell's current velocity as if constant, but horizontal velocity decays 1%/tick, so true arrival is later than shown. Display-only.

### Deferred integrations

- **Crates aren't drone-filled**: `CrateBlockEntity.setCargo` has no callers and nothing places a `CrateBlock` programmatically, so the unpack → StorageDeck pipeline never runs in-game. Needs a logistics drone to place and fill crates on arrival (ties into ROADMAP Phase 2 "wheeled logistics drone + crates").
- **Ammo/power gate for ground gun units**: the IFV and GroundDrone are ungated (`getMagazineSize() == 0`); only Medium/Heavy/Sniper fliers carry an onboard magazine + retreat gate. Decide whether ground units should too.

### Deferred artillery slices (see `ARTILLERY_AND_INDIRECT_FIRE.md`)

- Terrain saturation / erosion accumulator (repeated impacts degrade the ground).
- Off-screen statistical resolution: resolve fire missions abstractly in unloaded chunks (no player near).
- Battery fire: multiple tubes coordinated on a single mission.
- Additional artillery types: mortar rework, naval bombardment (ship guns), MLRS / rocket artillery, guided missiles, close air support.
- Shell-family generalization: a shared shell base across mortar / howitzer / etc.

### Other long-standing items

- Full audio SFX set wired into `sounds.json` (custom recordings replacing the vanilla placeholders — also ROADMAP Phase 1).
- Gunship detail-model pass (bring it up to the IFV / howitzer detail level).
- Research tree (player reverse-engineering; see `RESEARCH_TREE.md` / `RESEARCH_AND_LOGISTICS.md`; ROADMAP Phase 4).

## Environment notes for resuming

- Gradle can't run locally (the proxy blocks the distribution download). CI is compile-only — verify via GitHub Actions on the branch, never locally.
- 1.21.1 Yarn API gotchas: `onStateReplaced` is the 5-arg overload; recipe result is `{"id","count"}` and ingredients use the object form `{"item"}` / `{"tag"}`; data folders are singular (`recipe` / `loot_table` / `advancement`); `DustParticleEffect(Vector3f, float)`.
- Model pipeline: scratchpad Python generators (`gunship_sculpt.py` / `detailed_ifv.py` / `detailed_cargo.py`) → source geometry + UV atlas + texture + Java `getTexturedModelData()`. Front = -Z, up = -Y (Minecraft convention).
- Commit trailer convention is set by the environment; never put a model identifier in commits, PRs, or code.
