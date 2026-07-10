# Program 7 — Supply Lines: First Slice Spec (fuel + the air leash)

> `SUPPLY_NETWORK.md` is the framework; this is the **buildable first slice**
> with every knob given a number. Scope follows the framework's build order
> steps 1–2: an **UpkeepProfile with degradation on the air class** (the
> battery-center brownout, generalized) plus **one depot block — the fuel
> plant** — with nearest-in-range sourcing and the `SupplyNetwork` manager.
> A Sonnet coder should be able to implement this doc top to bottom without
> asking a question. Ammo plants, airbases, convoy-scheduled links, research
> hooks, and off-screen statistical supply are **explicitly deferred** (§8).

## 1. Scope of the slice (and what it proves)

**In:** fuel upkeep on the four Tier 2/3 fliers that already exist
(`MediumAttackDroneEntity`, `SniperDroneEntity`, `HeavyAttackDroneEntity`,
`ReconHelicopterEntity`), a `FuelPlantBlock` planted at every probe core and
every completed outpost, per-cycle nearest-depot sourcing, a brownout-style
degradation tell, and a `SupplyNetwork` organ on `ProgramDirectorState`.

**Out (deferred, §8):** ammo plants / `FireMissionManager` throttle, airbases,
`SupplyLink` convoy scheduling, off-screen statistical resolution, datapad
supply panel, upkeep on ground/naval units.

**What it proves:** the whole loop in miniature — *kill the fuel plant, watch
the heavies over your head go from apex to limping on a timer you can count.*

**Exemptions (fairness by design):** Tier 1 `AttackDroneEntity` runs on the
void engine — no upkeep, the baseline threat never starves. Couriers
(`LogisticsDroneEntity`, `WheeledHaulerEntity`, `TransportDroneEntity`) and
`BatteryCenterEntity` are exempt: they *are* the logistics layer, and starving
the supply system's own vehicles creates an unrecoverable death spiral.
Virtualized units (`VirtualFleet` tokens) pay nothing in this slice — upkeep is
only sampled on live, loaded entities (the framework's §8 lean: query loaded
chunks; statistical for the rest comes with the off-screen slice).

## 2. UpkeepProfile — the numbers

One new record, `dev.rheava.program7.director.UpkeepProfile`:

```java
public record UpkeepProfile(String supplyType, int drainPerCycle, int graceCycles) {}
```

- **Cycle length: `SupplyNetwork.CYCLE_TICKS = 200`** (10 s). Coarse on
  purpose — upkeep is economy, not physics. Same order as the assembler's
  `EVALUATE_INTERVAL` (100) and the Director's own 200-tick site checks, so
  nothing new is hot-ticking.
- **Sourcing rule (per unit, per cycle):** try to draw `drainPerCycle` FUEL
  from the nearest in-range depot. **Success → grace refills** (+3 cycles,
  capped at `graceCycles`). **Failure → burn 1 grace cycle.** Grace at 0 →
  **degraded** (§5). First successful draw lifts degradation immediately.
- **Drain is proportional to build cost** so upkeep never dwarfs or trivializes
  the dispatch economy. Total ledger units per build (from
  `ProgramDirectorState`): medium 11, sniper 12, heavy 28, recon heli 28 —
  Tier 3 costs ~2.4× Tier 2, so it drains 2×.

| Unit | Tier | Build cost (ledger) | supplyType | drainPerCycle | graceCycles | Full-power endurance off-depot |
|---|---|---|---|---|---|---|
| Attack drone | T1 | 4 iron, 4 gunpowder, 2 redstone | NONE | — | — | infinite (void engine) |
| Medium attack drone | T2 | 6 iron, 2 gunpowder, 3 redstone | FUEL | 1 | 15 | **150 s** |
| Sniper drone | T2 | 5 iron, 3 copper, 4 redstone | FUEL | 1 | 15 | **150 s** |
| Heavy attack drone | T3 | 12 iron, 6 copper, 6 redstone, 4 gunpowder | FUEL | 2 | 9 | **90 s** |
| Recon helicopter | T3 | 14 iron, 8 copper, 6 redstone | FUEL | 2 | 9 | **90 s** |

This matches the framework's tier table: T1 unaffected, T2 mild (long leash,
mild slowdown), T3 real (short leash, hard brownout). T4 doesn't exist yet.

