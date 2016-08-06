package li.cil.lib.api.ecs.component.event;

import li.cil.lib.api.ecs.component.Component;
import net.minecraftforge.items.IItemHandler;

public interface InventoryChangeListener extends Component {
    void handleInventoryChange(final IItemHandler inventory, final int slot);
}
