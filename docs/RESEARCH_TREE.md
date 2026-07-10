# Program 7 — Research Tree Framework

> `FRAMEWORKS.md` names `ResearchTree` the system that actually opens Tier 4+:
> it precedes the gunship and the terror weapons because those tiers are
> gated by *research*, not by threat alone. This is its **buildable manager
> architecture** — the manager's state, the node schema, the adaptive-focus
> engine, and the psionic building that embodies and exposes all of it. The
> *vision* (why research exists, the adaptive-focus fantasy, the worked
> gunship chain) lives in `RESEARCH_AND_LOGISTICS.md` (layer 1) and
> `DESIGN.md`; this is the engineering skeleton that makes it real, to the
> same bar as `ARTILLERY_AND_INDIRECT_FIRE.md`.

## 1. The core idea

**Research is what actually opens the next tier — not a threat threshold, and
not a calendar.** `ProgramDirectorState.tier2Unlocked()` / `tier3Unlocked()`
today read raw counters (`globalThreat`, `scansCompleted`). Those counters
don't go away — they become **prerequisites that make a node researchable** —
but the tier itself doesn't flip until the Program has *actually finished
studying it*, tracked as a visible progress meter on a **physical building**.

That single move — research embodied in a building instead of an invisible
threshold — is what makes the whole system fair and disruptable:

- It has a **place** (the psionic research building, §5) → a concrete thing
  to find and hit, mirroring `ProbeCoreBlockEntity`'s HP/under-attack model.
- It has a **meter, not a countdown** → the player watches it climb instead of
  racing an invisible clock, and can watch it *stall* when they've hurt it.
- It is **adaptive** (§4) → the Program researches counters to whatever is
  actually hurting it, reusing the weapon-profiling the Director already
  keeps on every player.
- It is **starvable** → like every other Program system, it draws on the
  ledger, and an empty ledger stalls it exactly like an empty ledger silences
  the dispatch queue.
- It has a **player-side mirror** → the first-base-kill psionic unlock (§9)
  is the same fantasy running in reverse.

## 2. Which backbones it reuses

Per `FRAMEWORKS.md`'s bar, `ResearchTree` invents no new private systems:

| Backbone | How research uses it |
|---|---|
| **Ledger** | Every step of progress drains a material cost from `ProgramDirectorState`'s resource map via `tryConsume`, exactly like `ConstructionSite`'s `BUILD_STEP_COST`. No ledger, no progress. |
| **Manager pattern** | `ResearchTree` is a sibling organ of `VirtualFleet` and the dispatch queue — its own state, ticked once per overworld tick, NBT-persisted. |
| **Perception / weapon profiling** | Adaptive focus (§4) reads the *same* `PlayerIntel.weaponProfile` / `elytra` fields `dispatchEscalation`/`dispatchInterception` already consult — no second profiling system. |
| **Datapad** | The focus node + progress meter is a new field on `DatapadSnapshotPayload.Header`, the same channel that already carries posture/heat/tier (§8). |
| **Construction blueprint machinery** | The building itself is *built* by reusing `ConstructionSite`/`tickConstructionSites`'s incremental blueprint placement (the same code that grows a supply depot) — no new "watch it rise" system. |
| **Tier gating** | `tier2Unlocked()`/`tier3Unlocked()` become thin wrappers that ask `ResearchTree` whether the relevant capstone node is `UNLOCKED` (§7), instead of comparing counters directly. |
| **Block-entity + disruption pattern** | The building follows `ProbeCoreBlock`/`ProbeCoreBlockEntity` exactly: HP, an under-attack window, a destruction hook reported to the Director. |

If a later pass wants materials physically *hauled in* by drones instead of
drawn straight from the ledger, that's a `SupplyNetwork` integration (a convoy
feeding the building's local stock) — not a reason for `ResearchTree` to grow
its own transport layer.

## 3. The `ResearchTree` manager

A new class, `dev.rheava.program7.director.ResearchTree`, owned by
`ProgramDirectorState` next to `virtualFleet`:

