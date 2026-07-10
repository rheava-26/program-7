package dev.rheava.program7.director;

/**
 * Per-unit upkeep contract (see {@code SUPPLY_LINES_SPEC.md} §2): which
 * {@link SupplyNetwork} supply type a unit draws, how much of it per cycle,
 * and how many failed draws in a row ({@code graceCycles}) it can absorb
 * before it reads as degraded.
 *
 * <p>{@link dev.rheava.program7.entity.ProgramDroneEntity#upkeepProfile()}
 * returns {@code null} for exempt units (Tier 1, couriers, the battery
 * center) — those never even look at this record.
 */
public record UpkeepProfile(String supplyType, int drainPerCycle, int graceCycles) {
}
