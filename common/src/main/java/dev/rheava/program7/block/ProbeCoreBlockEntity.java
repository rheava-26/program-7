package dev.rheava.program7.block;

import dev.rheava.program7.registry.P7BlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The block-entity brains of a landed probe core: an HP/under-attack model
 * for the Program's base heart. {@link ProbeCoreBlock} owns the actual
 * "was this destroyed" and "someone's mining it right now" hooks; this class
 * just carries the persisted state those hooks read and write.
 *
 * <p>{@code hp} is scaffolding for a future damage mechanic (e.g. explosive
 * attacks) — nothing in this phase decrements it beyond the passive regen
 * below, but it's persisted now so that mechanic won't need an NBT migration
 * later. {@code lastDamagedTick} is live today: it's stamped the moment a
 * player starts mining the core, and {@link #isUnderAttack} — the "base
 * under attack" signal a later defense-escalation task reads — answers off
 * of it.
 */
public class ProbeCoreBlockEntity extends BlockEntity {
	public static final int MAX_HP = 400;
	/** A break attempt (or, later, a hit) counts as "under attack" for this many ticks afterward. */
	private static final int UNDER_ATTACK_WINDOW = 200;
	/** How often (in ticks) the core passively repairs, provided nobody's currently on it. */
	private static final int REGEN_INTERVAL = 100;
	private static final int REGEN_AMOUNT = 1;

	private static final String NBT_HP = "Hp";
	private static final String NBT_LAST_DAMAGED_TICK = "LastDamagedTick";

	private int hp = MAX_HP;
	/** Far enough in the past at construction that a freshly placed core doesn't read as under attack. */
	private long lastDamagedTick = Long.MIN_VALUE / 2;

	public ProbeCoreBlockEntity(BlockPos pos, BlockState state) {
		super(P7BlockEntities.PROBE_CORE.get(), pos, state);
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, ProbeCoreBlockEntity core) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		core.tick(serverWorld);
	}

	private void tick(ServerWorld world) {
		if (this.hp < MAX_HP && world.getTime() % REGEN_INTERVAL == 0 && !this.isUnderAttack(world.getTime())) {
			this.hp = Math.min(MAX_HP, this.hp + REGEN_AMOUNT);
			this.markDirty();
		}
	}

	/** Called when a player starts mining this core — the "base under attack" signal. */
	public void markDamaged(long now) {
		this.lastDamagedTick = now;
		this.markDirty();
	}

	/** True if the core has been hit/mined within the last {@value #UNDER_ATTACK_WINDOW} ticks. */
	public boolean isUnderAttack(long now) {
		return now - this.lastDamagedTick <= UNDER_ATTACK_WINDOW;
	}

	public int getHp() {
		return this.hp;
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putInt(NBT_HP, this.hp);
		nbt.putLong(NBT_LAST_DAMAGED_TICK, this.lastDamagedTick);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.hp = nbt.contains(NBT_HP) ? nbt.getInt(NBT_HP) : MAX_HP;
		this.lastDamagedTick = nbt.contains(NBT_LAST_DAMAGED_TICK)
				? nbt.getLong(NBT_LAST_DAMAGED_TICK)
				: Long.MIN_VALUE / 2;
	}
}