```java
private final ResearchTree researchTree = new ResearchTree();
```

**State it holds:**

- `Map<String, NodeStatus> nodeStatus` — every node's current state:
  `LOCKED` (prerequisites unmet) → `AVAILABLE` (prerequisites met, not yet
  chosen) → `FOCUSED` (the one currently accruing progress) → `UNLOCKED`
  (done, capability live).
- `String focusedNodeId` (nullable) — the single node currently being
  studied. Only one node is ever in progress at a time — this is the
  "single strategic choke point" the vision locks in.
- `int focusProgress` / `int feedCooldown` — the same two-field cadence as
  `ConstructionSite.progress`/`buildCooldown`: a step counter and a countdown
  to the next step attempt.
- `Map<String, Float> counterPressure` — a decaying score per **counter tag**
  (`RANGED`, `MELEE`, `ELYTRA`, `FORTIFICATION`, …), fed by player behavior
  and read by the focus-selection pass (§4).
- `@Nullable BlockPos buildingPos` — where the psionic research building
  currently stands, or `null` if it hasn't been built yet (or was destroyed
  and needs rebuilding).
- `long lastFocusEvalTime` — throttles focus re-evaluation the same way
  `BASE_DEFENSE_INTERVAL` throttles base-defense checks; research priorities
  don't need to be recomputed every tick.

**Tick** (`researchTree.tick(ServerWorld, ProgramDirectorState)`, called from
`ProgramDirectorState.tick` right after the construction-site pass):

1. If no `buildingPos` and a construction slot is free, queue the psionic
   research building as a buildable blueprint (reuses `tryFoundConstructionSite`
   — see §5).
2. If a building stands: check its block entity's `isUnderAttack()`/HP. If
   healthy, attempt a feed step — `tryConsume(currentNode.stepCost())` — and
   on success increment `focusProgress`. If it completes the node's
   `totalSteps`, flip status to `UNLOCKED`, re-derive which newly-`AVAILABLE`
   nodes unlocked, and clear `focusedNodeId`.
3. Every `FOCUS_EVAL_INTERVAL` ticks (coarse, minutes not seconds): re-run the
   adaptive priority pass (§4) over all `AVAILABLE` nodes and re-pick
   `focusedNodeId` if a new node clearly outscores the current one.
4. Decay `counterPressure` slightly every interval, same shape as
   `HEAT_DECAY_INTERVAL` — old behavior stops dominating the read the longer
   it goes unrepeated.

**Persistence:** a `ResearchTree` NBT tag inside `ProgramDirectorState`'s own
`writeNbt`/`fromNbt` — `nodeStatus` as a compound of id→ordinal, `counterPressure`
as a compound of tag→float, plus the scalar fields — exactly the shape
`VirtualFleet.toNbt`/`readNbt` already uses as a sub-tag. A node mid-progress
persists its `focusProgress`/`feedCooldown` so a server restart resumes the
meter instead of losing it (same persistence discipline as in-flight fire
missions and pod descents).

## 4. The tree data model

Every node is one row in the same schema — adding a node is data, not a new
system:

| Field | Meaning |
|---|---|
| **id** | stable string key, NBT-safe (`"tier2_hardware"`, `"armor_plating_kinetic"`) |
| **prerequisites** | list of node ids that must be `UNLOCKED` (or, for the tier-capstone nodes, raw Director counters — see §7) before this node goes `AVAILABLE` |
| **unlockTarget** | what flips live on completion: a unit class becomes fieldable, a tier gate opens, a counter-tactic activates |
| **stepCost** | ledger cost per feed step (mirrors `BUILD_STEP_COST`) |
| **totalSteps** | how many successful feed steps complete it — the "how long" knob, tuned per node weight |
| **counterTags** | zero or more tags from `counterPressure` this node answers — this is what makes it *biddable* by adaptive focus |
| **basePriority** | a small constant baseline score so untagged capstone nodes (tier gates) still get picked in the absence of any player pressure |

Illustrative nodes (not exhaustive — the full roster is a later content pass):

