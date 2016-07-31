package li.cil.lib.api.ecs.component.event;

import net.minecraftforge.items.IItemHandler;

public interface InventoryChangeListener {
    void handleInventoryChange(final IItemHandler inventory, final int slot);
}
