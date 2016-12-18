package li.cil.circuity.common.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ContainerAddressSelector extends Container {
    @Override
    public boolean canInteractWith(final EntityPlayer player) {
        return true;
    }
}