| id | unlockTarget | counterTags |
|---|---|---|
| `tier2_hardware` | flips `tier2Unlocked()` true | — (capstone; gated by prereqs below) |
| `tier3_hardware` | flips `tier3Unlocked()` true | — (capstone) |
| `standoff_harasser` | fields a ranged harasser sooner/cheaper | `MELEE` |
| `swarm_doctrine` | cheaper/faster explosive-drone swarms | `RANGED` |
| `interception_optics` | unlocks the elytra-hunting interceptor drone | `ELYTRA` |
| `area_denial_ordnance` | unlocks explosive/rocket saturation fire | `FORTIFICATION` |
| `wall_breaker_shells` | unlocks the howitzer's heavy-shell munition (`FireMissionManager` roster, §4 of the artillery doc) | `FORTIFICATION` |
| `enchant_analogue` | opens the enchanting research branch | — (bait-only, no counter tag) |

**Adaptive focus is a scoring pass, not a rewrite of the tree.** Every
`FOCUS_EVAL_INTERVAL`, for each `AVAILABLE` node:

```
score(node) = node.basePriority
            + Σ counterPressure[tag] for tag in node.counterTags
```

The highest-scoring `AVAILABLE` node becomes (or stays) `focusedNodeId`.
**Hysteresis matters:** don't switch off a node that's more than, say, 60% fed
unless a challenger's score beats it by a real margin — otherwise focus
flickers every time a player takes one arrow shot, and "bait the Program's
focus away" (the vision's core counterplay) stops reading as a deliberate
choice you can force and starts reading as noise.

**Feeding `counterPressure` off the weapon-profiling that already exists:**
`recordScan` already writes `PlayerIntel.weaponProfile` and `.elytra`. Add one
line to that same method: bump `counterPressure[tagFor(profile)] += riskTier`
(and `counterPressure[ELYTRA] += riskTier` if `elytra`). No second profiling
pass — this is the *existing* per-scan signal, just also feeding the research
prioritizer instead of only the dispatch composition. `FORTIFICATION` pressure
is the one new signal worth adding: bump it whenever `tickBaseDefense` finds a
core under sustained attack without the attacker being killed quickly — a
crude but grounded "they're digging in / grinding my base" tell.

## 5. The psionic research building

Follows `ProbeCoreBlock`/`ProbeCoreBlockEntity` exactly, because that pair
already models everything a disruptable Program structure needs:

- **`PsionicResearchBlock`** (a `BlockWithEntity`, same shape as
  `ProbeCoreBlock`): `onBlockBreakStart` calls
  `PsionicResearchBlockEntity.markDamaged(now)`; `onStateReplaced` reports a
  real destruction to the Director via a new
  `ResearchTree.onBuildingDestroyed(ServerWorld, BlockPos)` hook.
- **`PsionicResearchBlockEntity`**: `hp`, `lastDamagedTick`,
  `isUnderAttack(now)` — copy-pasted shape from `ProbeCoreBlockEntity`, same
  regen-when-not-attacked rule. This is the block `ResearchTree.tick` polls
  each cycle before attempting a feed step (§3 step 2).
- **Built, not spawned:** the building is a `ConstructionSite` blueprint
  (§2), grown incrementally the same way a supply depot rises — this is the
  vision's "watch a helipad rise" line made literal, and it's free
  architecture since `tickConstructionSites` already exists. It becomes
  buildable once the Program has a core site and a free construction slot,
  same gate as any other outpost.
- **One building matters at a time** for v1 — `ResearchTree.buildingPos`
  is a single nullable field, not a list. A multi-base game could research
  from any surviving building later; that's an open question (§10), not a v1
  requirement.

**Disruption is graded, and destruction is a real setback, not a wipe:**

- **Damaging it** (`isUnderAttack` true): feed steps simply don't attempt
  while it reads as under attack — research pauses, doesn't reverse. This is
  the "hit it and progression stalls" promise from the vision, done with the
  same window `ProbeCoreBlockEntity` already uses.
