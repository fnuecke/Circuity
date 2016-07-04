package li.cil.lib.items;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public interface ItemHandlerProxy extends IItemHandler {
    IItemHandler getItemHandler();

    // --------------------------------------------------------------------- //
    // IItemHandler

    @Override
    default int getSlots() {
        return getItemHandler().getSlots();
    }

    @Override
    default ItemStack getStackInSlot(final int slot) {
        return getItemHandler().getStackInSlot(slot);
    }

    @Override
    default ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        return getItemHandler().insertItem(slot, stack, simulate);
    }

    @Override
    default ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        return getItemHandler().extractItem(slot, amount, simulate);
    }
}
