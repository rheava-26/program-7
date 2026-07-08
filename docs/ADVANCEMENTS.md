# Program 7 — Advancement Ideas

Player-suggested advancements. Each is tagged by how buildable it is today:

- 🟢 **Ready-ish** — the underlying system exists; needs a custom advancement
  criterion (a code-registered trigger) and wiring.
- 🟡 **One system away** — needs a single new mechanic built first.
- 🔴 **Blocked** — depends on a major unbuilt system (player-drone tree,
  charge laser, base-kill/psionics, datapad v2, etc.).

Almost all of these need **mod-specific advancement criteria** (vanilla
triggers can't see drones/heat/theft), so a small reusable "custom criterion"
framework is the shared prerequisite for the whole batch.

| Title | What it takes | Depends on |
|---|---|---|
| **Batter up** — knock a drone into another drone with a Punch bow / Knockback sword | 🟡 | drone↔drone knockback-collision damage + criterion |
| **Overkill much?** — one mace swing dealing ~60+ dmg (big fall) to a Tier-1 drone | 🟢 | mace-shatter mechanic (small) + damage-threshold criterion |
| **They're right behind me, aren't they?** — kill 3 drones in 10s while undetected | 🟡 | player-detection state exposed + kill-streak criterion |
| **Thief!** — steal from a logistics drone without alerting its group | 🔴 | player-steals-from-drone mechanic + group-alert stealth |
| **It's only fun when I do it!!** — have a drone steal from a chest you placed | 🟡 | gear-theft v2 (drones raid player storage) — partially designed |
| **Industrial revolution.** — craft more drones than the autobase has | 🔴 | player-drone crafting (Phase 4) + base drone census |
| **Incredible performance.** — get the fleet to deem you a non-threat and disengage | 🟢 | heat NEUTRAL state (built) + criterion on going neutral |
| **Friends?** — crouch several times in 5s near a drone (at neutral heat) | 🟡 | curiosity-pause behavior (new) + criterion |
| **This is gonna be fun, isn't it?** — craft an item from a scavenged electronic part | 🟡 | a reverse-engineering recipe using salvage + criterion |
| **Just need a battery…** — craft a combat laser from scavenged parts | 🔴 | charge-laser weapon |
| **Sustained fire.** — have 10+ drones attacking you with ranged attacks at once | 🟢 | count-concurrent-attackers criterion |
| **They have a what now?** — get intercepted mid-air by AA missiles on elytra/trident | 🔴 | anti-air missile interception (elytra counter) |
| **How???** — kill a fixed-wing drone with melee, no elytra/wind charge | 🟢 | Air UAV exists; melee-kill criterion with elytra/charge guard |
| **Kidnapping!!** — steal a drone while it's charging | 🔴 | drone-charging state + steal-drone mechanic |
| **I get why they like it so much!** — make 50+ non-combat drones | 🔴 | player-drone tree (Phase 4) |
| **Summoner class.** — kill a wither using only drones (any drones), never hitting it yourself | 🟡 | wither-killed-by-drones criterion (no player damage) |
| **Huh… it says they're right behind me?** — get hit by an explosive drone while in datapad view | 🔴 | datapad "view" mode (v2 HUD) |
| **It's… over?** — destroy the main autobase | 🟡 | base HP / base-kill event (Phase 2) |
| **…** — unlock a psionic power | 🔴 | first-base-kill psionic unlock (config-gated) |

## New mechanics these introduce (captured for the design bible)

- **Mace shatter.** Maces hit drones — *especially* light ones — hard enough to
  make them **violently shatter** (extra debris/spark VFX, an emphatic death).
  Fits the typed-armor model: a mace is a heavy MELEE impact; light/unarmored
  fliers take a shatter-kill and a bigger particle burst. A 20-block mace drop
  onto a Tier-1 drone is the canonical "Overkill much?" moment.
- **Curiosity pause.** At **neutral/low heat**, a drone that sees the player
  crouch repeatedly nearby will **stop and pause to watch** — a brief,
  non-hostile "what are you doing?" beat. Only happens when the fleet isn't
  hunting you; it's the tender flip-side of the horror, and the hook for the
  "Friends?" advancement.

## Config toggles referenced

- **Item stealing** — already in the config table (drones loot unattended gear).
- **Psionics** — NEW toggle to add: lets players disable psionic powers
  entirely (the "…" advancement and the first-base-kill unlock respect it).

## Build order suggestion

1. **Custom-criterion framework** (shared prerequisite) + the 🟢 quick wins:
   *Overkill much?* (with mace-shatter), *Incredible performance*, *Sustained
   fire*, *How???*.
2. 🟡 mechanics as their systems land: *Batter up*, *They're right behind me*,
   *Friends?* (curiosity pause), *Summoner class*, *It's… over?*.
3. 🔴 tie to their big systems (charge laser, player-drones, datapad v2,
   psionics, AA interception) as those get built.
