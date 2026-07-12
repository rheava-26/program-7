package dev.rheava.program7.block;

import java.util.List;

import dev.rheava.program7.registry.P7BlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The cargo brains behind a delivered {@link CrateBlock}: a small drop-off
 * container a logistics drone fills on arrival (via {@link #setCargo}), which
 * then spends a few seconds "unpacking" before it empties itself out.
 *
 * <p>On unpack it prefers to hand its contents to an orthogonally adjacent
 * {@link StorageDeckBlockEntity} — a crate set down against the base stockpile
 * folds straight into it — and spills anything that won't fit (or that has no
 * deck to receive it) onto the ground with {@link ItemScatterer}. Once empty
 * the crate block removes itself, so a serviced drop point doesn't leave inert
 * blocks lying around.
 *
 * <p>Cargo and the unpack countdown both persist in NBT, and a crate broken
 * mid-unpack drops whatever it was still holding (see
 * {@link CrateBlock#onStateReplaced}).
 */
public class CrateBlockEntity extends BlockEntity {
	/** How many stacks of cargo a single crate holds. */
	public static final int SLOTS = 5;
	/** Ticks a freshly-filled crate spends unpacking before it deposits — ~4s. */
	private static final int UNPACK_TICKS = 80;

	private final DefaultedList<ItemStack> cargo = DefaultedList.ofSize(SLOTS, ItemStack.EMPTY);
	/** Ticks left before this crate unpacks; {@code 0} means idle (empty, or already unpacked). */
	private int unpackTimer;

	public CrateBlockEntity(BlockPos pos, BlockState state) {
		super(P7BlockEntities.CRATE.get(), pos, state);
	}

	/**
	 * Load a drone's delivery into the crate and start the unpack countdown.
	 * Anything beyond {@link #SLOTS} is dropped, matching {@link
	 * StorageDeckBlockEntity#fill}.
	 */
	public void setCargo(List<ItemStack> contents) {
		this.cargo.clear();
		for (int i = 0; i < Math.min(SLOTS, contents.size()); i++) {
			this.cargo.set(i, contents.get(i));
		}
		this.unpackTimer = this.hasCargo() ? UNPACK_TICKS : 0;
		this.markDirty();
	}

	public boolean hasCargo() {
		return this.cargo.stream().anyMatch(stack -> !stack.isEmpty());
	}

	/** Exposed so {@link CrateBlock#onStateReplaced} can spill a crate broken mid-unpack. */
	public DefaultedList<ItemStack> getCargo() {
		return this.cargo;
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, CrateBlockEntity crate) {
		if (!(world instanceof ServerWorld serverWorld) || crate.unpackTimer <= 0) {
			return;
		}
		crate.unpackTimer--;
		if (crate.unpackTimer <= 0) {
			crate.unpack(serverWorld, pos);
		}
	}

	/**
	 * Empty the crate: fold cargo into an adjacent storage deck where possible,
	 * spill the rest into the world, then remove the now-empty crate block.
	 */
	private void unpack(ServerWorld world, BlockPos pos) {
		StorageDeckBlockEntity deck = findAdjacentDeck(world, pos);
		for (ItemStack stack : this.cargo) {
			if (stack.isEmpty()) {
				continue;
			}
			if (deck == null || !deck.offer(stack)) {
				ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
			}
		}
		this.cargo.clear();
		this.unpackTimer = 0;
		// Cargo is already cleared, so CrateBlock#onStateReplaced sees an empty
		// crate here and won't double-drop anything.
		world.removeBlock(pos, false);
	}

	@Nullable
	private static StorageDeckBlockEntity findAdjacentDeck(World world, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			if (world.getBlockEntity(pos.offset(direction)) instanceof StorageDeckBlockEntity deck) {
				return deck;
			}
		}
		return null;
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		Inventories.writeNbt(nbt, this.cargo, registryLookup);
		nbt.putInt("UnpackTimer", this.unpackTimer);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.cargo.clear();
		Inventories.readNbt(nbt, this.cargo, registryLookup);
		this.unpackTimer = nbt.getInt("UnpackTimer");
	}
}
