package li.cil.lib.items;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public interface ItemHandlerModifiableProxy extends ItemHandlerProxy, IItemHandlerModifiable {
    IItemHandlerModifiable getItemHandler();

    // --------------------------------------------------------------------- //
    // IItemHandlerModifiable

    @Override
    default void setStackInSlot(final int slot, final ItemStack stack) {
        getItemHandler().setStackInSlot(slot, stack);
    }
}
