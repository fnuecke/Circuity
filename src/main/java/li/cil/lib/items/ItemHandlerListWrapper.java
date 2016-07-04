package li.cil.lib.items;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.List;

public abstract class ItemHandlerListWrapper implements IItemHandler, IItemHandlerModifiable, INBTSerializable<NBTTagList> {
    protected abstract List<ItemStack> getList();

    protected int getStackLimit(final int slot, final ItemStack stack) {
        return stack.getMaxStackSize();
    }

    // --------------------------------------------------------------------- //
    // IItemHandler

    @Override
    public int getSlots() {
        return getList().size();
    }

    @Nullable
    @Override
    public ItemStack getStackInSlot(final int slot) {
        return getList().get(slot);
    }

    @Nullable
    @Override
    public ItemStack insertItem(final int slot, @Nullable final ItemStack stack, final boolean simulate) {
        if (stack == null || stack.stackSize <= 0) {
            return null;
        }

        final ItemStack existing = getStackInSlot(slot);

        int limit = getStackLimit(slot, stack);

        if (existing != null) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing)) {
                return stack.copy();
            }

            limit -= existing.stackSize;
        }

        if (limit <= 0) {
            return stack.copy();
        }

        final boolean reachedLimit = stack.stackSize > limit;

        if (!simulate) {
            if (existing == null) {
                setStackInSlot(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack.copy());
            } else {
                setStackInSlot(slot, ItemHandlerHelper.copyStackWithSize(stack, existing.stackSize + (reachedLimit ? limit : stack.stackSize)));
            }
        }

        return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.stackSize - limit) : null;
    }

    @Nullable
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (amount <= 0) {
            return null;
        }

        final ItemStack existing = getStackInSlot(slot);

        if (existing == null) {
            return null;
        }

        final int toExtract = Math.min(amount, existing.getMaxStackSize());

        if (existing.stackSize <= toExtract) {
            if (!simulate) {
                setStackInSlot(slot, null);
            }
            return existing.copy();
        } else {
            if (!simulate) {
                setStackInSlot(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.stackSize - toExtract));
            }

            return ItemHandlerHelper.copyStackWithSize(existing, toExtract);
        }
    }

    // --------------------------------------------------------------------- //
    // IItemHandlerModifiable

    @Override
    public void setStackInSlot(final int slot, @Nullable final ItemStack stack) {
        if (!ItemStack.areItemStacksEqual(getList().get(slot), stack)) {
            getList().set(slot, stack);
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
            if (stack != null) {
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
            getList().add(ItemStack.loadItemStackFromNBT(itemNbt));
        }
    }
}
