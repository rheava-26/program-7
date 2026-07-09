package dev.rheava.program7.director;

import java.util.function.Function;

import dev.rheava.program7.entity.ProgramDroneEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * A lightweight stand-in for a real {@link ProgramDroneEntity} that has
 * dropped out of range of every player. Holds just enough to reconstruct the
 * original entity byte-for-byte: its type, last known position/facing, and
 * its full saved NBT (inventory, health, alert state, everything).
 *
 * <p>Tokens carry no behaviour of their own in this pass — they're pure data,
 * held by the {@link VirtualFleet} and round-tripped back into a real entity
 * the moment a player wanders close enough again.
 */
public final class DroneToken {
	private final Identifier entityTypeId;
	private final String dimensionId;
	private final double x;
	private final double y;
	private final double z;
	private final float yaw;
	private final NbtCompound data;

	private DroneToken(Identifier entityTypeId, String dimensionId,
			double x, double y, double z, float yaw, NbtCompound data) {
		this.entityTypeId = entityTypeId;
		this.dimensionId = dimensionId;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.data = data;
	}

	/**
	 * Snapshot a live drone into a token. Returns {@code null} if the entity
	 * refuses to save (e.g. already removed), in which case the caller should
	 * simply not virtualize it this pass.
	 */
	@Nullable
	public static DroneToken capture(ProgramDroneEntity drone) {
		NbtCompound data = new NbtCompound();
		if (!drone.saveNbt(data)) {
			return null;
		}
		Identifier entityTypeId = EntityType.getId(drone.getType());
		String dimensionId = drone.getWorld().getRegistryKey().getValue().toString();
		return new DroneToken(entityTypeId, dimensionId,
				drone.getX(), drone.getY(), drone.getZ(), drone.getYaw(), data);
	}

	public double getX() {
		return this.x;
	}

	public double getY() {
		return this.y;
	}

	public double getZ() {
		return this.z;
	}

	public Identifier getEntityTypeId() {
		return this.entityTypeId;
	}

	/** The dimension this drone was captured in — a token only ever re-materializes in its own world. */
	public String getDimensionId() {
		return this.dimensionId;
	}

	public NbtCompound toNbt() {
		NbtCompound tag = new NbtCompound();
		tag.putString("EntityType", this.entityTypeId.toString());
		tag.putString("Dimension", this.dimensionId);
		tag.putDouble("X", this.x);
		tag.putDouble("Y", this.y);
		tag.putDouble("Z", this.z);
		tag.putFloat("Yaw", this.yaw);
		tag.put("Data", this.data);
		return tag;
	}

	/**
	 * Rebuild a token from NBT. Throws if the tag is malformed (missing/blank
	 * entity id) — {@link VirtualFleet#readNbt} calls this per entry inside a
	 * try/catch so one bad token can never abort loading the whole fleet (let
	 * alone the rest of the Director state).
	 */
	public static DroneToken fromNbt(NbtCompound tag) {
		Identifier entityTypeId = Identifier.of(tag.getString("EntityType"));
		String dimensionId = tag.contains("Dimension") ? tag.getString("Dimension") : "minecraft:overworld";
		double x = tag.getDouble("X");
		double y = tag.getDouble("Y");
		double z = tag.getDouble("Z");
		float yaw = tag.getFloat("Yaw");
		NbtCompound data = tag.getCompound("Data");
		return new DroneToken(entityTypeId, dimensionId, x, y, z, yaw, data);
	}

	/**
	 * Reconstruct the real entity from the saved NBT and drop it back into the
	 * world at this token's last known position/facing. Returns {@code null}
	 * (and spawns nothing) if the entity fails to load — e.g. the saved NBT no
	 * longer resolves to a valid entity type.
	 */
	@Nullable
	public Entity materialize(ServerWorld world) {
		Entity entity = EntityType.loadEntityWithPassengers(this.data, world, Function.identity());
		if (entity == null) {
			return null;
		}
		entity.refreshPositionAndAngles(this.x, this.y, this.z, this.yaw, entity.getPitch());
		world.spawnEntity(entity);
		return entity;
	}
}
