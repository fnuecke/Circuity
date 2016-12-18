package li.cil.lib.capabilities;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;

// combines multiple IItemHandler into one interface
// based on Forge's CombinedInvWrapper
public final class CombinedItemHandlerWrapper implements IItemHandler {
    private final IItemHandler[] itemHandler; // the handlers
    private final int[] baseIndex; // index-offsets of the different handlers
    private final int slotCount; // number of total slots

    // --------------------------------------------------------------------- //

    public CombinedItemHandlerWrapper(final IItemHandler... itemHandler) {
        this.itemHandler = itemHandler;
        this.baseIndex = new int[itemHandler.length];
        int index = 0;
        for (int i = 0; i < itemHandler.length; i++) {
            index += itemHandler[i].getSlots();
            baseIndex[i] = index;
        }
        this.slotCount = index;
    }

    // --------------------------------------------------------------------- //
    // IItemHandler

    @Override
    public int getSlots() {
        return slotCount;
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        final int index = getHandlerIndex(slot);
        final IItemHandler handler = getHandlerFromIndex(index);
        return handler.getStackInSlot(getLocalSlot(slot, index));
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        final int index = getHandlerIndex(slot);
        final IItemHandler handler = getHandlerFromIndex(index);
        return handler.insertItem(getLocalSlot(slot, index), stack, simulate);
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        final int index = getHandlerIndex(slot);
        final IItemHandler handler = getHandlerFromIndex(index);
        return handler.extractItem(getLocalSlot(slot, index), amount, simulate);
    }

    @Override
    public int getSlotLimit(final int slot) {
        final int index = getHandlerIndex(slot);
        final IItemHandler handler = getHandlerFromIndex(index);
        return handler.getSlotLimit(getLocalSlot(slot, index));
    }

    // --------------------------------------------------------------------- //

    private int getHandlerIndex(final int slot) {
        if (slot < 0)
            return -1;

        for (int i = 0; i < baseIndex.length; i++) {
            if (slot - baseIndex[i] < 0) {
                return i;
            }
        }
        return -1;
    }

    private IItemHandler getHandlerFromIndex(final int index) {
        if (index < 0 || index >= itemHandler.length) {
            return EmptyHandler.INSTANCE;
        }
        return itemHandler[index];
    }

    private int getLocalSlot(final int slot, final int index) {
        if (index <= 0 || index >= baseIndex.length) {
            return slot;
        }
        return slot - baseIndex[index - 1];
    }
}