- **Destroying it**: `onBuildingDestroyed` clears `buildingPos`, **halves**
  `focusProgress` on whatever node was mid-flight (a real cost, not total
  annihilation — mirrors how `onBaseDestroyed` drops loot rather than just
  erasing a base), and the Program must queue a fresh construction site
  before research resumes at all. This is the single choke point the vision
  calls out — sever it and the *entire* tree stalls, not just the current
  node.

## 6. Counterplay & the fairness contract

The same four-answers shape as artillery (§5 of `ARTILLERY_AND_INDIRECT_FIRE.md`),
translated to research:

- **Find the building and hit it** — pauses progress; find it and *destroy*
  it — costs real accumulated progress and forces a rebuild.
- **Starve the ledger** — the feed step is a real `tryConsume`; a Program
  bled dry by raiding its storage or cutting a supply line simply stops
  accruing, same lever as everything else.
- **Destroy what it's studying** (vision-locked, later phase): if research
  nodes eventually key off analyzing a specific captured block/unit/wreck
  (per `RESEARCH_AND_LOGISTICS.md`), removing that object denies the input.
  Not required for the v1 build order (§10) — flagged so the node schema
  doesn't have to be revisited when it lands.
- **Bait the focus away** — change your loadout/tactics and watch
  `counterPressure` (and the datapad's focus readout, §8) shift toward a
  different node, provided you clear the hysteresis margin (§4).

## 7. The gating hook

`tier2Unlocked()`/`tier3Unlocked()` stop being direct counter comparisons and
become:

```java
private boolean tier2Unlocked() {
    return this.researchTree.isUnlocked("tier2_hardware");
}
```

The existing counters don't disappear — they become the **prerequisites**
that flip `tier2_hardware` from `LOCKED` to `AVAILABLE` in the first place:

```
tier2_hardware.prerequisites = globalThreat >= TIER_2_THREAT
                             || scansCompleted >= TIER_2_MIN_SCANS
```

So the shape of escalation the player already experiences is preserved
(enough hostile contact before Tier 2 is even *on the table*), but the actual
flip to "Tier 2 units now field" is delayed until the Program has finished
studying it — which the player can watch coming on the datapad (§8) and can
delay by hitting the building. `tier3_hardware` follows the same pattern,
gated behind `tier2_hardware` being `UNLOCKED` plus the existing
`TIER_3_THREAT` prerequisite. `currentTierEstimate()` (the datapad's coarse
tier readout) switches from reading raw threat to reading
`researchTree`'s highest-unlocked capstone, so the number on the datapad and
the number that actually governs dispatch are finally the same number.

## 8. Datapad integration

Extend `DatapadSnapshotPayload.Header` with two fields, the same flat style
its existing scalars already use:

```java
public record Header(String posture, int heat, int fleetEstimate, int tier,
        float baseYaw, int baseDistance,
        String researchFocus, float researchProgress) { ... }
```

- `researchFocus` — the focused node's id (client-side maps ids to display
  strings, same as the `guess` int already maps to a type label for
  contacts), or `""` if nothing is focused (building destroyed/not yet
  built).
- `researchProgress` — `focusProgress / totalSteps`, 0..1, so the client can
  draw a meter, not a countdown — no ETA is ever shown, matching the "no hard
  timer" lock from `RESEARCH_AND_LOGISTICS.md`.

Once the building has been scouted, it also earns a `Contact` blip (a new
`guess` value alongside gun/recon/logistics) so it shows up on the radar as a
findable position, not just a status line — turning "the Program is close to
fielding wall-breaker shells" into a place you can walk to.

## 9. Player mirror

The **first-base-kill psionic unlock** (`ProgramDirectorState.onFirstBaseKilled`,
already shipped) is the symmetric player-side tree, not a separate system to
design from scratch:

