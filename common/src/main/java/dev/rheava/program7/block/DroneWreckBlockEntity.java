package dev.rheava.program7.block;

import java.util.List;

import dev.rheava.program7.registry.P7BlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/**
 * Holds a downed unit's salvage until somebody picks through the wreckage.
 */
public class DroneWreckBlockEntity extends BlockEntity {
	public static final int SLOTS = 9;

	private final DefaultedList<ItemStack> items = DefaultedList.ofSize(SLOTS, ItemStack.EMPTY);

	public DroneWreckBlockEntity(BlockPos pos, BlockState state) {
		super(P7BlockEntities.DRONE_WRECK.get(), pos, state);
	}

	/** Load salvage into the wreck; anything beyond capacity is discarded. */
	public void fill(List<ItemStack> salvage) {
		for (int i = 0; i < Math.min(SLOTS, salvage.size()); i++) {
			this.items.set(i, salvage.get(i));
		}
		this.markDirty();
	}

	/** Pull the next stack out of the wreckage. */
	public ItemStack takeNextStack() {
		for (int i = 0; i < this.items.size(); i++) {
			ItemStack stack = this.items.get(i);
			if (!stack.isEmpty()) {
				this.items.set(i, ItemStack.EMPTY);
				this.markDirty();
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	public boolean isEmpty() {
		return this.items.stream().allMatch(ItemStack::isEmpty);
	}

	public DefaultedList<ItemStack> getItems() {
		return this.items;
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		Inventories.writeNbt(nbt, this.items, registryLookup);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.items.clear();
		Inventories.readNbt(nbt, this.items, registryLookup);
	}
}
