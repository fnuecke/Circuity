package li.cil.lib.items;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

public abstract class ItemHandlerListWrapper implements IItemHandler, IItemHandlerModifiable, INBTSerializable<NBTTagList> {
    protected abstract List<ItemStack> getList();

    protected int getStackLimit(final int slot, final ItemStack stack) {
        return stack.getMaxStackSize();
    }

    protected void onContentsChanged(final int slot) {
    }

    // --------------------------------------------------------------------- //
    // IItemHandler

    @Override
    public int getSlots() {
        return getList().size();
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        return getList().get(slot);
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final ItemStack existing = getStackInSlot(slot);

        int limit = getStackLimit(slot, stack);

        if (!existing.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing)) {
                return stack.copy();
            }

            limit -= existing.getCount();
        }

        if (limit <= 0) {
            return stack.copy();
        }

        final boolean reachedLimit = stack.getCount() > limit;

        if (!simulate) {
            if (existing.isEmpty()) {
                setStackInSlot(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack.copy());
            } else {
                setStackInSlot(slot, ItemHandlerHelper.copyStackWithSize(stack, existing.getCount() + (reachedLimit ? limit : stack.getCount())));
            }
        }

        return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - limit) : ItemStack.EMPTY;
    }

    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        final ItemStack existing = getStackInSlot(slot);

        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final int toExtract = Math.min(amount, existing.getMaxStackSize());

        if (existing.getCount() <= toExtract) {
            if (!simulate) {
                setStackInSlot(slot, ItemStack.EMPTY);
            }
            return existing.copy();
        } else {
            if (!simulate) {
                setStackInSlot(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.getCount() - toExtract));
            }

            return ItemHandlerHelper.copyStackWithSize(existing, toExtract);
        }
    }

    @Override
    public int getSlotLimit(final int slot) {
        return 64;
    }

    // --------------------------------------------------------------------- //
    // IItemHandlerModifiable

    @Override
    public void setStackInSlot(final int slot, final ItemStack stack) {
        if (!ItemStack.areItemStacksEqual(getList().get(slot), stack)) {
            getList().set(slot, stack);
            onContentsChanged(slot);
        }
    }

    // --------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagList serializeNBT() {
        final NBTTagList itemsNbt = new NBTTagList();
        for (int slot = 0; slot < getSlots(); slot++) {
            final NBTTagCompound itemNbt = new NBTTagCompound();
            final ItemStack stack = getStackInSlot(slot);
            if (!stack.isEmpty()) {
                stack.writeToNBT(itemNbt);
            }
            itemsNbt.appendTag(itemNbt);
        }
        return itemsNbt;
    }

    @Override
    public void deserializeNBT(final NBTTagList itemsNbt) {
        getList().clear();
        for (int slot = 0; slot < itemsNbt.tagCount(); slot++) {
            final NBTTagCompound itemNbt = itemsNbt.getCompoundTagAt(slot);
            getList().add(new ItemStack(itemNbt));
        }
    }
}
