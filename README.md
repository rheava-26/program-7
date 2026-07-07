# Program 7

A Fabric mod for Minecraft **1.21.1**.

An automated orbital probe program has chosen your world as its testing
ground. Its drop pods land, mine, build and adapt — profiling how you fight
and escalating alongside you. Everything it fields can be shot down, salvaged,
reverse-engineered and turned against it.

- **[Design bible](docs/DESIGN.md)** — full concept, lore, systems
- **[Unit bible](docs/UNITS.md)** — every unit's behavior, counters, salvage,
  and the five Program tech tiers
- **[Roadmap](docs/ROADMAP.md)** — phased implementation plan and status

## What's in the current build (Phase 0 + Phase 1 core)

- **Drop pod insertion** — 2–3 in-game days in, a pod comes down 300–600
  blocks from a random player: roaring descent, smoke-and-flame trail,
  vanilla-style explosion on impact, and a placed **Probe Core**. Landings
  nobody is around to see resolve instantly with a distant boom. Force one
  with `/program7 land [distance]`.
- **Probe Core block** — iron-pick tier, very tough, glows faintly; cracking
  it drops heavy salvage and iron.
- **Attack Drone** — the Tier 1 response: fast flyer that chases you down,
  arms at contact range (accelerating beeps), and detonates. Once armed it's
  committed — kiting it into terrain or enemies is legitimate counterplay.
  Shoot it down before it arms to salvage its warhead.
- **Scan → response loop** — once the pod is down, every completed surveyor
  scan musters a tier-scaled attack drone dispatch on the scanned player.
- **Psionic interference** — a violet hue-shift creeps in from the screen
  edges whenever Program hardware is within ~3 chunks, scaling with how
  dangerous the nearby units are (an armed attack drone hits much harder
  than a surveyor), with bursts of audio static. Optional directionality
  (threat-facing edge glows hotter) via config.
- **Config** — `config/program7.json`: overlay/directionality, item
  stealing, structure interference, terrain destruction, ceasefire,
  endless waves (several are forward declarations for later phases).
- **"Still Here" advancement** — survive seven in-game days after landfall.
- **Resource economy v1** — every pod lands with a stockpile (iron, copper,
  redstone, coal, gunpowder). Attack drone dispatches cost real resources;
  when the ledger can't pay, nothing launches. Check it with
  `/program7 status`.
- **Item theft** — surveyors snatch unattended dropped items and carry them
  as cargo. Shoot the thief down and it all spills back out. Config-gated.
- **Harvester Drone** — the economy on wheels. Finds ore and logs (by block
  tag), grinds them out with visible crack stages, hauls a full hopper back
  to the probe core and deposits into the ledger. Unarmed; panics when hurt.
  Two arrive with every pod — killing haulers starves future responses.

- **Surveyor Drone** — the Program's flying recon unit. It closes in on
  players, sweeps them with an accelerating scan (the beeping speeds up as it
  finishes — that's your window to break line of sight or shoot it down),
  hits you with psionic interference when the scan completes, then retreats.
  Spawn it with the spawn egg from the *Program 7* creative tab.
- **Program Director** — persistent per-world brain. Every completed scan
  files an intel report (risk tier, weapon profile, elytra flag, death count)
  and raises the global threat level. Future phases read this state to decide
  what the Program sends after you.
- **Risk assessment** — the doc's tier table implemented: naked frequent
  diers are Tier 1; iron-grade fighters Tier 2; diamond+ Tier 3 (frequent
  deaths demote you — you're not a priority target, however shiny).
- **Salvage items** — drone core, power bank, transmitter, gun barrel,
  explosive warhead, magazine. Surveyors drop the recon-flavored ones.
- **Sound scaffolding** — all drone sound events are registered and currently
  redirect to vanilla sounds; real recordings drop into
  `assets/program7/sounds.json` with zero code changes.
- **Debug commands** — `/program7 status`, `/program7 assess`,
  `/program7 threat <0-100>` (op level 2).

## Building

Requires Java 21.

```bash
./gradlew build        # jar lands in build/libs/
./gradlew runClient    # dev-launch a client
```

> Version pins live in `gradle.properties`. If dependency resolution ever
> fails on `fabric_version` / `yarn_mappings`, grab the current numbers for
> MC 1.21.1 from <https://fabricmc.net/develop> — everything else should
> stand.

## Project layout

```
src/main/java/dev/rheava/program7/
  Program7.java            mod entrypoint
  registry/                items, entities, sounds
  entity/                  SurveyorDroneEntity + ai/ goals
  director/                ProgramDirectorState, RiskAssessment, ScanRecord
  command/                 /program7 debug commands
  client/                  renderer + quad-rotor model
```

## License

All rights reserved (pre-release). A proper license will be chosen before the
CurseForge release.