**The fairness target, stated:** *cutting a Tier 3 unit's supply degrades it in
at most 90 seconds — about one more engagement beat, long enough that killing
the depot mid-fight is a plan rather than an instant win — and recovery takes
~10 s to lift the debuff and ~30 s to rebuild full reserves once back in
supply, so a brief gap is a scare, not a death sentence.* Tier 2's 150 s keeps
its dependency real but forgiving — mediums are the workhorse and shouldn't
feel bugged the moment they leave home.

**Staggering:** each unit runs its upkeep check when
`(entity.age + entity.getId()) % CYCLE_TICKS == 0` — one check per unit per
10 s, spread across ticks, no synchronized spike.

## 3. The fuel plant (the one depot block)

**`FuelPlantBlock` + `FuelPlantBlockEntity`**, mirroring the
`AssemblerBlock`/`AssemblerBlockEntity` split exactly (a `BlockWithEntity`
shell; behavior in the block entity; `serverTick` via `validateTicker`).

| Knob | Value | Rationale |
|---|---|---|
| `Block.Settings` | `.strength(10.0f, 250.0f)`, `requiresTool()`, metal sounds/map color copied from `ASSEMBLER`'s registration | Between the autogun turret (8/200) and the assembler (12/300): a real demolition target for an iron-pick player (~15 s of mining under fire), not a drive-by freebie, and creepers alone won't erase it. |
| **Supply type** | `SupplyNetwork.SUPPLY_FUEL = "fuel"` | Constants live on `SupplyNetwork`, not `Resources` — depot stock is depot-local, never a ledger key. |
| **Provision radius** | **64 blocks** (sphere, center = block pos) | Wider than the battery center's 32-block aura and the assembler's 48-block complement ring: one plant covers a whole base footprint and its siege perimeter, but nowhere near two bases. Kiting a heavy 65+ blocks off its depot is itself a (slow) counter. |
| **Stock capacity** | **240 FUEL** | 40 cycles (~6.6 min) of a full T3-led siege (§6 math) with the ledger already dry — a razed economy still gets a last stand, not an instant global off-switch. |
| **Refill (this slice)** | Converts **1 COAL → 8 FUEL, at most 1 coal per cycle** (so +8 FUEL/cycle max), paid via `director.tryConsume(Map.of(Resources.COAL, 1))` whenever `stock <= capacity - 8` | Ledger-direct refill is the slice-1 simplification; the convoy slice (§8) replaces it for forward plants. Coal is the obvious fuel feedstock and already in the pod stockpile (64) and orbital resupply. |
| **Planting** | (a) `deployProbeAt` gains `placeFuelPlant(world, pos)` — same terrain scan as `placeAssembler`, at horizontal **offset 6**, fallback `core.west(6)` (assembler sits at 3, storage deck 4, catapult 5 — no collisions). (b) When a `ConstructionSite` completes in `tickConstructionSites`, place a fuel plant at `site.origin.up()` (the blueprint's open center hatch) and register it. | Every core and every finished outpost is a depot. Outposts (founded 60–140 blocks out) are how the Program *earns* sustained T3 reach toward you — and razing them is how you deny it. |
| **On break** | `onStateReplaced` → `SupplyNetwork.removeDepot(pos)` + spill `floor(stock / 16)` coal (cap 15) via `ItemScatterer` | Killing infrastructure pays a little salvage, like shooting down a courier — interdiction is always slightly rewarded, never just denial. |
| **Registration** | `P7Blocks.FUEL_PLANT`, `P7BlockEntities.FUEL_PLANT` | Mirrors `ASSEMBLER` / `STORAGE_DECK` entries verbatim. |

**Stock ownership:** the `SupplyNetwork.Depot` record is the single source of
truth for `stock`, persisted in the Director's NBT. The block entity is a thin
shell: it registers the depot on first server tick if absent, plays the working
tell (a low `ASSEMBLER_WORKING` hum + `CAMPFIRE_COSY_SMOKE` every 40 ticks
while stock < capacity and coal is flowing), and deregisters on break. This
keeps stock alive when the chunk unloads and makes the later statistical slice
free. Each manager cycle also prunes any depot whose chunk is loaded but whose
block is no longer a fuel plant (the `pruneDeadCoreSites` pattern) — pistons,
explosions, and `/setblock` can't leave ghost depots.

## 4. Sourcing — how a unit finds its fuel

All on `ProgramDroneEntity` (base class), driven by a new overridable hook:

```java
@Nullable
protected UpkeepProfile upkeepProfile() { return null; }   // base: exempt
```

The four fliers override it (medium/sniper return `(SUPPLY_FUEL, 1, 15)`;
heavy/recon return `(SUPPLY_FUEL, 2, 9)`). Per staggered cycle (§2), a unit
with a non-null profile calls:

```java
SupplyNetwork net = ProgramDirectorState.get(world).getSupplyNetwork();
boolean fed = net.drawSupply(this.getBlockPos(), profile.supplyType(), profile.drainPerCycle());
```

`drawSupply` finds the **nearest depot** of that type whose radius covers the
position **and whose stock covers the full amount** (no partial draws — a dry
depot reads as dry), debits it, and returns success. On success: `graceLeft =
min(graceLeft + 3, graceCycles)`. On failure: `graceLeft--`, floored at 0.
Units spawn with **full grace** — a dispatched wave arrives fueled for one
sortie's worth of fighting (90 s / 150 s), then must be inside a depot bubble
to keep its edge. `graceLeft` persists in entity NBT (`"UpkeepGrace"`, with
"absent = full" as the load default so old saves don't spawn pre-starved
units).

## 5. Degradation — what starved air looks like

**Degraded** = `graceLeft == 0`. The tell is the **battery-center brownout,
made continuous** (cite: `BatteryCenterEntity.onDeath` applies SLOWNESS
amp 2 + WEAKNESS amp 1 for 300 ticks + `ELECTRIC_SPARK`). Each cycle while
degraded, the unit reapplies to itself:

- `StatusEffects.SLOWNESS`, amplifier **1**, duration `CYCLE_TICKS + 40`
  (continuous, one notch softer than the death-brownout since it's sustained);
- `StatusEffects.WEAKNESS`, amplifier **0**, same duration;
- 12 `ELECTRIC_SPARK` particles + the unit's ambient sound at pitch 0.6 —
  a visible, audible sputter. Slowness also drags `GENERIC_FLYING_SPEED`
  movement, so fliers read as mushy and easy to lead; the existing
  knockback-**scramble** mechanic (`ProgramDroneEntity.scramble`) becomes far
  deadlier against a slow target, which is the intended synergy — starve it,
  then swat it.

No damage-over-time, no forced landing, no despawn: **supply is a leash, not a
death sentence** (framework §2). Recovery: the first successful draw clears
the flag within one cycle (≤10 s back inside a stocked radius), full grace
rebuilds at +3/cycle (30 s for T3, 50 s for T2).

## 6. The `SupplyNetwork` manager

A Director organ, sibling of `VirtualFleet` — same shape: plain class, owned
field, ticked from `ProgramDirectorState.tick`, NBT round-trip.

```java
public class SupplyNetwork {
	public static final int CYCLE_TICKS = 200;
	public static final String SUPPLY_FUEL = "fuel";

	public static final int FUEL_PLANT_RADIUS = 64;
	public static final int FUEL_PLANT_CAPACITY = 240;
	public static final int FUEL_PER_COAL = 8;

	private final List<Depot> depots = new ArrayList<>();

	/** Coarse cycle: refill depots from the ledger, prune dead ones. Returns true if dirty. */
	public boolean tick(ServerWorld world, ProgramDirectorState director);

	/** Idempotent: re-registering an existing pos is a no-op (keeps its stock). */
	public void registerDepot(BlockPos pos, String supplyType, int radius, int capacity);
	public void removeDepot(BlockPos pos);

	/** Nearest in-range depot of the type with stock >= amount; debit and report. */
	public boolean drawSupply(BlockPos pos, String supplyType, int amount);

	@Nullable public Depot depotAt(BlockPos pos);      // block-entity queries (tells, debug)
	public List<Depot> getDepots();                     // read-only copy, datapad later

	public NbtCompound toNbt();
	public void readNbt(NbtCompound nbt);

	public static final class Depot {
		BlockPos pos; String supplyType; int radius; int capacity; int stock;
	}
}
```

- **State:** the depot list only. Per-unit upkeep lives on the units (§4) —
  the framework's open question resolved toward "query loaded chunks," so
  there is no index to go stale.
- **Tick:** runs its cycle when `world.getTime() % CYCLE_TICKS == 0`: for each
  depot, (1) if its chunk is loaded and the block is gone, remove it; (2) if
  `stock <= capacity - FUEL_PER_COAL` and
  `director.tryConsume(Map.of(Resources.COAL, 1))`, add `FUEL_PER_COAL`.
  Refill runs whether or not the chunk is loaded — the ledger is already
  abstract, and a depot that only refills while watched would punish players
  for exploring.
- **Hook-up in `ProgramDirectorState`:** field
  `private final SupplyNetwork supplyNetwork = new SupplyNetwork();`, getter
  `getSupplyNetwork()`, `if (this.supplyNetwork.tick(world, this)) markDirty();`
  in `tick()` next to the `virtualFleet.tick` call, and
  `nbt.put("SupplyNetwork", ...)` / `readNbt` alongside `"VirtualFleet"`.

**Ledger math (why these numbers hold up).** A serious siege — 1 heavy,
1 recon heli, 1 medium, 1 sniper inside one bubble — drains 6 FUEL/cycle
against a refill ceiling of 8/cycle: the plant keeps up, burning ~0.75
coal/cycle ≈ **45 coal per 10 minutes** of sustained T3 pressure. The pod
lands with 64 coal and orbital resupply runs every 3 days, so heavy air is a
real recurring expense the Program can afford in bursts but not forever —
exactly the "scariest fire is the most starvable" rule, and mining out its
harvesters (the existing ledger counterplay) now also grounds its air force.

## 7. Counterplay & fairness (the contract)

Three levers, each with a distinct speed and skill floor:

1. **Kill the fuel plant** (the direct lever). 10.0 hardness ≈ 15 s with an
   iron pick, ~7 s with unenchanted diamond — a commitment under fire, trivial
   for no one. Bite: T3 fliers inside the bubble brown out in **exactly 90 s**
   (they were topped up, so they burn full grace), T2 in 150 s. You *count
   down* your interdiction working, which is the readable-timer promise.
2. **Starve the coal** (the economy lever, already in the game). Kill
   harvesters / raid the storage deck → the ledger's coal dries up → the plant
   coasts on stock (≤240 FUEL, ≤6.6 min of full siege drain) and then every
   T2+ flier in the world is living on grace. Slower, strategic, no new code.
3. **Kite** (the free lever). Drag a heavy 64+ blocks off its depot and hold
   it there 90 s. Weak alone, by design — it's the floor, not the strategy.

**Fresh player** (pre-iron, Tier 1 threat): unaffected. T1 attack drones have
no upkeep, and a player who hasn't escalated past `TIER_2_THREAT = 8` rarely
sees a fueled unit at all. Nothing in this slice touches the early game.

**Geared player** (Tier 3 threat, the target audience): gains a real objective
loop — scout the base or outpost, crack the fuel plant, fight browned-out
heavies — without the units despawning or the fight ending for free.

**Fair to the Program too:** dispatched waves arrive with full grace, so a
heavy still delivers 90 s of full-power terror anywhere on the map before the
leash shows; sustained pressure *at your base* requires it to found an outpost
nearby (which it already does, 60–140 blocks out) and keep the plant alive —
supply becomes the time-gate above T3 that the framework promises.

**Rejected extremes (and the chosen middle):** grace under ~30 s makes a depot
kill an instant fight-ender and makes every remote T3 sortie arrive pre-nerfed
— degradation would read as a bug. Grace over ~4 min makes interdiction
invisible inside one engagement — upkeep would be decorative. 90 s / 150 s
sits between; drains below 1/cycle or a free (no-coal) refill would make the
economy lever irrelevant, and stock above ~400 would let a dead ledger keep an
air force aloft for a quarter hour. All knobs land where both levers stay live.

## 8. Deferred (named, not designed here)

- **SupplyLink + convoy scheduling** — replace forward plants' ledger-direct
  refill with courier runs (`CourierUnit` cargo already spills on shootdown);
  interdicting the *line*, not just the depot. Next slice.
- **Ammo plant** → `FireMissionManager` scrub throttle. **Airbase** → air
  basing + the gunship. **Off-screen statistical supply** + **datapad supply
  panel** ("hostile units in this sector are running on reserves"). **Ground /
  naval upkeep** once the air precedent is tuned.

## 9. Build checklist (for the implementing coder)

**New files** (all under `common/src/main/java/dev/rheava/program7/` unless noted):

1. `director/UpkeepProfile.java` — the §2 record, three fields, no logic.
2. `director/SupplyNetwork.java` — exactly §6: constants
   (`CYCLE_TICKS = 200`, `SUPPLY_FUEL = "fuel"`, `FUEL_PLANT_RADIUS = 64`,
   `FUEL_PLANT_CAPACITY = 240`, `FUEL_PER_COAL = 8`), depot list, the listed
   method signatures, NBT list of depots
   (`Pos` int-array / `SupplyType` string / `Radius`, `Capacity`, `Stock` ints).
3. `block/FuelPlantBlock.java` — copy `AssemblerBlock`'s structure (codec,
   `createBlockEntity`, `getRenderType`, `getTicker` →
   `FuelPlantBlockEntity::serverTick`); add `onStateReplaced` → `removeDepot`
   + the §3 coal spill (`ItemScatterer`, `floor(stock/16)` cap 15).
4. `block/FuelPlantBlockEntity.java` — `serverTick`: on first tick call
   `registerDepot(pos, SUPPLY_FUEL, FUEL_PLANT_RADIUS, FUEL_PLANT_CAPACITY)`
   (idempotent); every 40 ticks, if the depot is refilling, play
   `P7Sounds.ASSEMBLER_WORKING` at 0.4f + `CAMPFIRE_COSY_SMOKE`. No own stock
   NBT — stock lives in the manager.

**Edits:**

5. `registry/P7Blocks.java` — register `FUEL_PLANT`,
   `.strength(10.0f, 250.0f)` `requiresTool()`, otherwise mirroring the
   `ASSEMBLER` entry.
6. `registry/P7BlockEntities.java` — register `FUEL_PLANT` mirroring
   `ASSEMBLER`.
7. `director/ProgramDirectorState.java` —
   - field + getter + tick call + NBT round-trip per §6;
   - `private static void placeFuelPlant(ServerWorld world, BlockPos core)` —
     clone `placeAssembler`'s scan at offset 6, fallback `core.west(6)`;
     call it from `deployProbeAt` after `placeLaunchCatapult`;
   - in `tickConstructionSites`, at the completion branch (the
     `ASSEMBLER_COMPLETE` sound), `world.setBlockState(site.origin.up(),
     P7Blocks.FUEL_PLANT...)` if replaceable, then register the depot.
8. `entity/ProgramDroneEntity.java` — `upkeepProfile()` hook (default null);
   `graceLeft` field (init to profile's max in first tick or constructor-safe
   lazy init); the §2 staggered cycle check in `tick()` (server side only);
   the §4 draw/refill/burn logic; the §5 degraded effects; NBT `"UpkeepGrace"`
   (absent → full).
9. `entity/MediumAttackDroneEntity.java`, `entity/SniperDroneEntity.java` —
   override `upkeepProfile()` → `new UpkeepProfile(SUPPLY_FUEL, 1, 15)`.
10. `entity/HeavyAttackDroneEntity.java`, `entity/ReconHelicopterEntity.java`
    — override → `new UpkeepProfile(SUPPLY_FUEL, 2, 9)`.

**Assets** (mirror `assembler`'s set):

11. `common/src/main/resources/assets/program7/blockstates/fuel_plant.json`,
    block + item models, item registration wherever the other block items are
    registered, `en_us.json` lang ("Fuel Plant"), and a
    `data/program7/loot_table/blocks/fuel_plant.json` (drops nothing — the
    coal spill in `onStateReplaced` is the loot).

**Acceptance checks:** (a) plant a fuel plant + spawn a heavy attack drone
inside 64 blocks with coal in the ledger → no debuff, depot stock visibly
debited 2/cycle; (b) break the plant → the heavy gains Slowness II + sparks
after 9 cycles (90 s), a medium after 15 (150 s); (c) rebuild/replant → debuff
lifts within one cycle; (d) drain the ledger's coal → plant stops refilling,
goes quiet, and starves after ≤240 stock is drawn down; (e) restart the server
mid-starvation → grace, stock, and depots all persist.

The leash holds only if every knob above ships at these values first and gets
retuned from play, not from theory — change them in one place
(`SupplyNetwork`'s constants, the two profile lines), never inline.
