package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.SurveyorDroneEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The escalation of {@link StealItemsGoal}: the Program doesn't just sweep up
 * what you dropped, it comes for what you stashed. A surveyor that finds an
 * unattended container nearby — a chest, a barrel, whatever holds items —
 * flies over, cracks it, and pulls a few stacks out into its cargo. Shoot the
 * thief down and it all falls back out, same as any other cargo.
 *
 * <p>It leaves the block itself alone (no grief), skips the Program's own
 * block entities so drones never loot their own base, and — like every other
 * theft behavior — respects the {@code itemStealing} config toggle.
 */
public class RaidStorageGoal extends Goal {
	private static final int SEARCH_RADIUS_H = 8;
	private static final int SEARCH_RADIUS_V = 4;
	private static final double ARRIVAL_RANGE = 2.5;
	/** Stacks lifted per successful raid before the drone backs off to cool down. */
	private static final int GRAB_STACKS = 2;
	/** Cooldown after a raid, so a single container isn't stripped in one tick-storm. */
	private static final int RAID_COOLDOWN = 160;
	/** Shorter wait between scans when there was nothing to raid. */
	private static final int RESCAN_INTERVAL = 40;
	/** Give up on a container we can't actually path to instead of orbiting it forever. */
	private static final int MAX_PURSUIT_TICKS = 200;

	private final SurveyorDroneEntity drone;
	@Nullable
	private BlockPos target;
	private int cooldown;
	private int pursuitTicks;

	public RaidStorageGoal(SurveyorDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (!Program7.CONFIG.itemStealing || this.drone.isRetreating() || this.drone.isCargoFull()
				|| this.drone.isScrambled()) {
			return false;
		}
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}
		this.target = findNearestStash();
		if (this.target == null) {
			this.cooldown = RESCAN_INTERVAL;
			return false;
		}
		this.pursuitTicks = 0;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return this.target != null && hasLoot(this.target)
				&& !this.drone.isRetreating() && !this.drone.isCargoFull()
				&& this.pursuitTicks < MAX_PURSUIT_TICKS
				&& this.drone.getBlockPos().isWithinDistance(this.target, 34.0);
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (this.target == null) {
			return;
		}
		this.pursuitTicks++;
		double cx = this.target.getX() + 0.5;
		double cy = this.target.getY() + 0.5;
		double cz = this.target.getZ() + 0.5;
		this.drone.getLookControl().lookAt(cx, cy, cz);
		if (this.drone.squaredDistanceTo(cx, cy, cz) > ARRIVAL_RANGE * ARRIVAL_RANGE) {
			if (this.drone.getNavigation().isIdle()) {
				this.drone.getNavigation().startMovingTo(cx, this.target.getY() + 1.0, cz, 1.1);
			}
			return;
		}
		this.raid(this.target);
	}

	private void raid(BlockPos pos) {
		Inventory inv = inventoryAt(pos);
		if (inv == null) {
			this.target = null;
			return;
		}
		this.drone.playSound(SoundEvents.BLOCK_CHEST_OPEN, 0.7f, 0.9f);
		int grabbed = 0;
		for (int slot = 0; slot < inv.size() && grabbed < GRAB_STACKS && !this.drone.isCargoFull(); slot++) {
			ItemStack stack = inv.getStack(slot);
			if (!stack.isEmpty()) {
				this.drone.addCargo(inv.removeStack(slot));
				this.drone.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 0.8f);
				grabbed++;
			}
		}
		if (grabbed > 0) {
			inv.markDirty();
		}
		this.cooldown = RAID_COOLDOWN;
		this.target = null;
	}

	/** Nearest raidable container with loot inside, or {@code null} if none in range. */
	@Nullable
	private BlockPos findNearestStash() {
		BlockPos origin = this.drone.getBlockPos();
		BlockPos min = origin.add(-SEARCH_RADIUS_H, -SEARCH_RADIUS_V, -SEARCH_RADIUS_H);
		BlockPos max = origin.add(SEARCH_RADIUS_H, SEARCH_RADIUS_V, SEARCH_RADIUS_H);
		BlockPos nearest = null;
		double best = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.iterate(min, max)) {
			Inventory inv = inventoryAt(pos);
			if (inv == null || inv.isEmpty()) {
				continue;
			}
			double distance = origin.getSquaredDistance(pos);
			if (distance < best) {
				best = distance;
				nearest = pos.toImmutable();
			}
		}
		return nearest;
	}

	private boolean hasLoot(BlockPos pos) {
		Inventory inv = inventoryAt(pos);
		return inv != null && !inv.isEmpty();
	}

	/**
	 * The container inventory at {@code pos}, or {@code null} if there's no
	 * lootable container there. Program 7's own block entities are skipped so
	 * a raider never strips its own base's assembler/wreck/turret.
	 */
	@Nullable
	private Inventory inventoryAt(BlockPos pos) {
		World world = this.drone.getWorld();
		BlockEntity be = world.getBlockEntity(pos);
		if (!(be instanceof Inventory inv)) {
			return null;
		}
		if ("program7".equals(Registries.BLOCK_ENTITY_TYPE.getId(be.getType()).getNamespace())) {
			return null;
		}
		return inv;
	}

	@Override
	public void stop() {
		this.target = null;
		this.drone.getNavigation().stop();
	}
}
