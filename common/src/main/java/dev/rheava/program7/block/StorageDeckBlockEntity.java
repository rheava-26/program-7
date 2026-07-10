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
 * Holds a Program base's physical stockpile until a player raids the base
 * and picks through it.
 */
public class StorageDeckBlockEntity extends BlockEntity {
	public static final int SLOTS = 27;

	private final DefaultedList<ItemStack> items = DefaultedList.ofSize(SLOTS, ItemStack.EMPTY);

	public StorageDeckBlockEntity(BlockPos pos, BlockState state) {
		super(P7BlockEntities.STORAGE_DECK.get(), pos, state);
	}

	/** Load the base's stores into the deck; anything beyond capacity is discarded. */
	public void fill(List<ItemStack> contents) {
		for (int i = 0; i < Math.min(SLOTS, contents.size()); i++) {
			this.items.set(i, contents.get(i));
		}
		this.markDirty();
	}

	/** Pull the next stack out of storage. */
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