- The Program's research draws on its **psionic connection**; severing that
  relay at the first base kill is *literally* the player seizing a slice of
  the same power (`DESIGN.md`: "you seize part of that psionic power as your
  own psionic unlock").
- Where the Program's tree is adaptive and building-gated, the player's is a
  **single milestone unlock** rather than a full mirrored tree in v1 — that
  asymmetry is intentional: the Program grinds for its power over the whole
  game, the player earns theirs in one decisive strike. A full player-side
  tree (multiple psionic unlocks gated by subsequent base kills / re-insertion
  cycles) is a natural post-v1.0 extension once endless-wave re-insertion
  gives the player more than one base to crack.
- Both trees key off the same config flag (`Psionics: on/off` in `DESIGN.md`'s
  configuration table) — turning it off disables the Program's psionic
  research building's *narrative* framing but not its function; it disables
  only the player's unlock, per the existing config note.

## 10. Build order

1. **`PsionicResearchBlock`/`PsionicResearchBlockEntity`**, copy-pasted from
   `ProbeCoreBlock`/`ProbeCoreBlockEntity` (HP, under-attack window, a
   destruction hook). *Lowest risk — it's a known pattern, not new code
   shape.*
2. **`ResearchTree` manager with exactly one node** (e.g. `tier2_hardware`)
   and the building built via the existing `ConstructionSite` blueprint path.
   Proves the whole loop end to end: building rises → feeds off the ledger →
   meter climbs → damaging the building pauses it → destroying it costs
   progress → node completes and *something* changes. This is the cheapest
   client that proves the architecture, same spirit as artillery's "mortar
   first" and supply's "one upkeep class first."
3. **Wire `tier2Unlocked()`/`tier3Unlocked()`** to read `researchTree`
   instead of raw counters (§7) — now research is genuinely what opens the
   tier, not a display estimate alongside it.
4. **Expand the node table** (§4) with the counter-tactic nodes and their
   `counterTags`, and turn on the `counterPressure` feed from `recordScan`.
   Proves adaptivity on real player data before building the full tree.
5. **Adaptive focus scoring + hysteresis** (§4) — the re-evaluation pass and
   the switch-cost margin. Without hysteresis this step is worse than not
   having it (flickering focus reads as broken, not smart).
6. **Datapad fields** (§8): `researchFocus`/`researchProgress` on the header,
   the building's `Contact` blip once scouted.
7. **Fill out the tree**: the full roster from `RESEARCH_AND_LOGISTICS.md`
   (aircraft-frame → large-rotor → ducted-tiltrotor for the gunship, the
   enchanting branch, wall-breaker shells feeding `FireMissionManager`) — by
   this point it's data entry against a finished skeleton, not new
   architecture.
8. **(Later, optional) `SupplyNetwork` integration**: materials physically
   hauled to the building by convoy instead of drawn straight from the
   ledger — only worth doing once `SupplyNetwork` itself is built, per its
   own doc's build order.

## 11. Open questions

- **Multi-base research**: if the player lets several bases stand, does each
  grow its own building/tree, or is there one Program-wide tree with one
  building at a time (this doc's v1 assumption)? Lean toward the latter until
  multi-base play is actually common.
- **"Destroy what it's studying"**: the vision calls this out as a locked
  disruption vector, but it presumes nodes reference a specific analyzed
  object (a captured block/unit/wreck). Not modeled in the node schema above
  (§4) — needs its own field (`studyTargetRef`) once that mechanic is
  designed, not before.
- **Hysteresis tuning**: how big a score margin should force a focus switch,
  and how "fed" does a node have to be before it counts as committed? Same
  category of question as the artillery doc's CEP constants — needs
  playtesting, not more architecture.
- **Node failure state**: can a node be permanently abandoned (Program gives
  up on a branch it can't get resources for), or does it just sit `AVAILABLE`
  forever losing focus contests? Leaning toward the latter — simpler, and an
  `AVAILABLE`-forever node is itself legible information ("the Program wants
  this but never wins the priority fight for it").

Nothing here fields a single new unit. Every unlock still runs through the
existing dispatch/`tryConsume` path — `ResearchTree` only decides *whether*
`tier2Unlocked()`/`tier3Unlocked()` (and, later, individual node-gated
capabilities) return true. The tree is the lock; the ledger is still the
gun.
