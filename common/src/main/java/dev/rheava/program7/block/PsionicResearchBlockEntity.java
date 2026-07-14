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
 * The block-entity brains of the psionic research building — copy-pasted
 * shape from {@link ProbeCoreBlockEntity} exactly per {@code
 * RESEARCH_TREE.md} §5/§10 build order step 1 ("HP, an under-attack window,
 * a destruction hook — copy-pasted shape from ProbeCoreBlockEntity, same
 * regen-when-not-attacked rule"). {@link PsionicResearchBlock} owns the
 * actual "was this destroyed" / "someone's mining it right now" hooks; this
 * class carries the persisted state those hooks read and write, and is what
 * {@link dev.rheava.program7.director.ResearchTree#tick} polls each cycle
 * before attempting a feed step.
 *
 * <p><b>{@code hp} is dead state (finding #10), same as in {@link
 * ProbeCoreBlockEntity} it was copied from</b> — see that class's own doc
 * comment: "scaffolding for a future damage mechanic... nothing in this
 * phase decrements it beyond the passive regen." Neither building has an
 * actual damage-dealing hook anywhere in the codebase today (only {@link
 * #markDamaged} exists, which stamps {@code lastDamagedTick} for the
 * under-attack window but never touches {@code hp}), so wiring real damage
 * in here alone — without also deciding what should deal it for both
 * buildings — is a bigger design task than this pass, not a small fix.
 * Tracked in {@code BACKLOG.md}.
 */
public class PsionicResearchBlockEntity extends BlockEntity {
	public static final int MAX_HP = 300;
	/** A break attempt counts as "under attack" for this many ticks afterward. */
	private static final int UNDER_ATTACK_WINDOW = 200;
	private static final int REGEN_INTERVAL = 100;
	private static final int REGEN_AMOUNT = 1;

	private static final String NBT_HP = "Hp";
	private static final String NBT_LAST_DAMAGED_TICK = "LastDamagedTick";

	private int hp = MAX_HP;
	/** Far enough in the past at construction that a freshly placed building doesn't read as under attack. */
	private long lastDamagedTick = Long.MIN_VALUE / 2;

	public PsionicResearchBlockEntity(BlockPos pos, BlockState state) {
		super(P7BlockEntities.PSIONIC_RESEARCH.get(), pos, state);
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, PsionicResearchBlockEntity block) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		block.tick(serverWorld);
	}

	private void tick(ServerWorld world) {
		if (this.hp < MAX_HP && world.getTime() % REGEN_INTERVAL == 0 && !this.isUnderAttack(world.getTime())) {
			this.hp = Math.min(MAX_HP, this.hp + REGEN_AMOUNT);
			this.markDirty();
		}
	}

	/** Called when a player starts mining this building — the "research paused" signal {@code ResearchTree.tick} reads via {@link #isUnderAttack}. */
	public void markDamaged(long now) {
		this.lastDamagedTick = now;
		this.markDirty();
	}

	/** True if the building has been hit/mined within the last {@value #UNDER_ATTACK_WINDOW} ticks — feed steps don't attempt while this reads true (doc §5: "research pauses, doesn't reverse"). */
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
