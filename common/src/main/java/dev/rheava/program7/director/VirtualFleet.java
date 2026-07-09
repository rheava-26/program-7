package dev.rheava.program7.director;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.ProgramDroneEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.ChunkStatus;

/**
 * The registry of "virtualized" Program drones — real {@link ProgramDroneEntity}
 * instances that have been swapped out for a lightweight {@link DroneToken}
 * because no player is anywhere near them.
 *
 * <p>This is v1: just the registry, persistence, and the range-poll round
 * trip. Virtualized drones have no off-screen behaviour whatsoever — they sit
 * inert as data until a player wanders back into range, at which point they
 * are reconstructed exactly as they were.
 */
public final class VirtualFleet {
	/** A token this close to a player gets materialized back into a real entity. */
	private static final double MATERIALIZE_RANGE = 64.0;
	/**
	 * A live drone this far from every player gets virtualized into a token.
	 * Deliberately larger than {@link #MATERIALIZE_RANGE} so a player sitting
	 * right at the boundary can't flip a drone back and forth every scan.
	 */
	private static final double DEMATERIALIZE_RANGE = 96.0;
	/** Only scan every 40 ticks (2 seconds) — this never needs to be tick-perfect. */
	private static final int SCAN_INTERVAL = 40;
	private static final int VERSION = 1;

	private final List<DroneToken> tokens = new ArrayList<>();
	private int tickCounter = 0;

	/**
	 * Called every tick from the Director. Internally throttled to one scan
	 * every {@link #SCAN_INTERVAL} ticks. Returns {@code true} if the token
	 * list actually changed this call, so the caller knows whether to mark
	 * its persistent state dirty.
	 */
	public boolean tick(ServerWorld world) {
		this.tickCounter++;
		if (this.tickCounter < SCAN_INTERVAL) {
			return false;
		}
		this.tickCounter = 0;

		boolean changed = this.materializeNearby(world);
		changed |= this.dematerializeDistant(world);
		return changed;
	}

	/**
	 * Bring any token back to life if a player is close enough to see it and
	 * its chunk is actually loaded (so the spawn isn't wasted on an unloaded
	 * chunk).
	 */
	private boolean materializeNearby(ServerWorld world) {
		if (this.tokens.isEmpty()) {
			return false;
		}
		String dimensionId = world.getRegistryKey().getValue().toString();
		boolean changed = false;
		Iterator<DroneToken> iterator = this.tokens.iterator();
		while (iterator.hasNext()) {
			DroneToken token = iterator.next();
			if (!token.getDimensionId().equals(dimensionId)) {
				continue; // captured in a different dimension — never spawn it here
			}
			if (world.getClosestPlayer(token.getX(), token.getY(), token.getZ(), MATERIALIZE_RANGE, false) == null) {
				continue;
			}
			int chunkX = ((int) token.getX()) >> 4;
			int chunkZ = ((int) token.getZ()) >> 4;
			if (world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) == null) {
				continue; // chunk isn't actually loaded yet — try again next scan
			}
			Entity entity = token.materialize(world);
			if (entity != null) {
				iterator.remove();
				changed = true;
			} else {
				// A player is right here and the chunk is loaded, but the entity
				// type no longer resolves (a mod/datapack removed or renamed it).
				// Drop the token rather than retrying it forever.
				Program7.LOGGER.warn("[Program 7] Dropping unrecoverable virtual drone token: {}",
						token.getEntityTypeId());
				iterator.remove();
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * Sweep every loaded {@link ProgramDroneEntity} and virtualize the ones
	 * with no player anywhere near. Drones to discard are collected first and
	 * discarded afterward, since {@link ServerWorld#iterateEntities()} can't
	 * be mutated mid-iteration.
	 */
	private boolean dematerializeDistant(ServerWorld world) {
		List<ProgramDroneEntity> toDiscard = new ArrayList<>();
		for (Entity entity : world.iterateEntities()) {
			if (entity instanceof ProgramDroneEntity drone
					&& world.getClosestPlayer(drone.getX(), drone.getY(), drone.getZ(), DEMATERIALIZE_RANGE, false) == null) {
				toDiscard.add(drone);
			}
		}
		if (toDiscard.isEmpty()) {
			return false;
		}

		boolean changed = false;
		for (ProgramDroneEntity drone : toDiscard) {
			DroneToken token = DroneToken.capture(drone);
			if (token != null) {
				this.tokens.add(token);
				drone.discard();
				changed = true;
			}
		}
		return changed;
	}

	public NbtCompound toNbt(RegistryWrapper.WrapperLookup registryLookup) {
		NbtCompound tag = new NbtCompound();
		tag.putInt("Version", VERSION);
		NbtList list = new NbtList();
		for (DroneToken token : this.tokens) {
			list.add(token.toNbt());
		}
		tag.put("Tokens", list);
		return tag;
	}

	public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
		this.tokens.clear();
		if (!tag.contains("Tokens")) {
			return;
		}
		NbtList list = tag.getList("Tokens", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			try {
				this.tokens.add(DroneToken.fromNbt(list.getCompound(i)));
			} catch (RuntimeException e) {
				// One malformed token must never abort loading the rest of the
				// fleet — let alone the whole Director state. Skip it and move on.
				Program7.LOGGER.warn("[Program 7] Skipping malformed virtual drone token on load", e);
			}
		}
	}

	public int size() {
		return this.tokens.size();
	}

	/** Read-only snapshot of the current tokens, for display purposes (e.g. the debug command). */
	public List<DroneToken> getTokens() {
		return List.copyOf(this.tokens);
	}
}
